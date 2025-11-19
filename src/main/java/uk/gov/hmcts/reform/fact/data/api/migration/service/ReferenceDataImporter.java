package uk.gov.hmcts.reform.fact.data.api.migration.service;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
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
import uk.gov.hmcts.reform.fact.data.api.repositories.OpeningHourTypeRepository;
import uk.gov.hmcts.reform.fact.data.api.repositories.RegionRepository;
import uk.gov.hmcts.reform.fact.data.api.repositories.ServiceAreaRepository;
import uk.gov.hmcts.reform.fact.data.api.migration.repository.LegacyServiceRepository;

/**
 * Handles one-off reference data mapping before the court entities are persisted. This keeps the
 * main migration service focused on orchestration while this helper deals with translating legacy
 * IDs into the UUIDs generated in the new schema.
 */
class ReferenceDataImporter {

    private static final Logger LOG = LoggerFactory.getLogger(ReferenceDataImporter.class);

    private final RegionRepository regionRepository;
    private final AreaOfLawTypeRepository areaOfLawTypeRepository;
    private final ServiceAreaRepository serviceAreaRepository;
    private final LegacyServiceRepository legacyServiceRepository;
    private final LocalAuthorityTypeRepository localAuthorityTypeRepository;
    private final ContactDescriptionTypeRepository contactDescriptionTypeRepository;
    private final OpeningHourTypeRepository openingHourTypeRepository;

    ReferenceDataImporter(
        RegionRepository regionRepository,
        AreaOfLawTypeRepository areaOfLawTypeRepository,
        ServiceAreaRepository serviceAreaRepository,
        LegacyServiceRepository legacyServiceRepository,
        LocalAuthorityTypeRepository localAuthorityTypeRepository,
        ContactDescriptionTypeRepository contactDescriptionTypeRepository,
        OpeningHourTypeRepository openingHourTypeRepository
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
            AreaOfLawType entity = areaOfLawTypeRepository.findByNameIgnoreCase(dto.getName())
                .orElseThrow(() -> new MigrationClientException(
                    "Area of law '%s' was not found in the target database".formatted(dto.getName())
                ));
            ids.put(dto.getId(), entity.getId());
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
        Map<Integer, UUID> ids
    ) {
        if (isEmpty(localAuthorityTypes)) {
            return;
        }

        for (LocalAuthorityTypeDto dto : localAuthorityTypes) {
            if (StringUtils.isBlank(dto.getName())) {
                LOG.warn("Skipping local authority type with id {} because name is blank", dto.getId());
                continue;
            }

            Optional<LocalAuthorityType> existing = localAuthorityTypeRepository.findByName(dto.getName());
            if (existing.isEmpty()) {
                LOG.warn("No matching local authority type found for name '{}'", dto.getName());
                continue;
            }

            ids.put(dto.getId(), existing.get().getId());
        }
    }

    private void mapExistingContactDescriptions(List<ContactDescriptionTypeDto> dtos) {
        if (isEmpty(dtos)) {
            return;
        }

        Map<String, ContactDescriptionType> existing = contactDescriptionTypeRepository.findAll().stream()
            .collect(java.util.stream.Collectors.toMap(
                ContactDescriptionType::getName, type -> type, (left, right) -> left
            ));

        for (ContactDescriptionTypeDto dto : dtos) {
            if (!existing.containsKey(dto.getName())) {
                LOG.warn("Contact description '{}' was not found in the target database", dto.getName());
            }
        }
    }

    private void mapExistingOpeningHours(List<OpeningHourTypeDto> dtos) {
        if (isEmpty(dtos)) {
            return;
        }

        Map<String, OpeningHourType> existing = openingHourTypeRepository.findAll().stream()
            .collect(java.util.stream.Collectors.toMap(
                OpeningHourType::getName, type -> type, (left, right) -> left
            ));

        for (OpeningHourTypeDto dto : dtos) {
            if (!existing.containsKey(dto.getName())) {
                LOG.warn("Opening hour type '{}' was not found in the target database", dto.getName());
            }
        }
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
            return null;
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
        return results.isEmpty() ? null : results;
    }
}
