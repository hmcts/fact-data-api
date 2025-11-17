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

    int migrateCourts(List<CourtDto> courts, MigrationContext context) {
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
                .serviceAreaId(mapIds(dto.serviceAreaIds(), context.getServiceAreaIds(), "court service area"))
                .catchmentType(parseCatchmentType(dto.catchmentType()))
                .build();
            courtServiceAreasRepository.save(entity);
            context.courtServiceAreasMigrated++;
        }
    }

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
            .areasOfLaw(mapIds(dto.areaOfLawIds(), context.getAreaOfLawIds(), "court areas of law"))
            .build();
        courtAreasOfLawRepository.save(entity);
        context.courtAreasOfLawMigrated++;
    }

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
            .areasOfLaw(mapIds(dto.areaOfLawIds(), context.getAreaOfLawIds(), "court single point of entry"))
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

            UUID areaOfLawId = context.getAreaOfLawIds().get(dto.areaOfLawId());
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

    private UUID resolveRegionId(CourtDto dto, MigrationContext context) {
        UUID regionId = dto.regionId() == null ? null : context.getRegionIds().get(dto.regionId());
        if (regionId != null) {
            return regionId;
        }
        if (!Boolean.TRUE.equals(dto.isServiceCentre())) {
            return null;
        }
        UUID fallback = context.getServiceCentreRegionId();
        if (fallback == null) {
            fallback = loadServiceCentreRegionId();
            context.setServiceCentreRegionId(fallback);
            if (fallback == null) {
                LOG.warn(
                    "Unable to assign region for service centre '{}' because the fallback region was not found",
                    dto.slug()
                );
                return null;
            }
        }
        return fallback;
    }

    private UUID loadServiceCentreRegionId() {
        return regionRepository.findByNameAndCountry("Service Centre", "England")
            .map(uk.gov.hmcts.reform.fact.data.api.entities.Region::getId)
            .orElse(null);
    }

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
