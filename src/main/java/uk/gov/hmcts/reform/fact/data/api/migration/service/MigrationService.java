package uk.gov.hmcts.reform.fact.data.api.migration.service;

import jakarta.validation.ConstraintViolationException;
import java.time.Instant;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;
import uk.gov.hmcts.reform.fact.data.api.entities.AreaOfLawType;
import uk.gov.hmcts.reform.fact.data.api.entities.ContactDescriptionType;
import uk.gov.hmcts.reform.fact.data.api.entities.Court;
import uk.gov.hmcts.reform.fact.data.api.entities.CourtAreasOfLaw;
import uk.gov.hmcts.reform.fact.data.api.entities.CourtCodes;
import uk.gov.hmcts.reform.fact.data.api.entities.CourtLocalAuthorities;
import uk.gov.hmcts.reform.fact.data.api.entities.CourtDxCode;
import uk.gov.hmcts.reform.fact.data.api.entities.CourtFax;
import uk.gov.hmcts.reform.fact.data.api.entities.CourtProfessionalInformation;
import uk.gov.hmcts.reform.fact.data.api.entities.CourtServiceAreas;
import uk.gov.hmcts.reform.fact.data.api.entities.CourtSinglePointsOfEntry;
import uk.gov.hmcts.reform.fact.data.api.entities.CourtType;
import uk.gov.hmcts.reform.fact.data.api.entities.LocalAuthorityType;
import uk.gov.hmcts.reform.fact.data.api.entities.OpeningHourType;
import uk.gov.hmcts.reform.fact.data.api.entities.Region;
import uk.gov.hmcts.reform.fact.data.api.entities.ServiceArea;
import uk.gov.hmcts.reform.fact.data.api.entities.validation.ValidationConstants;
import uk.gov.hmcts.reform.fact.data.api.entities.types.CatchmentMethod;
import uk.gov.hmcts.reform.fact.data.api.entities.types.CatchmentType;
import uk.gov.hmcts.reform.fact.data.api.entities.types.ServiceAreaType;
import uk.gov.hmcts.reform.fact.data.api.migration.client.LegacyFactClient;
import uk.gov.hmcts.reform.fact.data.api.migration.exception.MigrationAlreadyAppliedException;
import uk.gov.hmcts.reform.fact.data.api.migration.exception.MigrationClientException;
import uk.gov.hmcts.reform.fact.data.api.migration.entities.LegacyCourtMapping;
import uk.gov.hmcts.reform.fact.data.api.migration.entities.LegacyService;
import uk.gov.hmcts.reform.fact.data.api.migration.entities.MigrationAudit;
import uk.gov.hmcts.reform.fact.data.api.migration.entities.MigrationStatus;
import uk.gov.hmcts.reform.fact.data.api.migration.model.AreaOfLawTypeDto;
import uk.gov.hmcts.reform.fact.data.api.migration.model.ContactDescriptionTypeDto;
import uk.gov.hmcts.reform.fact.data.api.migration.model.CourtDto;
import uk.gov.hmcts.reform.fact.data.api.migration.model.CourtLocalAuthorityDto;
import uk.gov.hmcts.reform.fact.data.api.migration.model.CourtProfessionalInformationDto;
import uk.gov.hmcts.reform.fact.data.api.migration.model.LegacyExportResponse;
import uk.gov.hmcts.reform.fact.data.api.migration.model.MigrationResult;
import uk.gov.hmcts.reform.fact.data.api.migration.model.MigrationSummary;
import uk.gov.hmcts.reform.fact.data.api.migration.model.OpeningHourTypeDto;
import uk.gov.hmcts.reform.fact.data.api.migration.model.ServiceAreaDto;
import uk.gov.hmcts.reform.fact.data.api.migration.model.ServiceDto;
import uk.gov.hmcts.reform.fact.data.api.migration.model.CourtServiceAreaDto;
import uk.gov.hmcts.reform.fact.data.api.migration.model.CourtAreasOfLawDto;
import uk.gov.hmcts.reform.fact.data.api.migration.model.CourtSinglePointOfEntryDto;
import uk.gov.hmcts.reform.fact.data.api.migration.model.CourtCodesDto;
import uk.gov.hmcts.reform.fact.data.api.migration.model.CourtDxCodeDto;
import uk.gov.hmcts.reform.fact.data.api.migration.model.CourtFaxDto;
import uk.gov.hmcts.reform.fact.data.api.migration.model.LocalAuthorityTypeDto;
import uk.gov.hmcts.reform.fact.data.api.migration.model.CourtTypeDto;
import uk.gov.hmcts.reform.fact.data.api.migration.model.RegionDto;
import uk.gov.hmcts.reform.fact.data.api.repositories.AreaOfLawTypeRepository;
import uk.gov.hmcts.reform.fact.data.api.repositories.ContactDescriptionTypeRepository;
import uk.gov.hmcts.reform.fact.data.api.repositories.CourtAreasOfLawRepository;
import uk.gov.hmcts.reform.fact.data.api.repositories.CourtCodesRepository;
import uk.gov.hmcts.reform.fact.data.api.repositories.CourtDxCodeRepository;
import uk.gov.hmcts.reform.fact.data.api.repositories.CourtFaxRepository;
import uk.gov.hmcts.reform.fact.data.api.repositories.CourtRepository;
import uk.gov.hmcts.reform.fact.data.api.repositories.CourtProfessionalInformationRepository;
import uk.gov.hmcts.reform.fact.data.api.repositories.CourtServiceAreasRepository;
import uk.gov.hmcts.reform.fact.data.api.repositories.CourtSinglePointsOfEntryRepository;
import uk.gov.hmcts.reform.fact.data.api.repositories.CourtTypeRepository;
import uk.gov.hmcts.reform.fact.data.api.repositories.CourtLocalAuthoritiesRepository;
import uk.gov.hmcts.reform.fact.data.api.repositories.LocalAuthorityTypeRepository;
import uk.gov.hmcts.reform.fact.data.api.repositories.OpeningHourTypeRepository;
import uk.gov.hmcts.reform.fact.data.api.repositories.RegionRepository;
import uk.gov.hmcts.reform.fact.data.api.repositories.ServiceAreaRepository;
import uk.gov.hmcts.reform.fact.data.api.migration.repository.LegacyCourtMappingRepository;
import uk.gov.hmcts.reform.fact.data.api.migration.repository.LegacyServiceRepository;
import uk.gov.hmcts.reform.fact.data.api.migration.repository.MigrationAuditRepository;
import uk.gov.hmcts.reform.fact.data.api.services.CourtService;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Pattern;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Coordinates the end-to-end migration workflow. Fetches the legacy payload, maps it into the
 * new entity model, and persists it within a single transaction so that partial imports are
 * automatically rolled back.
 */
@Service
public class MigrationService {

    private static final Logger LOG = LoggerFactory.getLogger(MigrationService.class);

    private final LegacyFactClient legacyFactClient;

    private final RegionRepository regionRepository;
    private final AreaOfLawTypeRepository areaOfLawTypeRepository;
    private final ServiceAreaRepository serviceAreaRepository;
    private final LegacyServiceRepository legacyServiceRepository;
    private final LegacyCourtMappingRepository legacyCourtMappingRepository;
    private final LocalAuthorityTypeRepository localAuthorityTypeRepository;
    private final ContactDescriptionTypeRepository contactDescriptionTypeRepository;
    private final OpeningHourTypeRepository openingHourTypeRepository;
    private final CourtTypeRepository courtTypeRepository;
    private final CourtRepository courtRepository;
    private final CourtServiceAreasRepository courtServiceAreasRepository;
    private final CourtAreasOfLawRepository courtAreasOfLawRepository;
    private final CourtSinglePointsOfEntryRepository courtSinglePointsOfEntryRepository;
    private final CourtLocalAuthoritiesRepository courtLocalAuthoritiesRepository;
    private final CourtProfessionalInformationRepository courtProfessionalInformationRepository;
    private final CourtCodesRepository courtCodesRepository;
    private final CourtDxCodeRepository courtDxCodeRepository;
    private final CourtFaxRepository courtFaxRepository;
    private final MigrationAuditRepository migrationAuditRepository;
    private final TransactionTemplate transactionTemplate;
    private final CourtService courtService;
    private static final Pattern GENERIC_DESCRIPTION_PATTERN =
        Pattern.compile(ValidationConstants.GENERIC_DESCRIPTION_REGEX);
    private static final Pattern COURT_NAME_PATTERN =
        Pattern.compile(ValidationConstants.COURT_NAME_REGEX);
    private static final String SERVICE_CENTRE_REGION_NAME = "Service Centre";
    private static final String SERVICE_CENTRE_REGION_COUNTRY = "England";
    private static final String DATA_MIGRATION_NAME = "legacy-data-migration";

    public MigrationService(
        LegacyFactClient legacyFactClient,
        RegionRepository regionRepository,
        AreaOfLawTypeRepository areaOfLawTypeRepository,
        ServiceAreaRepository serviceAreaRepository,
        LegacyServiceRepository legacyServiceRepository,
        LegacyCourtMappingRepository legacyCourtMappingRepository,
        LocalAuthorityTypeRepository localAuthorityTypeRepository,
        ContactDescriptionTypeRepository contactDescriptionTypeRepository,
        OpeningHourTypeRepository openingHourTypeRepository,
        CourtTypeRepository courtTypeRepository,
        CourtRepository courtRepository,
        CourtServiceAreasRepository courtServiceAreasRepository,
        CourtAreasOfLawRepository courtAreasOfLawRepository,
        CourtSinglePointsOfEntryRepository courtSinglePointsOfEntryRepository,
        CourtLocalAuthoritiesRepository courtLocalAuthoritiesRepository,
        CourtProfessionalInformationRepository courtProfessionalInformationRepository,
        CourtCodesRepository courtCodesRepository,
        CourtDxCodeRepository courtDxCodeRepository,
        CourtFaxRepository courtFaxRepository,
        MigrationAuditRepository migrationAuditRepository,
        TransactionTemplate transactionTemplate,
        CourtService courtService
    ) {
        this.legacyFactClient = legacyFactClient;
        this.regionRepository = regionRepository;
        this.areaOfLawTypeRepository = areaOfLawTypeRepository;
        this.serviceAreaRepository = serviceAreaRepository;
        this.legacyServiceRepository = legacyServiceRepository;
        this.legacyCourtMappingRepository = legacyCourtMappingRepository;
        this.localAuthorityTypeRepository = localAuthorityTypeRepository;
        this.contactDescriptionTypeRepository = contactDescriptionTypeRepository;
        this.openingHourTypeRepository = openingHourTypeRepository;
        this.courtTypeRepository = courtTypeRepository;
        this.courtRepository = courtRepository;
        this.courtServiceAreasRepository = courtServiceAreasRepository;
        this.courtAreasOfLawRepository = courtAreasOfLawRepository;
        this.courtSinglePointsOfEntryRepository = courtSinglePointsOfEntryRepository;
        this.courtLocalAuthoritiesRepository = courtLocalAuthoritiesRepository;
        this.courtProfessionalInformationRepository = courtProfessionalInformationRepository;
        this.courtCodesRepository = courtCodesRepository;
        this.courtDxCodeRepository = courtDxCodeRepository;
        this.courtFaxRepository = courtFaxRepository;
        this.migrationAuditRepository = migrationAuditRepository;
        this.transactionTemplate = transactionTemplate;
        this.courtService = courtService;
    }

    /**
     * Executes the migration by first validating that it can run, then retrieving the payload
     * from the legacy FaCT endpoint.
     *
     * @return summary of the entities that were migrated.
     */
    public MigrationSummary migrate() {
        guardAgainstDuplicateExecution();

        markMigrationStatus(MigrationStatus.IN_PROGRESS);
        try {
            final LegacyExportResponse exportResponse = Optional.ofNullable(legacyFactClient.fetchExport())
                .orElseThrow(() -> new MigrationClientException("Legacy export response was empty"));

            MigrationSummary summary = transactionTemplate.execute(status -> persistExport(exportResponse));
            markMigrationStatus(MigrationStatus.SUCCESS);
            return summary;
        } catch (RuntimeException ex) {
            markMigrationStatus(MigrationStatus.FAILED);
            throw ex;
        }
    }

    /**
     * Persists the supplied export payload inside a database transaction.
     *
     * @param exportResponse payload returned by the legacy system.
     * @return summary of the entities that were persisted.
     */
    MigrationSummary persistExport(LegacyExportResponse exportResponse) {
        final MigrationContext context = new MigrationContext();

        mapExistingRegions(exportResponse.regions(), context.regionIds);
        mapExistingAreasOfLaw(exportResponse.areaOfLawTypes(), context.areaOfLawIds);
        persistServiceAreas(exportResponse.serviceAreas(), context);
        persistServices(exportResponse.services(), context);
        mapExistingLocalAuthorityTypes(exportResponse.localAuthorityTypes(), context.localAuthorityTypeIds);
        mapExistingContactDescriptions(exportResponse.contactDescriptionTypes());
        mapExistingOpeningHours(exportResponse.openingHourTypes());
        final int courtsMigrated = persistCourts(exportResponse.courts(), context);

        MigrationResult result = new MigrationResult(
            courtsMigrated,
            context.courtAreasOfLawMigrated,
            context.courtServiceAreasMigrated,
            context.courtLocalAuthoritiesMigrated,
            context.courtSinglePointsOfEntryMigrated,
            context.courtProfessionalInformationMigrated,
            context.courtCodesMigrated,
            context.courtDxCodesMigrated,
            context.courtFaxMigrated
        );

        return new MigrationSummary(result);
    }

    private void guardAgainstDuplicateExecution() {
        Optional<MigrationAudit> audit = migrationAuditRepository.findByMigrationName(DATA_MIGRATION_NAME);
        if (audit.isEmpty()) {
            return;
        }
        MigrationStatus status = audit.get().getStatus();
        if (status == MigrationStatus.SUCCESS) {
            throw new MigrationAlreadyAppliedException(
                "Legacy data migration has already been applied successfully. "
                    + "If you need to rerun it, reset the migration audit record first."
            );
        }
        if (status == MigrationStatus.IN_PROGRESS) {
            throw new MigrationAlreadyAppliedException(
                "Legacy data migration is already running. Please wait for it to finish."
            );
        }
    }

    private void markMigrationStatus(MigrationStatus status) {
        MigrationAudit audit = migrationAuditRepository.findByMigrationName(DATA_MIGRATION_NAME)
            .orElseGet(() -> MigrationAudit.builder()
                .migrationName(DATA_MIGRATION_NAME)
                .build());
        audit.setStatus(status);
        audit.setUpdatedAt(Instant.now());
        migrationAuditRepository.save(audit);
    }

    /**
     * Writes regions to the database and records the mapping between the legacy integer ID and
     * the generated UUID so that related entities can be linked up later in the import.
     *
     * @param regions   regions supplied by the legacy export.
     * @param regionIds map used to store the legacy-to-new identifier mapping.
     */
    private void mapExistingRegions(List<RegionDto> regions, Map<Integer, UUID> regionIds) {
        if (isEmpty(regions)) {
            return;
        }

        for (RegionDto regionDto : regions) {
            Region region = regionRepository.findByNameAndCountry(regionDto.name(), regionDto.country())
                .orElseThrow(() -> new MigrationClientException(
                    "Region '%s' (%s) was not found in the target database".formatted(
                        regionDto.name(), regionDto.country()
                    )
                ));
            regionIds.put(regionDto.id(), region.getId());
        }
    }

    /**
     * Persists area-of-law records and stores the legacy to UUID mapping.
     *
     * @param areaOfLawTypes legacy area-of-law records.
     * @param ids            map used to store the converted identifiers.
     * @return the number of area-of-law records migrated.
     */
    private int mapExistingAreasOfLaw(
        List<AreaOfLawTypeDto> areaOfLawTypes,
        Map<Integer, UUID> ids
    ) {
        if (isEmpty(areaOfLawTypes)) {
            return 0;
        }

        for (AreaOfLawTypeDto dto : areaOfLawTypes) {
            AreaOfLawType entity = areaOfLawTypeRepository.findByNameIgnoreCase(dto.name())
                .orElseThrow(() -> new MigrationClientException(
                    "Area of law '%s' was not found in the target database".formatted(dto.name())
                ));
            ids.put(dto.id(), entity.getId());
        }
        return 0;
    }

    /**
     * Persists service areas after translating legacy references (area of law, enums) into the
     * new representations.
     *
     * @param serviceAreas legacy service area records.
     * @param context      migration context containing ID mappings populated so far.
     * @return the number of service area records migrated.
     */
    private int persistServiceAreas(List<ServiceAreaDto> serviceAreas, MigrationContext context) {
        if (isEmpty(serviceAreas)) {
            return 0;
        }

        int mapped = 0;
        for (ServiceAreaDto dto : serviceAreas) {
            Optional<ServiceArea> existing = serviceAreaRepository.findByNameIgnoreCase(dto.name());
            if (existing.isEmpty()) {
                LOG.warn("Service area '{}' was not found in the target database", dto.name());
                continue;
            }
            context.serviceAreaIds.put(dto.id(), existing.get().getId());
            mapped++;
        }
        return mapped;
    }

    /**
     * Persists the provided services and associates them with the migrated service areas.
     *
     * @param services legacy service records.
     * @param context  migration context containing ID mappings populated so far.
     * @return the number of provided services migrated.
     */
    private int persistServices(List<ServiceDto> services, MigrationContext context) {
        if (isEmpty(services)) {
            return 0;
        }

        int processed = 0;
        for (ServiceDto dto : services) {
            List<UUID> serviceAreaIds = mapIds(dto.serviceAreaIds(), context.serviceAreaIds, "service area");
            LegacyService entity = legacyServiceRepository.findByName(dto.name())
                .orElseGet(LegacyService::new);
            entity.setName(dto.name());
            entity.setNameCy(dto.nameCy());
            entity.setDescription(dto.description());
            entity.setDescriptionCy(dto.descriptionCy());
            entity.setServiceAreas(serviceAreaIds);
            legacyServiceRepository.save(entity);
            processed++;
        }
        return processed;
    }

    /**
     * Migrates the static local authority type reference data.
     *
     * @param localAuthorityTypes legacy local authority types.
     * @param ids mapping between legacy identifiers and the UUIDs already present in the database.
     * @return the number of records migrated.
     */
    private int mapExistingLocalAuthorityTypes(
        List<LocalAuthorityTypeDto> localAuthorityTypes,
        Map<Integer, UUID> ids
    ) {
        if (isEmpty(localAuthorityTypes)) {
            return 0;
        }

        int mapped = 0;
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
            mapped++;
        }
        return mapped;
    }

    /**
     * Migrates the contact description reference data.
     *
     * @param dtos legacy contact description types.
     * @return the number of records migrated.
     */
    private int mapExistingContactDescriptions(List<ContactDescriptionTypeDto> dtos) {
        if (isEmpty(dtos)) {
            return 0;
        }

        Map<String, ContactDescriptionType> existing = contactDescriptionTypeRepository.findAll().stream()
            .collect(Collectors.toMap(ContactDescriptionType::getName, type -> type, (left, right) -> left));

        for (ContactDescriptionTypeDto dto : dtos) {
            ContactDescriptionType entity = existing.get(dto.name());
            if (entity == null) {
                LOG.warn("Contact description '{}' was not found in the target database", dto.name());
                continue;
            }
            // no id mapping needed; nothing else references these in migration
        }
        return 0;
    }

    /**
     * Migrates the opening hour types reference data.
     *
     * @param dtos legacy opening hour types.
     * @return the number of records migrated.
     */
    private int mapExistingOpeningHours(List<OpeningHourTypeDto> dtos) {
        if (isEmpty(dtos)) {
            return 0;
        }

        Map<String, OpeningHourType> existing = openingHourTypeRepository.findAll().stream()
            .collect(Collectors.toMap(OpeningHourType::getName, type -> type, (left, right) -> left));

        for (OpeningHourTypeDto dto : dtos) {
            if (!existing.containsKey(dto.name())) {
                LOG.warn("Opening hour type '{}' was not found in the target database", dto.name());
            }
        }
        return 0;
    }

    /**
     * Migrates the court type reference data.
     *
     * @param dtos legacy court types.
     * @return the number of records migrated.
     */
    private int mapExistingCourtTypes(List<CourtTypeDto> dtos) {
        if (isEmpty(dtos)) {
            return 0;
        }

        Map<String, CourtType> existing = courtTypeRepository.findAll().stream()
            .collect(Collectors.toMap(CourtType::getName, type -> type, (left, right) -> left));

        for (CourtTypeDto dto : dtos) {
            if (!existing.containsKey(dto.name())) {
                LOG.warn("Court type '{}' was not found in the target database", dto.name());
            }
        }
        return 0;
    }

    /**
     * Migrates each court and its related collections (service areas, areas of law, postcodes, etc.).
     *
     * @param courts  legacy court records.
     * @param context migration context containing ID mappings populated so far.
     * @return the number of courts migrated.
     */
    private int persistCourts(List<CourtDto> courts, MigrationContext context) {
        if (isEmpty(courts)) {
            return 0;
        }

        int total = 0;
        for (CourtDto dto : courts) {
            UUID regionId = resolveRegionId(dto, context);
            if (regionId == null) {
                LOG.warn("Skipping court {} because region {} was not migrated", dto.slug(), dto.regionId());
                continue;
            }

            String courtName = sanitiseCourtName(dto.name());
            if (StringUtils.isBlank(courtName)) {
                LOG.warn("Skipping court {} because sanitised name was blank", dto.slug());
                continue;
            }
            if (!COURT_NAME_PATTERN.matcher(courtName).matches()) {
                LOG.warn(
                    "Skipping court {} because sanitised name '{}' still fails validation regex",
                    dto.slug(),
                    courtName
                );
                continue;
            }

            Court court = Court.builder()
                .name(courtName)
                .slug(dto.slug())
                .open(dto.open())
                .regionId(regionId)
                .isServiceCentre(dto.isServiceCentre())
                .build();

            Court savedCourt;
            try {
                savedCourt = courtService.createCourt(court);
            } catch (ConstraintViolationException ex) {
                LOG.error("Validation failed while migrating court '{}': {}", dto.name(), ex.getMessage());
                throw ex;
            }
            UUID courtId = savedCourt.getId();
            persistCourtServiceAreas(dto.courtServiceAreas(), courtId, context);
            persistCourtAreasOfLaw(dto.courtAreasOfLaw(), courtId, context);
            persistCourtSinglePointsOfEntry(dto.courtSinglePointsOfEntry(), courtId, context);
            persistCourtLocalAuthorities(dto.courtLocalAuthorities(), courtId, context);
            persistCourtProfessionalInformation(dto.courtProfessionalInformation(), courtId, context);
            persistCourtCodes(dto.courtCodes(), courtId, context);
            persistCourtDxCodes(dto.courtDxCodes(), courtId, context);
            persistCourtFax(dto.courtFax(), courtId, context);
            persistLegacyCourtMapping(dto.id(), courtId);
            total++;
        }
        return total;
    }

    private UUID resolveRegionId(CourtDto dto, MigrationContext context) {
        UUID regionId = dto.regionId() == null ? null : context.regionIds.get(dto.regionId());
        if (regionId != null) {
            return regionId;
        }
        if (!Boolean.TRUE.equals(dto.isServiceCentre())) {
            return null;
        }
        UUID fallback = context.serviceCentreRegionId;
        if (fallback == null) {
            fallback = loadServiceCentreRegionId();
            context.serviceCentreRegionId = fallback;
            if (fallback == null) {
                LOG.warn(
                    "Unable to assign region for service centre '{}' because the fallback region was not found",
                    dto.slug()
                );
                return null;
            }
        }
        LOG.info(
            "Assigned '{}' region to service centre '{}'",
            SERVICE_CENTRE_REGION_NAME,
            dto.slug()
        );
        return fallback;
    }

    private UUID loadServiceCentreRegionId() {
        return regionRepository.findByNameAndCountry(SERVICE_CENTRE_REGION_NAME, SERVICE_CENTRE_REGION_COUNTRY)
            .map(Region::getId)
            .orElse(null);
    }

    /**
     * Persists the court-service-area join records for a given court.
     *
     * @param serviceAreas legacy service-area relationships for a court.
     * @param courtId      identifier of the court that was just migrated.
     * @param context      migration context containing service-area ID mappings.
     */
    private void persistCourtServiceAreas(
        List<CourtServiceAreaDto> serviceAreas,
        UUID courtId,
        MigrationContext context
    ) {
        if (isEmpty(serviceAreas)) {
            return;
        }

        for (CourtServiceAreaDto dto : serviceAreas) {
            CourtServiceAreas entity = CourtServiceAreas.builder()
                .courtId(courtId)
                .serviceAreaId(mapIds(dto.serviceAreaIds(), context.serviceAreaIds, "court service area"))
                .catchmentType(parseCatchmentType(dto.catchmentType()))
                .build();
            courtServiceAreasRepository.save(entity);
            context.courtServiceAreasMigrated++;
        }
    }

    /**
     * Persists court areas-of-law relationships for a given court.
     *
     * @param dto     legacy areas-of-law payload.
     * @param courtId identifier of the court that was just migrated.
     * @param context migration context containing area-of-law ID mappings.
     */
    private void persistCourtAreasOfLaw(
        CourtAreasOfLawDto dto,
        UUID courtId,
        MigrationContext context
    ) {
        if (dto == null || isEmpty(dto.areaOfLawIds())) {
            return;
        }

        CourtAreasOfLaw entity = CourtAreasOfLaw.builder()
            .courtId(courtId)
            .areasOfLaw(mapIds(dto.areaOfLawIds(), context.areaOfLawIds, "court areas of law"))
            .build();
        courtAreasOfLawRepository.save(entity);
        context.courtAreasOfLawMigrated++;
    }

    /**
     * Persists court single-point-of-entry relationships for a given court.
     *
     * @param dto     legacy SPOE payload.
     * @param courtId identifier of the court that was just migrated.
     * @param context migration context containing area-of-law ID mappings.
     */
    private void persistCourtSinglePointsOfEntry(
        CourtSinglePointOfEntryDto dto,
        UUID courtId,
        MigrationContext context
    ) {
        if (dto == null || isEmpty(dto.areaOfLawIds())) {
            return;
        }

        CourtSinglePointsOfEntry entity = CourtSinglePointsOfEntry.builder()
            .courtId(courtId)
            .areasOfLaw(mapIds(dto.areaOfLawIds(), context.areaOfLawIds, "court single point of entry"))
            .build();
        courtSinglePointsOfEntryRepository.save(entity);
        context.courtSinglePointsOfEntryMigrated++;
    }

    private void persistCourtLocalAuthorities(
        List<CourtLocalAuthorityDto> localAuthorities,
        UUID courtId,
        MigrationContext context
    ) {
        if (isEmpty(localAuthorities)) {
            return;
        }

        for (CourtLocalAuthorityDto dto : localAuthorities) {
            if (dto.localAuthorityIds() == null || dto.localAuthorityIds().isEmpty()) {
                continue;
            }

            UUID areaOfLawId = context.areaOfLawIds.get(dto.areaOfLawId());
            if (areaOfLawId == null && dto.areaOfLawId() != null) {
                LOG.warn(
                    "Skipping court local authority for court '{}' because area_of_law_id {} was not migrated",
                    courtId,
                    dto.areaOfLawId()
                );
                continue;
            }

            List<UUID> localAuthorityIds = mapIds(
                dto.localAuthorityIds(),
                context.localAuthorityTypeIds,
                "court local authorities"
            );
            if (localAuthorityIds == null) {
                LOG.warn(
                    "Skipping court local authority for court '{}' because local authority ids could not be mapped",
                    courtId
                );
                continue;
            }

            CourtLocalAuthorities entity = CourtLocalAuthorities.builder()
                .courtId(courtId)
                .areaOfLawId(areaOfLawId)
                .localAuthorityIds(localAuthorityIds)
                .build();
            courtLocalAuthoritiesRepository.save(entity);
            context.courtLocalAuthoritiesMigrated++;
        }
    }

    /**
     * Persists the professional information record for a court.
     *
     * @param dto     legacy professional information payload.
     * @param courtId identifier of the court that was just migrated.
     * @param context migration context used to track counters.
     */
    private void persistCourtProfessionalInformation(
        CourtProfessionalInformationDto dto,
        UUID courtId,
        MigrationContext context
    ) {
        if (dto == null) {
            return;
        }

        CourtProfessionalInformation entity = CourtProfessionalInformation.builder()
            .courtId(courtId)
            .interviewRooms(dto.interviewRooms())
            .interviewRoomCount(dto.interviewRoomCount())
            .interviewPhoneNumber(dto.interviewPhoneNumber())
            .videoHearings(dto.videoHearings())
            .commonPlatform(dto.commonPlatform())
            .accessScheme(dto.accessScheme())
            .build();
        courtProfessionalInformationRepository.save(entity);
        context.courtProfessionalInformationMigrated++;
    }

    private void persistLegacyCourtMapping(Long legacyCourtId, UUID courtId) {
        if (legacyCourtId == null) {
            return;
        }
        legacyCourtMappingRepository.save(
            LegacyCourtMapping.builder()
                .courtId(courtId)
                .legacyCourtId(legacyCourtId)
                .build()
        );
    }

    /**
     * Persists the court codes record for a court.
     *
     * @param dto     legacy code payload.
     * @param courtId identifier of the court that was just migrated.
     */
    private void persistCourtCodes(CourtCodesDto dto, UUID courtId, MigrationContext context) {
        if (dto == null) {
            return;
        }

        CourtCodes entity = CourtCodes.builder()
            .courtId(courtId)
            .magistrateCourtCode(dto.magistrateCourtCode())
            .familyCourtCode(dto.familyCourtCode())
            .tribunalCode(dto.tribunalCode())
            .countyCourtCode(dto.countyCourtCode())
            .crownCourtCode(dto.crownCourtCode())
            .gbs(dto.gbs())
            .build();
        courtCodesRepository.save(entity);
        context.courtCodesMigrated++;
    }

    /**
     * Persists DX codes for a court.
     *
     * @param dtos    legacy DX code payload.
     * @param courtId identifier of the court that was just migrated.
     */
    private void persistCourtDxCodes(List<CourtDxCodeDto> dtos, UUID courtId, MigrationContext context) {
        if (isEmpty(dtos)) {
            return;
        }

        for (CourtDxCodeDto dto : dtos) {
            if (StringUtils.isBlank(dto.dxCode()) && StringUtils.isBlank(dto.explanation())) {
                LOG.debug(
                    "Skipping DX code for court '{}' because both code and explanation are blank",
                    courtId
                );
                continue;
            }
            if (StringUtils.length(dto.dxCode()) > 200) {
                LOG.warn(
                    "Skipping DX code '{}' for court '{}' because it exceeds 200 characters",
                    dto.dxCode(),
                    courtId
                );
                continue;
            }
            if (StringUtils.isNotBlank(dto.dxCode())
                && !GENERIC_DESCRIPTION_PATTERN.matcher(dto.dxCode()).matches()) {
                LOG.warn(
                    "Skipping DX code '{}' for court '{}' due to invalid characters",
                    dto.dxCode(),
                    courtId
                );
                continue;
            }
            CourtDxCode entity = CourtDxCode.builder()
                .courtId(courtId)
                .dxCode(dto.dxCode())
                .explanation(dto.explanation())
                .build();
            courtDxCodeRepository.save(entity);
            context.courtDxCodesMigrated++;
        }
    }

    /**
     * Persists fax numbers for a court.
     *
     * @param dtos    legacy fax payload.
     * @param courtId identifier of the court that was just migrated.
     */
    private void persistCourtFax(List<CourtFaxDto> dtos, UUID courtId, MigrationContext context) {
        if (isEmpty(dtos)) {
            return;
        }

        for (CourtFaxDto dto : dtos) {
            if (StringUtils.isBlank(dto.faxNumber())) {
                continue;
            }
            CourtFax entity = CourtFax.builder()
                .courtId(courtId)
                .faxNumber(dto.faxNumber())
                .build();
            courtFaxRepository.save(entity);
            context.courtFaxMigrated++;
        }
    }

    /**
     * Normalises the legacy catchment method value into the new enum or returns {@code null}
     * when the value is blank or unknown.
     *
     * @param value legacy catchment method text.
     * @return parsed enum or {@code null} when the value cannot be parsed.
     */
    private CatchmentMethod parseCatchmentMethod(String value) {
        if (StringUtils.isBlank(value)) {
            return null;
        }
        try {
            return CatchmentMethod.valueOf(StringUtils.upperCase(value).replace('-', '_'));
        } catch (IllegalArgumentException ex) {
            LOG.warn("Unknown catchment method '{}'", value);
            return null;
        }
    }

    /**
     * Normalises the legacy service area type value into the new enum or returns {@code null}
     * when the value is blank or unknown.
     *
     * @param value legacy service area type text.
     * @return parsed enum or {@code null} when the value cannot be parsed.
     */
    private ServiceAreaType parseServiceAreaType(String value) {
        if (StringUtils.isBlank(value)) {
            return null;
        }
        try {
            return ServiceAreaType.valueOf(StringUtils.upperCase(value).replace('-', '_'));
        } catch (IllegalArgumentException ex) {
            LOG.warn("Unknown service area type '{}'", value);
            return null;
        }
    }

    /**
     * Normalises the legacy catchment type value into the new enum or returns {@code null}
     * when the value is blank or unknown.
     *
     * @param value legacy catchment type text.
     * @return parsed enum or {@code null} when the value cannot be parsed.
     */
    private CatchmentType parseCatchmentType(String value) {
        if (StringUtils.isBlank(value)) {
            return null;
        }
        try {
            return CatchmentType.valueOf(StringUtils.upperCase(value).replace('-', '_'));
        } catch (IllegalArgumentException ex) {
            LOG.warn("Unknown catchment type '{}'", value);
            return null;
        }
    }

    /**
     * Translates legacy integer identifiers into the UUIDs generated during earlier steps of the
     * migration. Missing IDs are logged and skipped rather than failing the entire import.
     *
     * @param sourceIds legacy identifiers.
     * @param lookup    lookup with mappings established previously in the migration.
     * @param context   human-readable description of the relationship being mapped.
     * @return list of UUIDs or {@code null} when there are no valid mappings.
     */
    private List<UUID> mapIds(List<Integer> sourceIds, Map<Integer, UUID> lookup, String context) {
        if (sourceIds == null || sourceIds.isEmpty()) {
            return null;
        }

        List<UUID> results = new ArrayList<>();
        for (Integer id : sourceIds) {
            UUID mapped = lookup.get(id);
            if (mapped == null) {
                LOG.warn("Unable to map {} identifier '{}' in migration payload", context, id);
                continue;
            }
            results.add(mapped);
        }

        return results.isEmpty() ? null : results;
    }

    /**
     * Parses a numeric string into an {@link Integer} while handling blanks or invalid values.
     *
     * @param value legacy numeric text.
     * @param context human-readable description of the field being parsed.
     * @return parsed integer or {@code null} when the value cannot be parsed.
     */
    private Integer parseInteger(String value, String context) {
        if (StringUtils.isBlank(value)) {
            return null;
        }
        try {
            return Integer.valueOf(value);
        } catch (NumberFormatException ex) {
            LOG.warn("Unable to parse integer for {} from '{}'", context, value);
            return null;
        }
    }

    private static class MigrationContext {
        private final Map<Integer, UUID> regionIds = new HashMap<>();
        private final Map<Integer, UUID> areaOfLawIds = new HashMap<>();
        private final Map<Integer, UUID> serviceAreaIds = new HashMap<>();
        private final Map<Integer, UUID> localAuthorityTypeIds = new HashMap<>();
        private UUID serviceCentreRegionId;
        private int courtAreasOfLawMigrated;
        private int courtServiceAreasMigrated;
        private int courtLocalAuthoritiesMigrated;
        private int courtSinglePointsOfEntryMigrated;
        private int courtProfessionalInformationMigrated;
        private int courtCodesMigrated;
        private int courtDxCodesMigrated;
        private int courtFaxMigrated;
    }

    /**
     * Convenience helper to treat {@code null} and empty collections the same way when guarding
     * optional payload sections from the legacy export.
     *
     * @param values collection to check.
     * @return {@code true} when the collection is {@code null} or contains no elements.
     */
    private static boolean isEmpty(Collection<?> values) {
        return values == null || values.isEmpty();
    }

    private String sanitiseCourtName(String name) {
        if (StringUtils.isBlank(name)) {
            return name;
        }
        String cleaned = name.replaceAll("[^A-Za-z&'(),\\- ]", " ");
        return cleaned.replaceAll("\\s+", " ").trim();
    }
}
