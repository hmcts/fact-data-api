package uk.gov.hmcts.reform.fact.data.api.migration.service;

import jakarta.validation.ConstraintViolationException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.regex.Pattern;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import uk.gov.hmcts.reform.fact.data.api.entities.AreaOfLawType;
import uk.gov.hmcts.reform.fact.data.api.entities.Court;
import uk.gov.hmcts.reform.fact.data.api.entities.CourtAreasOfLaw;
import uk.gov.hmcts.reform.fact.data.api.entities.CourtCodes;
import uk.gov.hmcts.reform.fact.data.api.entities.CourtDxCode;
import uk.gov.hmcts.reform.fact.data.api.entities.CourtFax;
import uk.gov.hmcts.reform.fact.data.api.entities.CourtLocalAuthorities;
import uk.gov.hmcts.reform.fact.data.api.entities.CourtProfessionalInformation;
import uk.gov.hmcts.reform.fact.data.api.entities.CourtSinglePointsOfEntry;
import uk.gov.hmcts.reform.fact.data.api.entities.types.AllowedLocalAuthorityAreasOfLaw;
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
import uk.gov.hmcts.reform.fact.data.api.migration.model.CourtSinglePointOfEntryDto;
import uk.gov.hmcts.reform.fact.data.api.migration.model.MigrationFindingSeverity;
import uk.gov.hmcts.reform.fact.data.api.migration.model.MigrationFindingType;
import uk.gov.hmcts.reform.fact.data.api.migration.model.MigrationSection;
import uk.gov.hmcts.reform.fact.data.api.repositories.AreaOfLawTypeRepository;
import uk.gov.hmcts.reform.fact.data.api.repositories.CourtAreasOfLawRepository;
import uk.gov.hmcts.reform.fact.data.api.repositories.CourtCodesRepository;
import uk.gov.hmcts.reform.fact.data.api.repositories.CourtDxCodeRepository;
import uk.gov.hmcts.reform.fact.data.api.repositories.CourtFaxRepository;
import uk.gov.hmcts.reform.fact.data.api.repositories.CourtLocalAuthoritiesRepository;
import uk.gov.hmcts.reform.fact.data.api.repositories.CourtProfessionalInformationRepository;
import uk.gov.hmcts.reform.fact.data.api.repositories.CourtRepository;
import uk.gov.hmcts.reform.fact.data.api.repositories.CourtSinglePointsOfEntryRepository;
import uk.gov.hmcts.reform.fact.data.api.services.CourtService;
import uk.gov.hmcts.reform.fact.data.api.migration.repository.LegacyCourtMappingRepository;

class CourtMigrationHelper {

    private static final Logger LOG = LoggerFactory.getLogger(CourtMigrationHelper.class);
    private static final Pattern COURT_NAME_PATTERN = Pattern.compile("^[A-Za-z&'()\\- ]+$");
    private static final Pattern GENERIC_DESCRIPTION_PATTERN =
        Pattern.compile(ValidationConstants.GENERIC_DESCRIPTION_REGEX);
    private static final Pattern INVALID_GENERIC_DESCRIPTION_CHARACTERS =
        Pattern.compile("[^A-Za-z0-9 ()':,-]+");
    private static final Map<Long, LegacySlugOverride> LEGACY_SLUG_OVERRIDES = Map.of(
        1479759L,
        new LegacySlugOverride("Chelmsford Justice Centre", "chelmsford-county-and-family-court"),
        1479760L,
        new LegacySlugOverride(
            "Newcastle upon Tyne Crown Court and Magistrates' Court",
            "newcastle-upon-tyne-combined-court-centre"
        ),
        1479894L,
        new LegacySlugOverride(
            "Bournemouth Combined Court",
            "bournemouth-and-poole-county-court-and-family-court"
        ),
        1479947L,
        new LegacySlugOverride("Truro Combined Court", "truro-county-court-and-family-court"),
        1479981L,
        new LegacySlugOverride("Bodmin Law Courts", "bodmin-county-court-and-family-court")
    );

    private final CourtAreasOfLawRepository courtAreasOfLawRepository;
    private final CourtSinglePointsOfEntryRepository courtSinglePointsOfEntryRepository;
    private final CourtLocalAuthoritiesRepository courtLocalAuthoritiesRepository;
    private final CourtProfessionalInformationRepository courtProfessionalInformationRepository;
    private final CourtCodesRepository courtCodesRepository;
    private final CourtDxCodeRepository courtDxCodeRepository;
    private final CourtFaxRepository courtFaxRepository;
    private final AreaOfLawTypeRepository areaOfLawTypeRepository;
    private final LegacyCourtMappingRepository legacyCourtMappingRepository;
    private final CourtService courtService;
    private final CourtRepository courtRepository;

    CourtMigrationHelper(
        CourtAreasOfLawRepository courtAreasOfLawRepository,
        CourtSinglePointsOfEntryRepository courtSinglePointsOfEntryRepository,
        CourtLocalAuthoritiesRepository courtLocalAuthoritiesRepository,
        CourtProfessionalInformationRepository courtProfessionalInformationRepository,
        CourtCodesRepository courtCodesRepository,
        CourtDxCodeRepository courtDxCodeRepository,
        CourtFaxRepository courtFaxRepository,
        AreaOfLawTypeRepository areaOfLawTypeRepository,
        LegacyCourtMappingRepository legacyCourtMappingRepository,
        CourtService courtService,
        CourtRepository courtRepository
    ) {
        this.courtAreasOfLawRepository = courtAreasOfLawRepository;
        this.courtSinglePointsOfEntryRepository = courtSinglePointsOfEntryRepository;
        this.courtLocalAuthoritiesRepository = courtLocalAuthoritiesRepository;
        this.courtProfessionalInformationRepository = courtProfessionalInformationRepository;
        this.courtCodesRepository = courtCodesRepository;
        this.courtDxCodeRepository = courtDxCodeRepository;
        this.courtFaxRepository = courtFaxRepository;
        this.areaOfLawTypeRepository = areaOfLawTypeRepository;
        this.legacyCourtMappingRepository = legacyCourtMappingRepository;
        this.courtService = courtService;
        this.courtRepository = courtRepository;
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
            if (Boolean.TRUE.equals(dto.getIsServiceCentre())) {
                LOG.info("Skipping service centre {}", dto.getSlug());
                continue;
            }

            Optional<UUID> regionId = resolveRegionId(dto, context);
            if (regionId.isEmpty()) {
                LOG.warn("Skipping court {} because region {} was not migrated", dto.getSlug(), dto.getRegionId());
                recordCourtError(
                    context,
                    dto,
                    "ORDINARY_COURT_REGION_MISSING_OR_UNMAPPED",
                    List.of(dto.getRegionId()).stream().filter(Objects::nonNull).toList()
                );
                continue;
            }

            String courtName = sanitiseCourtName(dto.getName());
            if (StringUtils.isBlank(courtName)) {
                LOG.warn("Skipping court {} because sanitised name was blank", dto.getSlug());
                recordCourtError(context, dto, "COURT_NAME_BLANK_AFTER_SANITISATION", List.of());
                continue;
            }
            if (!COURT_NAME_PATTERN.matcher(courtName).matches()) {
                LOG.warn(
                    "Skipping court {} because sanitised name '{}' still fails validation regex",
                    dto.getSlug(),
                    courtName
                );
                recordCourtError(context, dto, "COURT_NAME_INVALID_AFTER_SANITISATION", List.of());
                continue;
            }
            recordOrdinaryCourtServiceAreaDiscard(dto, context);
            if (!Objects.equals(dto.getName(), courtName)) {
                recordTransformation(
                    context,
                    MigrationSection.COURTS,
                    "COURT_NAME_SANITISED",
                    dto,
                    List.of("name")
                );
            }
            LegacySlugOverride slugOverride = resolveLegacySlugOverride(dto, courtName);

            String warningNotice = WarningNoticeSanitiser.sanitise(
                dto.getWarningNotice(),
                dto.getSlug(),
                "English"
            );
            String warningNoticeCy = WarningNoticeSanitiser.sanitise(
                dto.getWarningNoticeCy(),
                dto.getSlug(),
                "Welsh"
            );
            Court court = Court.builder()
                .name(courtName)
                .slug(dto.getSlug())
                .open(dto.getOpen())
                .warningNotice(warningNotice)
                .warningNoticeCy(warningNoticeCy)
                .regionId(regionId.get())
                .build();

            Court savedCourt;
            try {
                savedCourt = courtService.createCourt(court);
                savedCourt = applyLegacySlugOverride(dto.getId(), slugOverride, savedCourt, context);
            } catch (ConstraintViolationException ex) {
                LOG.error("Validation failed while migrating court '{}': {}", dto.getName(), ex.getMessage());
                throw ex;
            }
            if (StringUtils.isNotBlank(savedCourt.getWarningNotice())
                || StringUtils.isNotBlank(savedCourt.getWarningNoticeCy())) {
                context.warningNoticesMigrated++;
                context.persisted(MigrationSection.WARNING_NOTICES, 1, 0);
            }
            if (!Objects.equals(dto.getWarningNotice(), warningNotice)
                || !Objects.equals(dto.getWarningNoticeCy(), warningNoticeCy)) {
                recordTransformation(
                    context,
                    MigrationSection.WARNING_NOTICES,
                    "WARNING_NOTICE_SANITISED",
                    dto,
                    List.of("warningNotice", "warningNoticeCy")
                );
            }
            final UUID courtId = savedCourt.getId();
            context.persisted(MigrationSection.COURTS, 1, 0);
            context.persisted(MigrationSection.COURT_SLUGS, 1, 0);
            if (!Objects.equals(dto.getSlug(), savedCourt.getSlug())) {
                recordTransformation(
                    context,
                    MigrationSection.COURT_SLUGS,
                    "GENERATED_COURT_SLUG_DIFFERS",
                    dto,
                    List.of("slug")
                );
            }
            persistCourtAreasOfLaw(dto, courtId, context);
            persistCourtSinglePointsOfEntry(dto, courtId, context);
            persistCourtLocalAuthorities(dto, courtId, context);
            persistCourtProfessionalInformation(dto.getCourtProfessionalInformation(), courtId, context);
            persistCourtCodes(dto.getCourtCodes(), courtId, context);
            persistCourtDxCodes(dto, courtId, context);
            persistCourtFax(dto.getCourtFax(), courtId, context);
            persistLegacyCourtMapping(dto.getId(), courtId, context);
            total++;
        }
        return total;
    }

    private LegacySlugOverride resolveLegacySlugOverride(CourtDto dto, String courtName) {
        LegacySlugOverride override = LEGACY_SLUG_OVERRIDES.get(dto.getId());
        if (override == null) {
            return null;
        }

        if (!override.expectedName().equals(courtName)
            || !override.expectedSlug().equals(dto.getSlug())) {
            throw new IllegalStateException(
                "Reviewed slug override identity mismatch for legacy court " + dto.getId()
                    + ": expected name '" + override.expectedName()
                    + "' and slug '" + override.expectedSlug()
                    + "', received name '" + courtName
                    + "' and slug '" + dto.getSlug() + "'"
            );
        }
        return override;
    }

    private Court applyLegacySlugOverride(
        Long legacyCourtId,
        LegacySlugOverride override,
        Court savedCourt,
        MigrationContext context
    ) {
        if (override == null) {
            return savedCourt;
        }
        if (courtRepository.existsBySlug(override.expectedSlug())) {
            throw new IllegalStateException(
                "Cannot preserve reviewed slug '" + override.expectedSlug()
                    + "' for legacy court " + legacyCourtId + " because it already exists"
            );
        }

        savedCourt.setSlug(override.expectedSlug());
        final Court updatedCourt = courtRepository.save(savedCourt);
        context.courtSlugsPreserved++;
        recordTransformation(
            context,
            MigrationSection.COURT_SLUGS,
            "REVIEWED_LEGACY_SLUG_PRESERVED",
            legacyCourtId,
            override.expectedSlug(),
            List.of("slug")
        );
        LOG.info(
            "Preserved reviewed legacy slug {} for court {}",
            override.expectedSlug(),
            legacyCourtId
        );
        return updatedCourt;
    }

    /**
     * Persists the areas-of-law association for the supplied court.
     *
     * @param court legacy court payload.
     * @param courtId identifier of the court being migrated.
     * @param context migration context with mapped area-of-law IDs.
     */
    private void persistCourtAreasOfLaw(
        CourtDto court,
        UUID courtId,
        MigrationContext context
    ) {
        CourtAreasOfLawDto dto = court.getCourtAreasOfLaw();
        if (dto == null || isEmpty(dto.getAreaOfLawIds())) {
            return;
        }

        List<Integer> unmappedIds = unmappedIds(dto.getAreaOfLawIds(), context.getAreaOfLawIds());
        if (!unmappedIds.isEmpty()) {
            recordUnmappedChild(
                context,
                MigrationSection.COURT_AREAS_OF_LAW,
                "COURT_AREA_OF_LAW_UNMAPPED",
                court,
                unmappedIds
            );
            return;
        }
        CourtAreasOfLaw entity = CourtAreasOfLaw.builder()
            .courtId(courtId)
            .areasOfLaw(mapIds(dto.getAreaOfLawIds(), context.getAreaOfLawIds()))
            .build();
        courtAreasOfLawRepository.save(entity);
        context.courtAreasOfLawMigrated++;
        context.persisted(MigrationSection.COURT_AREAS_OF_LAW, 1, dto.getAreaOfLawIds().size());
    }

    /**
     * Persists single-point-of-entry associations for the supplied court.
     *
     * @param court legacy court payload.
     * @param courtId identifier of the court being migrated.
     * @param context migration context with mapped area-of-law IDs.
     */
    private void persistCourtSinglePointsOfEntry(
        CourtDto court,
        UUID courtId,
        MigrationContext context
    ) {
        CourtSinglePointOfEntryDto dto = court.getCourtSinglePointsOfEntry();
        if (dto == null || isEmpty(dto.getAreaOfLawIds())) {
            return;
        }

        List<Integer> unmappedIds = unmappedIds(dto.getAreaOfLawIds(), context.getAreaOfLawIds());
        if (!unmappedIds.isEmpty()) {
            recordUnmappedChild(
                context,
                MigrationSection.COURT_SINGLE_POINTS_OF_ENTRY,
                "COURT_SINGLE_POINT_OF_ENTRY_UNMAPPED",
                court,
                unmappedIds
            );
            return;
        }
        CourtSinglePointsOfEntry entity = CourtSinglePointsOfEntry.builder()
            .courtId(courtId)
            .areasOfLaw(mapIds(dto.getAreaOfLawIds(), context.getAreaOfLawIds()))
            .build();
        courtSinglePointsOfEntryRepository.save(entity);
        context.courtSinglePointsOfEntryMigrated++;
        context.persisted(MigrationSection.COURT_SINGLE_POINTS_OF_ENTRY, 1, dto.getAreaOfLawIds().size());
    }

    /**
     * Persists local-authority relationships for the supplied court. Skips entries whose area of
     * law or local authority ID cannot be mapped.
     *
     * @param court legacy court payload.
     * @param courtId identifier of the court being migrated.
     * @param context migration context containing mapped IDs.
     */
    private void persistCourtLocalAuthorities(
        CourtDto court,
        UUID courtId,
        MigrationContext context
    ) {
        List<CourtLocalAuthorityDto> localAuthorities = court.getCourtLocalAuthorities();
        if (isEmpty(localAuthorities)) {
            return;
        }

        List<UUID> allowedAreaOfLawIds =
            areaOfLawTypeRepository.findByNameIn(AllowedLocalAuthorityAreasOfLaw.displayNames()).stream()
                .map(AreaOfLawType::getId)
                .toList();

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
                recordUnmappedChild(
                    context,
                    MigrationSection.COURT_LOCAL_AUTHORITIES,
                    "COURT_LOCAL_AUTHORITY_AREA_OF_LAW_UNMAPPED",
                    court,
                    List.of(dto.getAreaOfLawId())
                );
                continue;
            }

            if (!allowedAreaOfLawIds.contains(areaOfLawId)) {
                LOG.warn(
                    "Skipping court local authority for court '{}' because area_of_law_id {} references an area "
                        + "of law that is not supported for local authority settings",
                    courtId,
                    dto.getAreaOfLawId()
                );
                context.discarded(
                    MigrationSection.COURT_LOCAL_AUTHORITIES,
                    1,
                    dto.getLocalAuthorityIds().size()
                );
                context.finding(
                    MigrationFindingSeverity.REVIEW,
                    MigrationFindingType.APPROVED_DISCARD,
                    MigrationSection.COURT_LOCAL_AUTHORITIES,
                    "FACT_2612_UNSUPPORTED_LOCAL_AUTHORITY_AREA",
                    court.getId(),
                    court.getSlug(),
                    dto.getId(),
                    dto.getLocalAuthorityIds(),
                    List.of("areaOfLawId", "localAuthorityIds"),
                    1,
                    dto.getLocalAuthorityIds().size()
                );
                continue;
            }

            List<Integer> unmappedAuthorityIds = unmappedLocalAuthorityIds(
                dto.getLocalAuthorityIds(),
                context.getLocalAuthorityTypeIds()
            );
            if (!unmappedAuthorityIds.isEmpty()) {
                recordUnmappedChild(
                    context,
                    MigrationSection.COURT_LOCAL_AUTHORITIES,
                    "COURT_LOCAL_AUTHORITY_REFERENCE_UNMAPPED",
                    court,
                    unmappedAuthorityIds
                );
                continue;
            }
            List<UUID> localAuthorityIds = mapLocalAuthorityIds(
                dto.getLocalAuthorityIds(),
                context.getLocalAuthorityTypeIds()
            );
            if (localAuthorityIds.isEmpty()) {
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
            context.persisted(
                MigrationSection.COURT_LOCAL_AUTHORITIES,
                1,
                localAuthorityIds.size() + 1
            );
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
        context.persisted(MigrationSection.COURT_PROFESSIONAL_INFORMATION, 1, 0);
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
        context.persisted(MigrationSection.COURT_CODES, 1, 0);
    }

    /**
     * Persists DX codes for the supplied court, skipping invalid entries.
     *
     * @param court legacy court payload.
     * @param courtId identifier of the court being migrated.
     * @param context migration context used to track counts.
     */
    private void persistCourtDxCodes(CourtDto court, UUID courtId, MigrationContext context) {
        List<CourtDxCodeDto> dtos = court.getCourtDxCodes();
        if (isEmpty(dtos)) {
            return;
        }

        for (CourtDxCodeDto dto : dtos) {
            Optional<String> dxCode = sanitiseGenericDescription(dto.getDxCode());
            Optional<String> explanation = sanitiseGenericDescription(dto.getExplanation());

            if (dxCode.isEmpty() && explanation.isEmpty()) {
                LOG.debug(
                    "Skipping DX code for court '{}' because both code and explanation are blank",
                    courtId
                );
                context.discarded(MigrationSection.COURT_DX_CODES, 1, 0);
                continue;
            }
            if (dxCode.isEmpty()) {
                LOG.warn(
                    "Skipping DX code for court '{}' because dx_code is blank after sanitisation",
                    courtId
                );
                rejectDx(context, court, "DX_CODE_BLANK_AFTER_SANITISATION");
                continue;
            }
            if (StringUtils.length(dxCode.get()) > 200) {
                LOG.warn(
                    "Skipping DX code '{}' for court '{}' because it exceeds 200 characters",
                    dxCode.get(),
                    courtId
                );
                rejectDx(context, court, "DX_CODE_TOO_LONG");
                continue;
            }
            if (!GENERIC_DESCRIPTION_PATTERN.matcher(dxCode.get()).matches()) {
                LOG.warn(
                    "Skipping DX code '{}' for court '{}' due to invalid characters",
                    dxCode.get(),
                    courtId
                );
                rejectDx(context, court, "DX_CODE_INVALID");
                continue;
            }
            if (explanation.map(value -> StringUtils.length(value) > 250).orElse(false)) {
                LOG.warn(
                    "Skipping DX code '{}' for court '{}' because explanation exceeds 250 characters",
                    dxCode.get(),
                    courtId
                );
                rejectDx(context, court, "DX_EXPLANATION_TOO_LONG");
                continue;
            }
            if (explanation.isPresent()
                && !GENERIC_DESCRIPTION_PATTERN.matcher(explanation.get()).matches()) {
                LOG.warn(
                    "Skipping DX code '{}' for court '{}' because explanation has invalid characters",
                    dxCode.get(),
                    courtId
                );
                rejectDx(context, court, "DX_EXPLANATION_INVALID");
                continue;
            }
            if (!Objects.equals(dto.getDxCode(), dxCode.get())
                || !Objects.equals(StringUtils.trimToNull(dto.getExplanation()), explanation.orElse(null))) {
                recordTransformation(
                    context,
                    MigrationSection.COURT_DX_CODES,
                    "DX_VALUE_SANITISED",
                    court,
                    List.of("dxCode", "explanation")
                );
            }
            CourtDxCode.CourtDxCodeBuilder entityBuilder = CourtDxCode.builder()
                .courtId(courtId)
                .dxCode(dxCode.get());
            explanation.ifPresent(entityBuilder::explanation);
            CourtDxCode entity = entityBuilder.build();
            courtDxCodeRepository.save(entity);
            context.courtDxCodesMigrated++;
            context.persisted(MigrationSection.COURT_DX_CODES, 1, 0);
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
                context.discarded(MigrationSection.COURT_FAX, 1, 0);
                continue;
            }
            CourtFax entity = CourtFax.builder()
                .courtId(courtId)
                .faxNumber(dto.getFaxNumber())
                .build();
            courtFaxRepository.save(entity);
            context.courtFaxMigrated++;
            context.persisted(MigrationSection.COURT_FAX, 1, 0);
        }
    }

    private void persistLegacyCourtMapping(Long legacyCourtId, UUID courtId, MigrationContext context) {
        if (legacyCourtId == null) {
            return;
        }
        legacyCourtMappingRepository.save(
            LegacyCourtMapping.builder()
                .courtId(courtId)
                .legacyCourtId(legacyCourtId)
                .build()
        );
        context.persisted(MigrationSection.LEGACY_COURT_MAPPINGS, 1, 0);
    }

    /**
     * Resolves the region ID for the supplied court.
     *
     * @param dto legacy court payload.
     * @param context migration context containing mapped region IDs.
     * @return mapped region ID, or empty when the court should be skipped.
     */
    private Optional<UUID> resolveRegionId(CourtDto dto, MigrationContext context) {
        UUID regionId = dto.getRegionId() == null ? null : context.getRegionIds().get(dto.getRegionId());
        if (regionId != null) {
            return Optional.of(regionId);
        }
        return Optional.empty();
    }

    /**
     * Normalises the catchment type value coming from the legacy service area payload.
     *
     * @param value legacy catchment text.
     * @return parsed {@link CatchmentType}, or empty if the value is blank/unknown.
     */
    private static Optional<CatchmentType> parseCatchmentType(String value) {
        if (StringUtils.isBlank(value)) {
            return Optional.empty();
        }
        try {
            return Optional.of(CatchmentType.valueOf(StringUtils.upperCase(value).replace('-', '_')));
        } catch (IllegalArgumentException ex) {
            LOG.warn("Unknown catchment method '{}'", value);
            return Optional.empty();
        }
    }

    private static List<UUID> mapIds(List<Integer> sourceIds, Map<Integer, UUID> lookup) {
        if (sourceIds == null || sourceIds.isEmpty()) {
            return List.of();
        }

        List<UUID> results = new ArrayList<>();
        for (Integer id : sourceIds) {
            UUID mapped = lookup.get(id);
            if (mapped != null) {
                results.add(mapped);
            }
        }

        return results.isEmpty() ? List.of() : results;
    }

    private static List<UUID> mapLocalAuthorityIds(
        List<Integer> sourceIds,
        Map<Integer, List<UUID>> lookup
    ) {
        if (sourceIds == null || sourceIds.isEmpty()) {
            return List.of();
        }

        LinkedHashSet<UUID> results = new LinkedHashSet<>();
        for (Integer id : sourceIds) {
            List<UUID> mapped = lookup.get(id);
            if (mapped == null || mapped.isEmpty()) {
                LOG.warn("Unable to map court local authorities identifier '{}' in migration payload", id);
                continue;
            }
            results.addAll(mapped);
        }

        return results.isEmpty() ? List.of() : new ArrayList<>(results);
    }

    private static List<Integer> unmappedIds(List<Integer> sourceIds, Map<Integer, UUID> lookup) {
        if (sourceIds == null) {
            return List.of();
        }
        return sourceIds.stream()
            .filter(id -> !lookup.containsKey(id))
            .toList();
    }

    private static List<Integer> unmappedLocalAuthorityIds(
        List<Integer> sourceIds,
        Map<Integer, List<UUID>> lookup
    ) {
        if (sourceIds == null) {
            return List.of();
        }
        return sourceIds.stream()
            .filter(id -> !lookup.containsKey(id) || lookup.get(id).isEmpty())
            .toList();
    }

    private static void recordCourtError(
        MigrationContext context,
        CourtDto court,
        String reasonCode,
        List<Integer> referenceIds
    ) {
        context.skipped(MigrationSection.COURTS, 1);
        context.unmapped(MigrationSection.COURTS, referenceIds.size());
        context.finding(
            MigrationFindingSeverity.ERROR,
            MigrationFindingType.SKIPPED,
            MigrationSection.COURTS,
            reasonCode,
            court.getId(),
            court.getSlug(),
            null,
            referenceIds,
            List.of(),
            1,
            referenceIds.size()
        );
    }

    private static void recordUnmappedChild(
        MigrationContext context,
        MigrationSection section,
        String reasonCode,
        CourtDto court,
        List<Integer> referenceIds
    ) {
        context.skipped(section, 1);
        context.unmapped(section, referenceIds.size());
        context.finding(
            MigrationFindingSeverity.ERROR,
            MigrationFindingType.UNMAPPED,
            section,
            reasonCode,
            court.getId(),
            court.getSlug(),
            null,
            referenceIds,
            List.of(),
            1,
            referenceIds.size()
        );
    }

    private static void recordOrdinaryCourtServiceAreaDiscard(
        CourtDto court,
        MigrationContext context
    ) {
        if (isEmpty(court.getCourtServiceAreas())) {
            return;
        }
        for (uk.gov.hmcts.reform.fact.data.api.migration.model.CourtServiceAreaDto serviceArea
            : court.getCourtServiceAreas()) {
            int referenceCount = serviceArea.getServiceAreaIds() == null
                ? 0
                : serviceArea.getServiceAreaIds().size();
            context.discarded(MigrationSection.ORDINARY_COURT_SERVICE_AREAS, 1, referenceCount);
            context.finding(
                MigrationFindingSeverity.REVIEW,
                MigrationFindingType.APPROVED_DISCARD,
                MigrationSection.ORDINARY_COURT_SERVICE_AREAS,
                "ORDINARY_COURT_SERVICE_AREA_NOT_IMPORTED",
                court.getId(),
                court.getSlug(),
                serviceArea.getId(),
                serviceArea.getServiceAreaIds(),
                List.of("catchmentType", "serviceAreaIds"),
                1,
                referenceCount
            );
        }
    }

    private static void rejectDx(MigrationContext context, CourtDto court, String reasonCode) {
        context.skipped(MigrationSection.COURT_DX_CODES, 1);
        context.finding(
            MigrationFindingSeverity.ERROR,
            MigrationFindingType.REJECTED,
            MigrationSection.COURT_DX_CODES,
            reasonCode,
            court.getId(),
            court.getSlug(),
            null,
            List.of(),
            List.of("dxCode", "explanation"),
            1,
            0
        );
    }

    private static void recordTransformation(
        MigrationContext context,
        MigrationSection section,
        String reasonCode,
        CourtDto court,
        List<String> fields
    ) {
        recordTransformation(context, section, reasonCode, court.getId(), court.getSlug(), fields);
    }

    private static void recordTransformation(
        MigrationContext context,
        MigrationSection section,
        String reasonCode,
        Long legacyCourtId,
        String courtSlug,
        List<String> fields
    ) {
        context.transformed(section, 1);
        context.finding(
            MigrationFindingSeverity.INFO,
            MigrationFindingType.TRANSFORMED,
            section,
            reasonCode,
            legacyCourtId,
            courtSlug,
            null,
            List.of(),
            fields,
            1,
            0
        );
    }

    private static boolean isEmpty(Collection<?> values) {
        return values == null || values.isEmpty();
    }

    private String sanitiseCourtName(String name) {
        if (StringUtils.isBlank(name)) {
            return name;
        }
        String cleaned = name.replaceAll("[^A-Za-z&'()\\- ]", " ");
        return cleaned.replaceAll("\\s+", " ").trim();
    }

    private static Optional<String> sanitiseGenericDescription(String value) {
        if (StringUtils.isBlank(value)) {
            return Optional.empty();
        }
        String cleaned = INVALID_GENERIC_DESCRIPTION_CHARACTERS.matcher(value).replaceAll(" ");
        String normalised = cleaned.replaceAll("\\s+", " ").trim();
        return StringUtils.isBlank(normalised) ? Optional.empty() : Optional.of(normalised);
    }

    private record LegacySlugOverride(String expectedName, String expectedSlug) {
    }
}
