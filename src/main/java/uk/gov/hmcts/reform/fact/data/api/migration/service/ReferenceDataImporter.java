package uk.gov.hmcts.reform.fact.data.api.migration.service;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.gov.hmcts.reform.fact.data.api.entities.AreaOfLawType;
import uk.gov.hmcts.reform.fact.data.api.entities.ContactDescriptionType;
import uk.gov.hmcts.reform.fact.data.api.entities.LocalAuthorityType;
import uk.gov.hmcts.reform.fact.data.api.entities.OpeningHourType;
import uk.gov.hmcts.reform.fact.data.api.entities.Region;
import uk.gov.hmcts.reform.fact.data.api.entities.ServiceArea;
import uk.gov.hmcts.reform.fact.data.api.migration.entities.LegacyService;
import uk.gov.hmcts.reform.fact.data.api.migration.exception.MigrationClientException;
import uk.gov.hmcts.reform.fact.data.api.migration.model.ContactDescriptionTypeDto;
import uk.gov.hmcts.reform.fact.data.api.migration.model.LegacyExportResponse;
import uk.gov.hmcts.reform.fact.data.api.migration.model.LocalAuthorityTypeDto;
import uk.gov.hmcts.reform.fact.data.api.migration.model.OpeningHourTypeDto;
import uk.gov.hmcts.reform.fact.data.api.migration.model.RegionDto;
import uk.gov.hmcts.reform.fact.data.api.migration.model.ServiceAreaDto;
import uk.gov.hmcts.reform.fact.data.api.migration.model.ServiceDto;
import uk.gov.hmcts.reform.fact.data.api.repositories.AreaOfLawTypeRepository;
import uk.gov.hmcts.reform.fact.data.api.repositories.ContactDescriptionTypeRepository;
import uk.gov.hmcts.reform.fact.data.api.repositories.LocalAuthorityTypeRepository;
import uk.gov.hmcts.reform.fact.data.api.repositories.OpeningHoursTypeRepository;
import uk.gov.hmcts.reform.fact.data.api.repositories.RegionRepository;
import uk.gov.hmcts.reform.fact.data.api.repositories.ServiceAreaRepository;
import uk.gov.hmcts.reform.fact.data.api.migration.repository.LegacyServiceRepository;

class ReferenceDataImporter {

    private static final Logger LOG = LoggerFactory.getLogger(ReferenceDataImporter.class);
    /**
     * Words removed during local-authority name normalisation so matching is based on
     * distinctive tokens (for example, "Bolton Borough Council" can map to
     * "Bolton Metropolitan Borough Council"). This is as opposed to simply skipping.
     */
    private static final Set<String> LOOKUP_STOP_WORDS = Set.of(
        "and",
        "authority",
        "borough",
        "city",
        "corporation",
        "council",
        "county",
        "district",
        "london",
        "metropolitan",
        "of",
        "royal",
        "the"
    );
    private static final Map<String, List<String>> LEGACY_LOCAL_AUTHORITY_NAME_ALIASES = Map.of(
        "cumbria county council",
        List.of("Cumberland Council", "Westmorland and Furness Council"),
        "northamptonshire county council",
        List.of("North Northamptonshire Council", "West Northamptonshire Council")
    );

    private final RegionRepository regionRepository;
    private final AreaOfLawTypeRepository areaOfLawTypeRepository;
    private final ServiceAreaRepository serviceAreaRepository;
    private final LegacyServiceRepository legacyServiceRepository;
    private final LocalAuthorityTypeRepository localAuthorityTypeRepository;
    private final ContactDescriptionTypeRepository contactDescriptionTypeRepository;
    private final OpeningHoursTypeRepository openingHourTypeRepository;

    ReferenceDataImporter(
        RegionRepository regionRepository,
        AreaOfLawTypeRepository areaOfLawTypeRepository,
        ServiceAreaRepository serviceAreaRepository,
        LegacyServiceRepository legacyServiceRepository,
        LocalAuthorityTypeRepository localAuthorityTypeRepository,
        ContactDescriptionTypeRepository contactDescriptionTypeRepository,
        OpeningHoursTypeRepository openingHourTypeRepository
    ) {
        this.regionRepository = regionRepository;
        this.areaOfLawTypeRepository = areaOfLawTypeRepository;
        this.serviceAreaRepository = serviceAreaRepository;
        this.legacyServiceRepository = legacyServiceRepository;
        this.localAuthorityTypeRepository = localAuthorityTypeRepository;
        this.contactDescriptionTypeRepository = contactDescriptionTypeRepository;
        this.openingHourTypeRepository = openingHourTypeRepository;
    }

    /**
     * Maps all reference data from the legacy export into the supplied migration context. Missing
     * reference records trigger warnings (or exceptions for required entities such as regions) so
     * that later steps can rely on a complete set of lookups.
     *
     * @param response legacy payload containing the reference sections.
     * @param context migration context used to store identifier mappings.
     */
    void importReferenceData(LegacyExportResponse response, MigrationContext context) {
        mapExistingRegions(response.getRegions(), context.getRegionIds());
        mapExistingAreasOfLaw(response.getAreaOfLawTypes(), context.getAreaOfLawIds());
        mapExistingLocalAuthorityTypes(response.getLocalAuthorityTypes(), context.getLocalAuthorityTypeIds());
        mapExistingContactDescriptions(response.getContactDescriptionTypes());
        mapExistingOpeningHours(response.getOpeningHourTypes());
        persistServiceAreas(response.getServiceAreas(), context);
        persistServices(response.getServices(), context);
    }

    /**
     * Maps region identifiers from the legacy export onto the pre-seeded regions in the new schema.
     *
     * @param regions regions supplied by the legacy export.
     * @param regionIds map storing the legacy-to-new identifier mapping.
     */
    private void mapExistingRegions(List<RegionDto> regions, Map<Integer, UUID> regionIds) {
        if (isEmpty(regions)) {
            return;
        }

        for (RegionDto regionDto : regions) {
            Region region = regionRepository.findByNameAndCountry(regionDto.getName(), regionDto.getCountry())
                .orElseThrow(() -> new MigrationClientException(
                    "Region '%s' (%s) was not found in the target database".formatted(
                        regionDto.getName(), regionDto.getCountry()
                    )
                ));
            regionIds.put(regionDto.getId(), region.getId());
        }
    }

    /**
     * Maps area-of-law identifiers from the legacy export onto the pre-seeded records.
     *
     * @param areaOfLawTypes legacy area-of-law records.
     * @param ids destination map storing legacy-to-new mappings.
     */
    private void mapExistingAreasOfLaw(
        List<uk.gov.hmcts.reform.fact.data.api.migration.model.AreaOfLawTypeDto> areaOfLawTypes,
        Map<Integer, UUID> ids
    ) {
        if (isEmpty(areaOfLawTypes)) {
            return;
        }

        for (uk.gov.hmcts.reform.fact.data.api.migration.model.AreaOfLawTypeDto dto : areaOfLawTypes) {
            Optional<AreaOfLawType> entity = areaOfLawTypeRepository.findByNameIgnoreCase(dto.getName());
            if (entity.isEmpty()) {
                LOG.warn("Area of law '{}' was not found in the target database", dto.getName());
                continue;
            }
            ids.put(dto.getId(), entity.get().getId());
        }
    }

    /**
     * Reuses service areas that were seeded via Flyway and stores the ID mappings in the context.
     *
     * @param serviceAreas legacy service area definitions.
     * @param context migration context used to store mappings.
     */
    private void persistServiceAreas(List<ServiceAreaDto> serviceAreas, MigrationContext context) {
        if (isEmpty(serviceAreas)) {
            return;
        }

        for (ServiceAreaDto dto : serviceAreas) {
            Optional<ServiceArea> existing = serviceAreaRepository.findByNameIgnoreCase(dto.getName());
            if (existing.isEmpty()) {
                LOG.warn("Service area '{}' was not found in the target database", dto.getName());
                continue;
            }
            context.getServiceAreaIds().put(dto.getId(), existing.get().getId());
        }
    }

    /**
     * Updates the temporary legacy service records with the IDs of the service areas that were
     * seeded earlier, so joins can be recreated.
     *
     * @param services legacy service definitions.
     * @param context migration context containing the service-area ID mappings.
     */
    private void persistServices(List<ServiceDto> services, MigrationContext context) {
        if (isEmpty(services)) {
            return;
        }

        for (ServiceDto dto : services) {
            LegacyService entity = legacyServiceRepository.findByName(dto.getName())
                .orElseGet(LegacyService::new);
            entity.setName(dto.getName());
            entity.setNameCy(dto.getNameCy());
            entity.setDescription(dto.getDescription());
            entity.setDescriptionCy(dto.getDescriptionCy());
            entity.setServiceAreas(mapIds(dto.getServiceAreaIds(), context.getServiceAreaIds(), "service area"));
            legacyServiceRepository.save(entity);
        }
    }

    private void mapExistingLocalAuthorityTypes(
        List<LocalAuthorityTypeDto> localAuthorityTypes,
        Map<Integer, List<UUID>> ids
    ) {
        if (isEmpty(localAuthorityTypes)) {
            return;
        }

        List<LocalAuthorityType> existingLocalAuthorities = localAuthorityTypeRepository.findAll();
        Map<String, LocalAuthorityType> existingByExactName = buildCaseInsensitiveLookupMap(
            existingLocalAuthorities,
            LocalAuthorityType::getName
        );
        Map<String, LocalAuthorityType> existingByNormalisedName = buildNormalisedLookupMap(
            existingLocalAuthorities,
            LocalAuthorityType::getName
        );

        for (LocalAuthorityTypeDto dto : localAuthorityTypes) {
            if (StringUtils.isBlank(dto.getName())) {
                LOG.warn("Skipping local authority type with id {} because name is blank", dto.getId());
                continue;
            }

            List<LocalAuthorityType> existingMatches = findLocalAuthorityMatches(
                dto.getName(),
                existingByExactName,
                existingByNormalisedName
            );
            if (existingMatches.isEmpty()) {
                LOG.warn("No matching local authority type found for name '{}'", dto.getName());
                continue;
            }

            ids.put(
                dto.getId(),
                existingMatches.stream().map(LocalAuthorityType::getId).distinct().toList()
            );
        }
    }

    private void mapExistingContactDescriptions(List<ContactDescriptionTypeDto> dtos) {
        if (isEmpty(dtos)) {
            return;
        }

        Map<String, ContactDescriptionType> existingByNormalisedName = buildNormalisedLookupMap(
            contactDescriptionTypeRepository.findAll(),
            ContactDescriptionType::getName
        );
        logMissingReferenceData(
            "contact descriptions",
            dtos.stream().map(ContactDescriptionTypeDto::getName).toList(),
            existingByNormalisedName
        );
    }

    private void mapExistingOpeningHours(List<OpeningHourTypeDto> dtos) {
        if (isEmpty(dtos)) {
            return;
        }

        Map<String, OpeningHourType> existingByNormalisedName = buildNormalisedLookupMap(
            openingHourTypeRepository.findAll(),
            OpeningHourType::getName
        );
        logMissingReferenceData(
            "opening hour types",
            dtos.stream().map(OpeningHourTypeDto::getName).toList(),
            existingByNormalisedName
        );
    }

    private static boolean isEmpty(Collection<?> values) {
        return values == null || values.isEmpty();
    }

    private static List<UUID> mapIds(
        List<Integer> ids,
        Map<Integer, UUID> lookup,
        String context
    ) {
        if (ids == null || ids.isEmpty()) {
            return List.of();
        }
        List<UUID> results = new java.util.ArrayList<>();
        for (Integer id : ids) {
            UUID mapped = lookup.get(id);
            if (mapped == null) {
                LOG.warn("Unable to map {} identifier '{}' in migration payload", context, id);
                continue;
            }
            results.add(mapped);
        }
        return results.isEmpty() ? List.of() : results;
    }

    private void logMissingReferenceData(
        String category,
        List<String> names,
        Map<String, ?> existingByNormalisedName
    ) {
        List<String> unmatchedNames = names.stream()
            .filter(StringUtils::isNotBlank)
            .filter(name -> findNormalisedMatch(name, existingByNormalisedName).isEmpty())
            .toList();

        if (unmatchedNames.isEmpty()) {
            return;
        }

        String examples = unmatchedNames.stream().limit(10).collect(Collectors.joining(", "));
        LOG.warn(
            "{} {} from the legacy export were not found in the target database. Examples: {}",
            unmatchedNames.size(),
            category,
            examples
        );
    }

    private static <T> Map<String, T> buildNormalisedLookupMap(
        List<T> values,
        Function<T, String> nameExtractor
    ) {
        if (isEmpty(values)) {
            return Map.of();
        }

        return values.stream()
            .filter(Objects::nonNull)
            .map(value -> Map.entry(normaliseLookupName(nameExtractor.apply(value)), value))
            .filter(entry -> StringUtils.isNotBlank(entry.getKey()))
            .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (left, right) -> left));
    }

    private static <T> Map<String, T> buildCaseInsensitiveLookupMap(
        List<T> values,
        Function<T, String> nameExtractor
    ) {
        if (isEmpty(values)) {
            return Map.of();
        }

        return values.stream()
            .filter(Objects::nonNull)
            .map(value -> Map.entry(normaliseCaseInsensitiveName(nameExtractor.apply(value)), value))
            .filter(entry -> StringUtils.isNotBlank(entry.getKey()))
            .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (left, right) -> left));
    }

    private static List<LocalAuthorityType> findLocalAuthorityMatches(
        String name,
        Map<String, LocalAuthorityType> caseInsensitiveLookup,
        Map<String, LocalAuthorityType> normalisedLookup
    ) {
        LocalAuthorityType exact = caseInsensitiveLookup.get(normaliseCaseInsensitiveName(name));
        if (exact != null) {
            return List.of(exact);
        }

        List<String> aliases = LEGACY_LOCAL_AUTHORITY_NAME_ALIASES.get(normaliseCaseInsensitiveName(name));
        if (aliases != null) {
            List<LocalAuthorityType> aliasMatches = aliases.stream()
                .map(alias -> caseInsensitiveLookup.get(normaliseCaseInsensitiveName(alias)))
                .filter(Objects::nonNull)
                .distinct()
                .toList();
            if (!aliasMatches.isEmpty()) {
                return aliasMatches;
            }
        }

        return findNormalisedMatch(name, normalisedLookup).stream().toList();
    }

    private static <T> Optional<T> findNormalisedMatch(String name, Map<String, T> lookup) {
        String normalisedName = normaliseLookupName(name);
        if (StringUtils.isBlank(normalisedName)) {
            return Optional.empty();
        }

        T exact = lookup.get(normalisedName);
        if (exact != null) {
            return Optional.of(exact);
        }

        Set<String> sourceTokens = tokeniseNormalisedName(normalisedName);
        if (sourceTokens.isEmpty()) {
            return Optional.empty();
        }

        List<T> partialMatches = lookup.entrySet().stream()
            .filter(entry -> {
                Set<String> targetTokens = tokeniseNormalisedName(entry.getKey());
                return targetTokens.containsAll(sourceTokens) || sourceTokens.containsAll(targetTokens);
            })
            .map(Map.Entry::getValue)
            .distinct()
            .toList();

        return partialMatches.size() == 1
            ? Optional.of(partialMatches.get(0))
            : Optional.empty();
    }

    private static Set<String> tokeniseNormalisedName(String normalisedName) {
        Set<String> tokens = new LinkedHashSet<>();
        for (String token : StringUtils.split(normalisedName, ' ')) {
            if (StringUtils.isBlank(token)) {
                continue;
            }
            tokens.add(token);
        }
        return tokens;
    }

    private static String normaliseLookupName(String name) {
        if (StringUtils.isBlank(name)) {
            return "";
        }

        String normalised = name.toLowerCase()
            .replace("&", " and ")
            .replaceAll("[^a-z0-9 ]+", " ");

        String[] tokens = StringUtils.split(normalised);
        if (tokens == null) {
            return "";
        }

        List<String> singularisedTokens = java.util.Arrays.stream(tokens)
            .map(ReferenceDataImporter::singulariseToken)
            .toList();

        List<String> filteredTokens = singularisedTokens.stream()
            .filter(token -> !LOOKUP_STOP_WORDS.contains(token))
            .toList();

        return (filteredTokens.isEmpty() ? singularisedTokens : filteredTokens).stream()
            .collect(Collectors.joining(" "));
    }

    private static String normaliseCaseInsensitiveName(String name) {
        return StringUtils.lowerCase(StringUtils.normalizeSpace(StringUtils.defaultString(name)));
    }

    private static String singulariseToken(String token) {
        if (token.length() > 3 && token.endsWith("s")) {
            return token.substring(0, token.length() - 1);
        }
        return token;
    }
}
