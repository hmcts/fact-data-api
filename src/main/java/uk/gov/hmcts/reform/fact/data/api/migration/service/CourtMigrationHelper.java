package uk.gov.hmcts.reform.fact.data.api.migration.service;

import jakarta.validation.ConstraintViolationException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Pattern;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.gov.hmcts.reform.fact.data.api.entities.Court;
import uk.gov.hmcts.reform.fact.data.api.entities.CourtAreasOfLaw;
import uk.gov.hmcts.reform.fact.data.api.entities.CourtCodes;
import uk.gov.hmcts.reform.fact.data.api.entities.CourtDxCode;
import uk.gov.hmcts.reform.fact.data.api.entities.CourtFax;
import uk.gov.hmcts.reform.fact.data.api.entities.CourtLocalAuthorities;
import uk.gov.hmcts.reform.fact.data.api.entities.CourtProfessionalInformation;
import uk.gov.hmcts.reform.fact.data.api.entities.CourtServiceAreas;
import uk.gov.hmcts.reform.fact.data.api.entities.CourtSinglePointsOfEntry;
import uk.gov.hmcts.reform.fact.data.api.entities.validation.ValidationConstants;
import uk.gov.hmcts.reform.fact.data.api.entities.types.CatchmentType;
import uk.gov.hmcts.reform.fact.data.api.migration.entities.LegacyCourtMapping;
import uk.gov.hmcts.reform.fact.data.api.migration.model.CourtAreasOfLawDto;
import uk.gov.hmcts.reform.fact.data.api.migration.model.CourtCodesDto;
import uk.gov.hmcts.reform.fact.data.api.migration.model.CourtDto;
import uk.gov.hmcts.reform.fact.data.api.migration.model.CourtDxCodeDto;
import uk.gov.hmcts.reform.fact.data.api.migration.model.CourtFaxDto;
import uk.gov.hmcts.reform.fact.data.api.migration.model.CourtLocalAuthorityDto;
import uk.gov.hmcts.reform.fact.data.api.migration.model.CourtProfessionalInformationDto;
import uk.gov.hmcts.reform.fact.data.api.migration.model.CourtServiceAreaDto;
import uk.gov.hmcts.reform.fact.data.api.migration.model.CourtSinglePointOfEntryDto;
import uk.gov.hmcts.reform.fact.data.api.repositories.CourtAreasOfLawRepository;
import uk.gov.hmcts.reform.fact.data.api.repositories.CourtCodesRepository;
import uk.gov.hmcts.reform.fact.data.api.repositories.CourtDxCodeRepository;
import uk.gov.hmcts.reform.fact.data.api.repositories.CourtFaxRepository;
import uk.gov.hmcts.reform.fact.data.api.repositories.CourtLocalAuthoritiesRepository;
import uk.gov.hmcts.reform.fact.data.api.repositories.CourtProfessionalInformationRepository;
import uk.gov.hmcts.reform.fact.data.api.repositories.CourtServiceAreasRepository;
import uk.gov.hmcts.reform.fact.data.api.repositories.CourtSinglePointsOfEntryRepository;
import uk.gov.hmcts.reform.fact.data.api.repositories.RegionRepository;
import uk.gov.hmcts.reform.fact.data.api.services.CourtService;
import uk.gov.hmcts.reform.fact.data.api.migration.repository.LegacyCourtMappingRepository;

/**
 * Responsible for migrating each court plus its related child entities (service areas, SPOEs,
 * codes, etc.). Keeps the primary migration service small by grouping the relationship logic in
 * one place.
 */
class CourtMigrationHelper {

    private static final Logger LOG = LoggerFactory.getLogger(CourtMigrationHelper.class);
    private static final Pattern COURT_NAME_PATTERN = Pattern.compile(ValidationConstants.COURT_NAME_REGEX);
    private static final Pattern GENERIC_DESCRIPTION_PATTERN =
        Pattern.compile(ValidationConstants.GENERIC_DESCRIPTION_REGEX);

    private final RegionRepository regionRepository;
    private final CourtServiceAreasRepository courtServiceAreasRepository;
    private final CourtAreasOfLawRepository courtAreasOfLawRepository;
    private final CourtSinglePointsOfEntryRepository courtSinglePointsOfEntryRepository;
    private final CourtLocalAuthoritiesRepository courtLocalAuthoritiesRepository;
    private final CourtProfessionalInformationRepository courtProfessionalInformationRepository;
    private final CourtCodesRepository courtCodesRepository;
    private final CourtDxCodeRepository courtDxCodeRepository;
    private final CourtFaxRepository courtFaxRepository;
    private final LegacyCourtMappingRepository legacyCourtMappingRepository;
    private final CourtService courtService;

    CourtMigrationHelper(
        RegionRepository regionRepository,
        CourtServiceAreasRepository courtServiceAreasRepository,
        CourtAreasOfLawRepository courtAreasOfLawRepository,
        CourtSinglePointsOfEntryRepository courtSinglePointsOfEntryRepository,
        CourtLocalAuthoritiesRepository courtLocalAuthoritiesRepository,
        CourtProfessionalInformationRepository courtProfessionalInformationRepository,
        CourtCodesRepository courtCodesRepository,
        CourtDxCodeRepository courtDxCodeRepository,
        CourtFaxRepository courtFaxRepository,
        LegacyCourtMappingRepository legacyCourtMappingRepository,
        CourtService courtService
    ) {
        this.regionRepository = regionRepository;
        this.courtServiceAreasRepository = courtServiceAreasRepository;
        this.courtAreasOfLawRepository = courtAreasOfLawRepository;
        this.courtSinglePointsOfEntryRepository = courtSinglePointsOfEntryRepository;
        this.courtLocalAuthoritiesRepository = courtLocalAuthoritiesRepository;
        this.courtProfessionalInformationRepository = courtProfessionalInformationRepository;
        this.courtCodesRepository = courtCodesRepository;
        this.courtDxCodeRepository = courtDxCodeRepository;
        this.courtFaxRepository = courtFaxRepository;
        this.legacyCourtMappingRepository = legacyCourtMappingRepository;
        this.courtService = courtService;
    }

    /**
     * Persists the supplied courts and their relationships. Skips any courts that cannot be mapped
     * (e.g. missing region) and records migration counts in the context so the caller can include
     * them in the final summary.
     *
     * @param courts legacy court definitions.
     * @param context migration context containing previously mapped reference IDs.
     * @return number of courts migrated successfully.
     */
    int migrateCourts(List<CourtDto> courts, MigrationContext context) {
        if (isEmpty(courts)) {
            return 0;
        }

        int total = 0;
        for (CourtDto dto : courts) {
            UUID regionId = resolveRegionId(dto, context);
            if (regionId == null) {
                LOG.warn("Skipping court {} because region {} was not migrated", dto.getSlug(), dto.getRegionId());
                continue;
            }

            String courtName = sanitiseCourtName(dto.getName());
            if (StringUtils.isBlank(courtName)) {
                LOG.warn("Skipping court {} because sanitised name was blank", dto.getSlug());
                continue;
            }
            if (!COURT_NAME_PATTERN.matcher(courtName).matches()) {
                LOG.warn(
                    "Skipping court {} because sanitised name '{}' still fails validation regex",
                    dto.getSlug(),
                    courtName
                );
                continue;
            }

            Court court = Court.builder()
                .name(courtName)
                .slug(dto.getSlug())
                .open(dto.getOpen())
                .regionId(regionId)
                .isServiceCentre(dto.getIsServiceCentre())
                .build();

            Court savedCourt;
            try {
                savedCourt = courtService.createCourt(court);
            } catch (ConstraintViolationException ex) {
                LOG.error("Validation failed while migrating court '{}': {}", dto.getName(), ex.getMessage());
                throw ex;
            }
            UUID courtId = savedCourt.getId();
            persistCourtServiceAreas(dto.getCourtServiceAreas(), courtId, context);
            persistCourtAreasOfLaw(dto.getCourtAreasOfLaw(), courtId, context);
            persistCourtSinglePointsOfEntry(dto.getCourtSinglePointsOfEntry(), courtId, context);
            persistCourtLocalAuthorities(dto.getCourtLocalAuthorities(), courtId, context);
            persistCourtProfessionalInformation(dto.getCourtProfessionalInformation(), courtId, context);
            persistCourtCodes(dto.getCourtCodes(), courtId, context);
            persistCourtDxCodes(dto.getCourtDxCodes(), courtId, context);
            persistCourtFax(dto.getCourtFax(), courtId, context);
            persistLegacyCourtMapping(dto.getId(), courtId);
            total++;
        }
        return total;
    }

    /**
     * Persists court-service-area joins for a single court.
     *
     * @param serviceAreas legacy service-area relationships.
     * @param courtId identifier of the newly created court.
     * @param context migration context containing service-area ID mappings.
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
                .serviceAreaId(mapIds(dto.getServiceAreaIds(), context.getServiceAreaIds(), "court service area"))
                .catchmentType(parseCatchmentType(dto.getCatchmentType()))
                .build();
            courtServiceAreasRepository.save(entity);
            context.courtServiceAreasMigrated++;
        }
    }

    /**
     * Persists the areas-of-law association for the supplied court.
     *
     * @param dto legacy areas-of-law payload.
     * @param courtId identifier of the court being migrated.
     * @param context migration context with mapped area-of-law IDs.
     */
    private void persistCourtAreasOfLaw(
        CourtAreasOfLawDto dto,
        UUID courtId,
        MigrationContext context
    ) {
        if (dto == null || isEmpty(dto.getAreaOfLawIds())) {
            return;
        }

        CourtAreasOfLaw entity = CourtAreasOfLaw.builder()
            .courtId(courtId)
            .areasOfLaw(mapIds(dto.getAreaOfLawIds(), context.getAreaOfLawIds(), "court areas of law"))
            .build();
        courtAreasOfLawRepository.save(entity);
        context.courtAreasOfLawMigrated++;
    }

    /**
     * Persists single-point-of-entry associations for the supplied court.
     *
     * @param dto legacy SPOE payload.
     * @param courtId identifier of the court being migrated.
     * @param context migration context with mapped area-of-law IDs.
     */
    private void persistCourtSinglePointsOfEntry(
        CourtSinglePointOfEntryDto dto,
        UUID courtId,
        MigrationContext context
    ) {
        if (dto == null || isEmpty(dto.getAreaOfLawIds())) {
            return;
        }

        CourtSinglePointsOfEntry entity = CourtSinglePointsOfEntry.builder()
            .courtId(courtId)
            .areasOfLaw(mapIds(dto.getAreaOfLawIds(), context.getAreaOfLawIds(), "court single point of entry"))
            .build();
        courtSinglePointsOfEntryRepository.save(entity);
        context.courtSinglePointsOfEntryMigrated++;
    }

    /**
     * Persists local-authority relationships for the supplied court. Skips entries whose area of
     * law or local authority ID cannot be mapped.
     *
     * @param localAuthorities legacy local authority payload.
     * @param courtId identifier of the court being migrated.
     * @param context migration context containing mapped IDs.
     */
    private void persistCourtLocalAuthorities(
        List<CourtLocalAuthorityDto> localAuthorities,
        UUID courtId,
        MigrationContext context
    ) {
        if (isEmpty(localAuthorities)) {
            return;
        }

        for (CourtLocalAuthorityDto dto : localAuthorities) {
            if (dto.getLocalAuthorityIds() == null || dto.getLocalAuthorityIds().isEmpty()) {
                continue;
            }

            UUID areaOfLawId = context.getAreaOfLawIds().get(dto.getAreaOfLawId());
            if (areaOfLawId == null && dto.getAreaOfLawId() != null) {
                LOG.warn(
                    "Skipping court local authority for court '{}' because area_of_law_id {} was not migrated",
                    courtId,
                    dto.getAreaOfLawId()
                );
                continue;
            }

            List<UUID> localAuthorityIds = mapIds(
                dto.getLocalAuthorityIds(),
                context.getLocalAuthorityTypeIds(),
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
     * Persists professional information for the supplied court.
     *
     * @param dto legacy professional information payload.
     * @param courtId identifier of the court being migrated.
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
            .interviewRooms(dto.getInterviewRooms())
            .interviewRoomCount(dto.getInterviewRoomCount())
            .interviewPhoneNumber(dto.getInterviewPhoneNumber())
            .videoHearings(dto.getVideoHearings())
            .commonPlatform(dto.getCommonPlatform())
            .accessScheme(dto.getAccessScheme())
            .build();
        courtProfessionalInformationRepository.save(entity);
        context.courtProfessionalInformationMigrated++;
    }

    /**
     * Persists court-code metadata such as GBS, magistrate, etc.
     *
     * @param dto legacy court-code payload.
     * @param courtId identifier of the court being migrated.
     * @param context migration context used to track counts.
     */
    private void persistCourtCodes(CourtCodesDto dto, UUID courtId, MigrationContext context) {
        if (dto == null) {
            return;
        }

        CourtCodes entity = CourtCodes.builder()
            .courtId(courtId)
            .magistrateCourtCode(dto.getMagistrateCourtCode())
            .familyCourtCode(dto.getFamilyCourtCode())
            .tribunalCode(dto.getTribunalCode())
            .countyCourtCode(dto.getCountyCourtCode())
            .crownCourtCode(dto.getCrownCourtCode())
            .gbs(dto.getGbs())
            .build();
        courtCodesRepository.save(entity);
        context.courtCodesMigrated++;
    }

    /**
     * Persists DX codes for the supplied court, skipping invalid entries.
     *
     * @param dtos legacy DX payload.
     * @param courtId identifier of the court being migrated.
     * @param context migration context used to track counts.
     */
    private void persistCourtDxCodes(List<CourtDxCodeDto> dtos, UUID courtId, MigrationContext context) {
        if (isEmpty(dtos)) {
            return;
        }

        for (CourtDxCodeDto dto : dtos) {
            if (StringUtils.isBlank(dto.getDxCode()) && StringUtils.isBlank(dto.getExplanation())) {
                LOG.debug(
                    "Skipping DX code for court '{}' because both code and explanation are blank",
                    courtId
                );
                continue;
            }
            if (StringUtils.length(dto.getDxCode()) > 200) {
                LOG.warn(
                    "Skipping DX code '{}' for court '{}' because it exceeds 200 characters",
                    dto.getDxCode(),
                    courtId
                );
                continue;
            }
            if (StringUtils.isNotBlank(dto.getDxCode())
                && !GENERIC_DESCRIPTION_PATTERN.matcher(dto.getDxCode()).matches()) {
                LOG.warn(
                    "Skipping DX code '{}' for court '{}' due to invalid characters",
                    dto.getDxCode(),
                    courtId
                );
                continue;
            }
            CourtDxCode entity = CourtDxCode.builder()
                .courtId(courtId)
                .dxCode(dto.getDxCode())
                .explanation(dto.getExplanation())
                .build();
            courtDxCodeRepository.save(entity);
            context.courtDxCodesMigrated++;
        }
    }

    /**
     * Persists fax numbers for the supplied court, ignoring blank entries.
     *
     * @param dtos legacy fax payload.
     * @param courtId identifier of the court being migrated.
     * @param context migration context used to track counts.
     */
    private void persistCourtFax(List<CourtFaxDto> dtos, UUID courtId, MigrationContext context) {
        if (isEmpty(dtos)) {
            return;
        }

        for (CourtFaxDto dto : dtos) {
            if (StringUtils.isBlank(dto.getFaxNumber())) {
                continue;
            }
            CourtFax entity = CourtFax.builder()
                .courtId(courtId)
                .faxNumber(dto.getFaxNumber())
                .build();
            courtFaxRepository.save(entity);
            context.courtFaxMigrated++;
        }
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
     * Resolves the region ID for the supplied court. Service centres fall back to the dedicated
     * "Service Centre" region when the legacy payload omits a region.
     *
     * @param dto legacy court payload.
     * @param context migration context containing mapped region IDs.
     * @return mapped region ID or {@code null} when the court should be skipped.
     */
    private UUID resolveRegionId(CourtDto dto, MigrationContext context) {
        UUID regionId = dto.getRegionId() == null ? null : context.getRegionIds().get(dto.getRegionId());
        if (regionId != null) {
            return regionId;
        }
        if (!Boolean.TRUE.equals(dto.getIsServiceCentre())) {
            return null;
        }
        UUID fallback = context.getServiceCentreRegionId();
        if (fallback == null) {
            fallback = loadServiceCentreRegionId();
            context.setServiceCentreRegionId(fallback);
            if (fallback == null) {
                LOG.warn(
                    "Unable to assign region for service centre '{}' because the fallback region was not found",
                    dto.getSlug()
                );
                return null;
            }
        }
        return fallback;
    }

    /**
     * Loads the fallback region for service centres. Returns {@code null} if the region is not
     * present (the caller logs the warning).
     *
     * @return identifier of the service-centre region or {@code null}.
     */
    private UUID loadServiceCentreRegionId() {
        return regionRepository.findByNameAndCountry("Service Centre", "England")
            .map(uk.gov.hmcts.reform.fact.data.api.entities.Region::getId)
            .orElse(null);
    }

    /**
     * Normalises the catchment type value coming from the legacy service area payload.
     *
     * @param value legacy catchment text.
     * @return parsed {@link CatchmentType} or {@code null} if the value is blank/unknown.
     */
    private static CatchmentType parseCatchmentType(String value) {
        if (StringUtils.isBlank(value)) {
            return null;
        }
        try {
            return CatchmentType.valueOf(StringUtils.upperCase(value).replace('-', '_'));
        } catch (IllegalArgumentException ex) {
            LOG.warn("Unknown catchment method '{}'", value);
            return null;
        }
    }

    private static List<UUID> mapIds(List<Integer> sourceIds, Map<Integer, UUID> lookup, String context) {
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
