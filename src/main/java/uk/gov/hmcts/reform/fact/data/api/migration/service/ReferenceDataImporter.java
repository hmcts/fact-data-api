package uk.gov.hmcts.reform.fact.data.api.migration.service;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
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
import uk.gov.hmcts.reform.fact.data.api.migration.model.ContactDescriptionTypeDto;
import uk.gov.hmcts.reform.fact.data.api.migration.model.LegacyExportResponse;
import uk.gov.hmcts.reform.fact.data.api.migration.model.LocalAuthorityTypeDto;
import uk.gov.hmcts.reform.fact.data.api.migration.model.MigrationFindingSeverity;
import uk.gov.hmcts.reform.fact.data.api.migration.model.MigrationFindingType;
import uk.gov.hmcts.reform.fact.data.api.migration.model.MigrationSection;
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
    private static final Map<String, String> LEGACY_AREA_OF_LAW_NAME_ALIASES = Map.of(
        "domestic violence", "Domestic abuse"
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
        mapExistingRegions(response.getRegions(), context);
        mapExistingAreasOfLaw(response.getAreaOfLawTypes(), context);
        mapExistingLocalAuthorityTypes(response.getLocalAuthorityTypes(), context);
        mapExistingContactDescriptions(response.getContactDescriptionTypes(), context);
        mapExistingOpeningHours(response.getOpeningHourTypes(), context);
        persistServiceAreas(response.getServiceAreas(), context);
        persistServices(response.getServices(), context);
    }

    /**
     * Maps region identifiers from the legacy export onto the pre-seeded regions in the new schema.
     *
     * @param regions regions supplied by the legacy export.
     * @param context migration context receiving the legacy-to-new identifier mapping.
     */
    private void mapExistingRegions(List<RegionDto> regions, MigrationContext context) {
        if (isEmpty(regions)) {
            return;
        }

        for (RegionDto regionDto : regions) {
            Optional<Region> region =
                regionRepository.findByNameAndCountry(regionDto.getName(), regionDto.getCountry());
            if (region.isEmpty()) {
                context.unmapped(MigrationSection.REGIONS, 1);
                context.finding(
                    MigrationFindingSeverity.REVIEW,
                    MigrationFindingType.UNMAPPED,
                    MigrationSection.REGIONS,
                    "REGION_REFERENCE_NOT_FOUND",
                    null,
                    null,
                    regionDto.getId(),
                    List.of(),
                    List.of(),
                    1,
                    0
                );
                continue;
            }
            context.getRegionIds().put(regionDto.getId(), region.get().getId());
            context.persisted(MigrationSection.REGIONS, 1, 0);
        }
    }

    /**
     * Maps area-of-law identifiers from the legacy export onto the pre-seeded records.
     *
     * @param areaOfLawTypes legacy area-of-law records.
     * @param context migration context receiving legacy-to-new mappings.
     */
    private void mapExistingAreasOfLaw(
        List<uk.gov.hmcts.reform.fact.data.api.migration.model.AreaOfLawTypeDto> areaOfLawTypes,
        MigrationContext context
    ) {
        if (isEmpty(areaOfLawTypes)) {
            return;
        }

        for (uk.gov.hmcts.reform.fact.data.api.migration.model.AreaOfLawTypeDto dto : areaOfLawTypes) {
            String lookupName = areaOfLawLookupName(dto.getName());
            Optional<AreaOfLawType> entity = areaOfLawTypeRepository.findByNameIgnoreCase(lookupName);
            if (entity.isEmpty()) {
                LOG.warn("Area of law '{}' was not found in the target database", dto.getName());
                context.unmapped(MigrationSection.AREAS_OF_LAW, 1);
                context.finding(
                    MigrationFindingSeverity.REVIEW,
                    MigrationFindingType.UNMAPPED,
                    MigrationSection.AREAS_OF_LAW,
                    "AREA_OF_LAW_REFERENCE_NOT_FOUND",
                    null,
                    null,
                    dto.getId(),
                    List.of(),
                    List.of(),
                    1,
                    0
                );
                continue;
            }
            context.getAreaOfLawIds().put(dto.getId(), entity.get().getId());
            context.persisted(MigrationSection.AREAS_OF_LAW, 1, 0);
        }
    }

    private static String areaOfLawLookupName(String legacyName) {
        if (legacyName == null) {
            return null;
        }
        return LEGACY_AREA_OF_LAW_NAME_ALIASES.getOrDefault(
            legacyName.toLowerCase(Locale.ROOT),
            legacyName
        );
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
                context.unmapped(MigrationSection.SERVICE_AREAS, 1);
                context.finding(
                    MigrationFindingSeverity.REVIEW,
                    MigrationFindingType.UNMAPPED,
                    MigrationSection.SERVICE_AREAS,
                    "SERVICE_AREA_REFERENCE_NOT_FOUND",
                    null,
                    null,
                    dto.getId(),
                    List.of(),
                    List.of(),
                    1,
                    0
                );
                continue;
            }
            context.getServiceAreaIds().put(dto.getId(), existing.get().getId());
            context.persisted(MigrationSection.SERVICE_AREAS, 1, 0);
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
            context.finding(
                MigrationFindingSeverity.ERROR,
                MigrationFindingType.REJECTED,
                MigrationSection.SERVICES,
                "SERVICE_SOURCE_COLLECTION_MISSING",
                null,
                null,
                null,
                List.of(),
                List.of(),
                1,
                0
            );
            return;
        }

        for (ServiceDto dto : services) {
            List<String> invalidFields = invalidServiceFields(dto);
            if (!invalidFields.isEmpty()) {
                context.skipped(MigrationSection.SERVICES, 1);
                context.finding(
                    MigrationFindingSeverity.ERROR,
                    MigrationFindingType.REJECTED,
                    MigrationSection.SERVICES,
                    "SERVICE_REQUIRED_FIELDS_MISSING",
                    null,
                    null,
                    dto.getId(),
                    dto.getServiceAreaIds(),
                    invalidFields,
                    1,
                    size(dto.getServiceAreaIds())
                );
                continue;
            }

            List<LegacyService> matches = legacyServiceRepository.findAllByNameIgnoreCase(dto.getName());
            if (matches.size() != 1) {
                context.skipped(MigrationSection.SERVICES, 1);
                context.finding(
                    MigrationFindingSeverity.ERROR,
                    MigrationFindingType.REJECTED,
                    MigrationSection.SERVICES,
                    matches.isEmpty() ? "SEEDED_SERVICE_NOT_FOUND" : "SEEDED_SERVICE_AMBIGUOUS",
                    null,
                    null,
                    dto.getId(),
                    dto.getServiceAreaIds(),
                    List.of("name"),
                    1,
                    size(dto.getServiceAreaIds())
                );
                continue;
            }

            List<Integer> unmappedIds = dto.getServiceAreaIds().stream()
                .filter(id -> !context.getServiceAreaIds().containsKey(id))
                .toList();
            if (!unmappedIds.isEmpty()) {
                context.unmapped(MigrationSection.SERVICE_AREA_LINKS, unmappedIds.size());
                context.skipped(MigrationSection.SERVICES, 1);
                context.finding(
                    MigrationFindingSeverity.ERROR,
                    MigrationFindingType.UNMAPPED,
                    MigrationSection.SERVICE_AREA_LINKS,
                    "SERVICE_AREA_LINK_UNMAPPED",
                    null,
                    null,
                    dto.getId(),
                    unmappedIds,
                    List.of("serviceAreaIds"),
                    1,
                    unmappedIds.size()
                );
                continue;
            }

            LegacyService entity = matches.get(0);
            final List<String> changedFields = changedServiceFields(entity, dto, context);
            entity.setName(dto.getName());
            entity.setNameCy(dto.getNameCy());
            entity.setDescription(dto.getDescription());
            entity.setDescriptionCy(dto.getDescriptionCy());
            entity.setServiceAreas(dto.getServiceAreaIds().stream()
                .map(context.getServiceAreaIds()::get)
                .toList());
            legacyServiceRepository.save(entity);
            context.servicesMigrated++;
            context.serviceAreaLinksMigrated += dto.getServiceAreaIds().size();
            context.persisted(MigrationSection.SERVICES, 1, 0);
            context.persisted(MigrationSection.SERVICE_AREA_LINKS, 0, dto.getServiceAreaIds().size());
            if (!changedFields.isEmpty()) {
                context.transformed(MigrationSection.SERVICES, 1);
                context.finding(
                    MigrationFindingSeverity.INFO,
                    MigrationFindingType.TRANSFORMED,
                    MigrationSection.SERVICES,
                    "SERVICE_FIELDS_UPDATED",
                    null,
                    null,
                    dto.getId(),
                    List.of(),
                    changedFields,
                    1,
                    0
                );
            }
        }
    }

    private static List<String> invalidServiceFields(ServiceDto dto) {
        List<String> invalid = new java.util.ArrayList<>();
        if (StringUtils.isBlank(dto.getName())) {
            invalid.add("name");
        }
        if (StringUtils.isBlank(dto.getNameCy())) {
            invalid.add("nameCy");
        }
        if (dto.getDescription() == null) {
            invalid.add("description");
        }
        if (dto.getDescriptionCy() == null) {
            invalid.add("descriptionCy");
        }
        if (isEmpty(dto.getServiceAreaIds())) {
            invalid.add("serviceAreaIds");
        }
        return invalid;
    }

    private static List<String> changedServiceFields(
        LegacyService entity,
        ServiceDto dto,
        MigrationContext context
    ) {
        List<String> changed = new java.util.ArrayList<>();
        addChanged(changed, "name", entity.getName(), dto.getName());
        addChanged(changed, "nameCy", entity.getNameCy(), dto.getNameCy());
        addChanged(changed, "description", entity.getDescription(), dto.getDescription());
        addChanged(changed, "descriptionCy", entity.getDescriptionCy(), dto.getDescriptionCy());
        List<UUID> mappedServiceAreas = dto.getServiceAreaIds().stream()
            .map(context.getServiceAreaIds()::get)
            .toList();
        addChanged(changed, "serviceAreaIds", entity.getServiceAreas(), mappedServiceAreas);
        return changed;
    }

    private static void addChanged(List<String> fields, String field, Object current, Object proposed) {
        if (!Objects.equals(current, proposed)) {
            fields.add(field);
        }
    }

    private void mapExistingLocalAuthorityTypes(
        List<LocalAuthorityTypeDto> localAuthorityTypes,
        MigrationContext context
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
                recordLocalAuthorityReferenceFinding(context, dto, "LOCAL_AUTHORITY_NAME_BLANK");
                continue;
            }

            List<LocalAuthorityType> existingMatches = findLocalAuthorityMatches(
                dto.getName(),
                existingByExactName,
                existingByNormalisedName
            );
            if (existingMatches.isEmpty()) {
                LOG.warn("No matching local authority type found for name '{}'", dto.getName());
                recordLocalAuthorityReferenceFinding(context, dto, "LOCAL_AUTHORITY_REFERENCE_NOT_FOUND");
                continue;
            }

            context.getLocalAuthorityTypeIds().put(
                dto.getId(),
                existingMatches.stream().map(LocalAuthorityType::getId).distinct().toList()
            );
            context.persisted(MigrationSection.LOCAL_AUTHORITY_TYPES, 1, 0);
        }
    }

    private static void recordLocalAuthorityReferenceFinding(
        MigrationContext context,
        LocalAuthorityTypeDto dto,
        String reasonCode
    ) {
        context.unmapped(MigrationSection.LOCAL_AUTHORITY_TYPES, 1);
        context.finding(
            MigrationFindingSeverity.REVIEW,
            MigrationFindingType.UNMAPPED,
            MigrationSection.LOCAL_AUTHORITY_TYPES,
            reasonCode,
            null,
            null,
            dto.getId(),
            List.of(),
            List.of(),
            1,
            0
        );
    }

    private void mapExistingContactDescriptions(List<ContactDescriptionTypeDto> dtos, MigrationContext context) {
        if (isEmpty(dtos)) {
            return;
        }

        Map<String, ContactDescriptionType> existingByNormalisedName = buildNormalisedLookupMap(
            contactDescriptionTypeRepository.findAll(),
            ContactDescriptionType::getName
        );
        int missing = logMissingReferenceData(
            "contact descriptions",
            dtos.stream().map(ContactDescriptionTypeDto::getName).toList(),
            existingByNormalisedName
        );
        context.persisted(MigrationSection.CONTACT_DESCRIPTION_TYPES, dtos.size() - missing, 0);
        recordDiagnosticReferenceFinding(
            context,
            MigrationSection.CONTACT_DESCRIPTION_TYPES,
            "CONTACT_DESCRIPTION_DEFERRED_TO_FORMS",
            missing
        );
    }

    private void mapExistingOpeningHours(List<OpeningHourTypeDto> dtos, MigrationContext context) {
        if (isEmpty(dtos)) {
            return;
        }

        Map<String, OpeningHourType> existingByNormalisedName = buildNormalisedLookupMap(
            openingHourTypeRepository.findAll(),
            OpeningHourType::getName
        );
        int missing = logMissingReferenceData(
            "opening hour types",
            dtos.stream().map(OpeningHourTypeDto::getName).toList(),
            existingByNormalisedName
        );
        context.persisted(MigrationSection.OPENING_HOUR_TYPES, dtos.size() - missing, 0);
        recordDiagnosticReferenceFinding(
            context,
            MigrationSection.OPENING_HOUR_TYPES,
            "OPENING_HOUR_TYPE_DEFERRED_TO_FORMS",
            missing
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

    private int logMissingReferenceData(
        String category,
        List<String> names,
        Map<String, ?> existingByNormalisedName
    ) {
        List<String> unmatchedNames = names.stream()
            .filter(StringUtils::isNotBlank)
            .filter(name -> findNormalisedMatch(name, existingByNormalisedName).isEmpty())
            .toList();

        if (unmatchedNames.isEmpty()) {
            return 0;
        }

        String examples = unmatchedNames.stream().limit(10).collect(Collectors.joining(", "));
        LOG.warn(
            "{} {} from the legacy export were not found in the target database. Examples: {}",
            unmatchedNames.size(),
            category,
            examples
        );
        return unmatchedNames.size();
    }

    private static void recordDiagnosticReferenceFinding(
        MigrationContext context,
        MigrationSection section,
        String reasonCode,
        int missing
    ) {
        if (missing == 0) {
            return;
        }
        context.unmapped(section, missing);
        context.finding(
            MigrationFindingSeverity.REVIEW,
            MigrationFindingType.DEFERRED,
            section,
            reasonCode,
            null,
            null,
            null,
            List.of(),
            List.of(),
            missing,
            0
        );
    }

    private static int size(Collection<?> values) {
        return values == null ? 0 : values.size();
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
