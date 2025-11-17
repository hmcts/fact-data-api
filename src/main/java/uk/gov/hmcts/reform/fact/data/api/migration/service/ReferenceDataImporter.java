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

    void importReferenceData(LegacyExportResponse response, MigrationContext context) {
        mapExistingRegions(response.regions(), context.getRegionIds());
        mapExistingAreasOfLaw(response.areaOfLawTypes(), context.getAreaOfLawIds());
        mapExistingLocalAuthorityTypes(response.localAuthorityTypes(), context.getLocalAuthorityTypeIds());
        mapExistingContactDescriptions(response.contactDescriptionTypes());
        mapExistingOpeningHours(response.openingHourTypes());
        persistServiceAreas(response.serviceAreas(), context);
        persistServices(response.services(), context);
    }

    private void mapExistingRegions(List<RegionDto> regions, Map<Integer, UUID> regionIds) {
        if (isEmpty(regions)) {
            return;
        }

        for (RegionDto regionDto : regions) {
            Region region = regionRepository.findByNameAndCountry(regionDto.name(), regionDto.country())
                .orElseThrow(() -> new IllegalStateException(
                    "Region '%s' (%s) was not found in the target database".formatted(
                        regionDto.name(), regionDto.country()
                    )
                ));
            regionIds.put(regionDto.id(), region.getId());
        }
    }

    private void mapExistingAreasOfLaw(
        List<uk.gov.hmcts.reform.fact.data.api.migration.model.AreaOfLawTypeDto> areaOfLawTypes,
        Map<Integer, UUID> ids
    ) {
        if (isEmpty(areaOfLawTypes)) {
            return;
        }

        for (uk.gov.hmcts.reform.fact.data.api.migration.model.AreaOfLawTypeDto dto : areaOfLawTypes) {
            AreaOfLawType entity = areaOfLawTypeRepository.findByNameIgnoreCase(dto.name())
                .orElseThrow(() -> new IllegalStateException(
                    "Area of law '%s' was not found in the target database".formatted(dto.name())
                ));
            ids.put(dto.id(), entity.getId());
        }
    }

    private void persistServiceAreas(List<ServiceAreaDto> serviceAreas, MigrationContext context) {
        if (isEmpty(serviceAreas)) {
            return;
        }

        for (ServiceAreaDto dto : serviceAreas) {
            Optional<ServiceArea> existing = serviceAreaRepository.findByNameIgnoreCase(dto.name());
            if (existing.isEmpty()) {
                LOG.warn("Service area '{}' was not found in the target database", dto.name());
                continue;
            }
            context.getServiceAreaIds().put(dto.id(), existing.get().getId());
        }
    }

    private void persistServices(List<ServiceDto> services, MigrationContext context) {
        if (isEmpty(services)) {
            return;
        }

        for (ServiceDto dto : services) {
            LegacyService entity = legacyServiceRepository.findByName(dto.name())
                .orElseGet(LegacyService::new);
            entity.setName(dto.name());
            entity.setNameCy(dto.nameCy());
            entity.setDescription(dto.description());
            entity.setDescriptionCy(dto.descriptionCy());
            entity.setServiceAreas(mapIds(dto.serviceAreaIds(), context.getServiceAreaIds(), "service area"));
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
            if (StringUtils.isBlank(dto.name())) {
                LOG.warn("Skipping local authority type with id {} because name is blank", dto.id());
                continue;
            }

            Optional<LocalAuthorityType> existing = localAuthorityTypeRepository.findByName(dto.name());
            if (existing.isEmpty()) {
                LOG.warn("No matching local authority type found for name '{}'", dto.name());
                continue;
            }

            ids.put(dto.id(), existing.get().getId());
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
            if (!existing.containsKey(dto.name())) {
                LOG.warn("Contact description '{}' was not found in the target database", dto.name());
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
            if (!existing.containsKey(dto.name())) {
                LOG.warn("Opening hour type '{}' was not found in the target database", dto.name());
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
