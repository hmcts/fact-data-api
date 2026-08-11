package uk.gov.hmcts.reform.fact.data.api.migration.service;

import jakarta.validation.ConstraintViolationException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.regex.Pattern;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.gov.hmcts.reform.fact.data.api.entities.ServiceCentre;
import uk.gov.hmcts.reform.fact.data.api.entities.ServiceCentreAreasOfLaw;
import uk.gov.hmcts.reform.fact.data.api.entities.types.CatchmentType;
import uk.gov.hmcts.reform.fact.data.api.migration.model.CourtAreasOfLawDto;
import uk.gov.hmcts.reform.fact.data.api.migration.model.CourtDto;
import uk.gov.hmcts.reform.fact.data.api.migration.model.CourtServiceAreaDto;
import uk.gov.hmcts.reform.fact.data.api.migration.model.MigrationFindingSeverity;
import uk.gov.hmcts.reform.fact.data.api.migration.model.MigrationFindingType;
import uk.gov.hmcts.reform.fact.data.api.migration.model.MigrationSection;
import uk.gov.hmcts.reform.fact.data.api.repositories.ServiceCentreAreasOfLawRepository;
import uk.gov.hmcts.reform.fact.data.api.repositories.ServiceCentreRepository;

class ServiceCentreMigrationHelper {

    private static final Logger LOG = LoggerFactory.getLogger(ServiceCentreMigrationHelper.class);
    private static final Pattern SERVICE_CENTRE_NAME_PATTERN = Pattern.compile("^[A-Za-z&'()\\- ]+$");

    private final ServiceCentreRepository serviceCentreRepository;
    private final ServiceCentreAreasOfLawRepository serviceCentreAreasOfLawRepository;

    ServiceCentreMigrationHelper(
        ServiceCentreRepository serviceCentreRepository,
        ServiceCentreAreasOfLawRepository serviceCentreAreasOfLawRepository
    ) {
        this.serviceCentreRepository = serviceCentreRepository;
        this.serviceCentreAreasOfLawRepository = serviceCentreAreasOfLawRepository;
    }

    int migrateServiceCentres(List<CourtDto> courts, MigrationContext context) {
        if (isEmpty(courts)) {
            return 0;
        }

        int total = 0;
        for (CourtDto dto : courts) {
            if (!Boolean.TRUE.equals(dto.getIsServiceCentre())) {
                continue;
            }

            String serviceCentreName = sanitiseServiceCentreName(dto.getName());
            if (StringUtils.isBlank(serviceCentreName)) {
                LOG.warn("Skipping service centre {} because sanitised name was blank", dto.getSlug());
                recordServiceCentreError(context, dto, "SERVICE_CENTRE_NAME_BLANK_AFTER_SANITISATION", List.of());
                continue;
            }
            if (!SERVICE_CENTRE_NAME_PATTERN.matcher(serviceCentreName).matches()) {
                LOG.warn(
                    "Skipping service centre {} because sanitised name '{}' still fails validation regex",
                    dto.getSlug(),
                    serviceCentreName
                );
                recordServiceCentreError(context, dto, "SERVICE_CENTRE_NAME_INVALID_AFTER_SANITISATION", List.of());
                continue;
            }
            if (StringUtils.length(serviceCentreName) < 5 || StringUtils.length(serviceCentreName) > 200) {
                LOG.warn("Skipping service centre {} because name length is outside 5-200 characters", dto.getSlug());
                recordServiceCentreError(context, dto, "SERVICE_CENTRE_NAME_LENGTH_INVALID", List.of());
                continue;
            }
            if (!Objects.equals(dto.getName(), serviceCentreName)) {
                recordTransformation(context, dto, MigrationSection.SERVICE_CENTRES, "SERVICE_CENTRE_NAME_SANITISED");
            }
            recordDeferredDetails(dto, context);

            ServiceAreaSelection serviceAreaSelection = selectServiceAreas(dto, context);
            UUID regionId = mapRegionId(dto, context);
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
            ServiceCentre serviceCentre = ServiceCentre.builder()
                .name(serviceCentreName)
                .slug(dto.getSlug())
                .open(Boolean.TRUE)
                .warningNotice(warningNotice)
                .warningNoticeCy(warningNoticeCy)
                .serviceAreaIds(serviceAreaSelection.serviceAreaIds())
                .regionId(regionId)
                .catchmentType(serviceAreaSelection.catchmentType().orElse(null))
                .build();

            ServiceCentre savedServiceCentre;
            try {
                savedServiceCentre = serviceCentreRepository.save(serviceCentre);
            } catch (ConstraintViolationException ex) {
                LOG.error("Validation failed while migrating service centre '{}': {}", dto.getName(), ex.getMessage());
                throw ex;
            }
            if (StringUtils.isNotBlank(savedServiceCentre.getWarningNotice())
                || StringUtils.isNotBlank(savedServiceCentre.getWarningNoticeCy())) {
                context.warningNoticesMigrated++;
                context.persisted(MigrationSection.WARNING_NOTICES, 1, 0);
            }
            if (!Objects.equals(dto.getWarningNotice(), warningNotice)
                || !Objects.equals(dto.getWarningNoticeCy(), warningNoticeCy)) {
                recordTransformation(
                    context,
                    dto,
                    MigrationSection.WARNING_NOTICES,
                    "WARNING_NOTICE_SANITISED"
                );
            }

            final UUID serviceCentreId = savedServiceCentre.getId();
            context.persisted(MigrationSection.SERVICE_CENTRES, 1, 0);
            context.persisted(MigrationSection.COURT_SLUGS, 1, 0);
            if (!serviceAreaSelection.serviceAreaIds().isEmpty()
                || serviceAreaSelection.catchmentType().isPresent()) {
                context.persisted(
                    MigrationSection.SERVICE_CENTRE_SERVICE_AREAS,
                    1,
                    serviceAreaSelection.serviceAreaIds().size()
                );
            }
            persistServiceCentreAreasOfLaw(dto, serviceCentreId, context);
            total++;
        }
        return total;
    }

    private void persistServiceCentreAreasOfLaw(
        CourtDto court,
        UUID serviceCentreId,
        MigrationContext context
    ) {
        CourtAreasOfLawDto dto = court.getCourtAreasOfLaw();
        if (dto == null || isEmpty(dto.getAreaOfLawIds())) {
            return;
        }

        List<Integer> unmappedIds = unmappedIds(dto.getAreaOfLawIds(), context.getAreaOfLawIds());
        if (!unmappedIds.isEmpty()) {
            recordUnmapped(
                context,
                court,
                MigrationSection.SERVICE_CENTRE_AREAS_OF_LAW,
                "SERVICE_CENTRE_AREA_OF_LAW_UNMAPPED",
                unmappedIds
            );
            return;
        }
        List<UUID> areasOfLaw = mapIds(dto.getAreaOfLawIds(), context.getAreaOfLawIds());

        serviceCentreAreasOfLawRepository.save(ServiceCentreAreasOfLaw.builder()
            .serviceCentreId(serviceCentreId)
            .areasOfLaw(areasOfLaw)
            .build());
        context.serviceCentreAreasOfLawMigrated++;
        context.persisted(
            MigrationSection.SERVICE_CENTRE_AREAS_OF_LAW,
            1,
            dto.getAreaOfLawIds().size()
        );
    }

    private ServiceAreaSelection selectServiceAreas(CourtDto court, MigrationContext context) {
        List<CourtServiceAreaDto> dtos = court.getCourtServiceAreas();
        if (isEmpty(dtos)) {
            return ServiceAreaSelection.empty();
        }

        List<ServiceAreaSelection> selections = dtos.stream()
            .map(dto -> mapServiceAreaSelection(court, dto, context))
            .filter(selection -> !selection.serviceAreaIds().isEmpty() || selection.catchmentType().isPresent())
            .toList();
        ServiceAreaSelection selected = selections.stream()
            .min(Comparator.comparing(ServiceCentreMigrationHelper::catchmentPriority))
            .orElseGet(ServiceAreaSelection::empty);
        int discarded = Math.max(0, selections.size() - (selected == ServiceAreaSelection.empty() ? 0 : 1));
        if (discarded > 0) {
            context.discarded(MigrationSection.SERVICE_CENTRE_SERVICE_AREAS, discarded, 0);
            context.finding(
                MigrationFindingSeverity.REVIEW,
                MigrationFindingType.APPROVED_DISCARD,
                MigrationSection.SERVICE_CENTRE_SERVICE_AREAS,
                "LOWER_PRIORITY_SERVICE_CENTRE_CATCHMENT_DISCARDED",
                court.getId(),
                court.getSlug(),
                null,
                List.of(),
                List.of("catchmentType", "serviceAreaIds"),
                discarded,
                0
            );
        }
        return selected;
    }

    private ServiceAreaSelection mapServiceAreaSelection(
        CourtDto court,
        CourtServiceAreaDto dto,
        MigrationContext context
    ) {
        List<Integer> unmappedIds = unmappedIds(dto.getServiceAreaIds(), context.getServiceAreaIds());
        if (!unmappedIds.isEmpty()) {
            recordUnmapped(
                context,
                court,
                MigrationSection.SERVICE_CENTRE_SERVICE_AREAS,
                "SERVICE_CENTRE_SERVICE_AREA_UNMAPPED",
                unmappedIds
            );
        }
        Optional<CatchmentType> catchment = parseCatchmentType(dto.getCatchmentType());
        if (StringUtils.isNotBlank(dto.getCatchmentType()) && catchment.isEmpty()) {
            context.skipped(MigrationSection.SERVICE_CENTRE_SERVICE_AREAS, 1);
            context.finding(
                MigrationFindingSeverity.ERROR,
                MigrationFindingType.REJECTED,
                MigrationSection.SERVICE_CENTRE_SERVICE_AREAS,
                "SERVICE_CENTRE_CATCHMENT_UNKNOWN",
                court.getId(),
                court.getSlug(),
                dto.getId(),
                List.of(),
                List.of("catchmentType"),
                1,
                0
            );
        }
        return new ServiceAreaSelection(
            mapIds(dto.getServiceAreaIds(), context.getServiceAreaIds()),
            catchment
        );
    }

    private static int catchmentPriority(ServiceAreaSelection selection) {
        return selection.catchmentType()
            .map(catchmentType -> switch (catchmentType) {
                    case NATIONAL -> 0;
                    case REGIONAL -> 1;
                    case LOCAL -> 2;
                })
            .orElse(3);
    }

    private static Optional<CatchmentType> parseCatchmentType(String value) {
        if (StringUtils.isBlank(value)) {
            return Optional.empty();
        }
        try {
            return Optional.of(CatchmentType.valueOf(StringUtils.upperCase(value).replace('-', '_')));
        } catch (IllegalArgumentException ex) {
            LOG.warn("Unknown service centre catchment type '{}'", value);
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

    private static UUID mapRegionId(CourtDto dto, MigrationContext context) {
        if (dto.getRegionId() == null) {
            return null;
        }

        UUID mappedRegionId = context.getRegionIds().get(dto.getRegionId());
        if (mappedRegionId == null) {
            LOG.warn("Unable to map service centre region identifier '{}' in migration payload", dto.getRegionId());
            recordUnmapped(
                context,
                dto,
                MigrationSection.SERVICE_CENTRES,
                "SERVICE_CENTRE_REGION_UNMAPPED",
                List.of(dto.getRegionId())
            );
        }
        return mappedRegionId;
    }

    private static List<Integer> unmappedIds(List<Integer> sourceIds, Map<Integer, UUID> lookup) {
        if (sourceIds == null) {
            return List.of();
        }
        return sourceIds.stream()
            .filter(id -> !lookup.containsKey(id))
            .toList();
    }

    private static void recordServiceCentreError(
        MigrationContext context,
        CourtDto court,
        String reasonCode,
        List<Integer> referenceIds
    ) {
        context.skipped(MigrationSection.SERVICE_CENTRES, 1);
        context.unmapped(MigrationSection.SERVICE_CENTRES, referenceIds.size());
        context.finding(
            MigrationFindingSeverity.ERROR,
            MigrationFindingType.SKIPPED,
            MigrationSection.SERVICE_CENTRES,
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

    private static void recordUnmapped(
        MigrationContext context,
        CourtDto court,
        MigrationSection section,
        String reasonCode,
        List<Integer> referenceIds
    ) {
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

    private static void recordTransformation(
        MigrationContext context,
        CourtDto court,
        MigrationSection section,
        String reasonCode
    ) {
        context.transformed(section, 1);
        context.finding(
            MigrationFindingSeverity.INFO,
            MigrationFindingType.TRANSFORMED,
            section,
            reasonCode,
            court.getId(),
            court.getSlug(),
            null,
            List.of(),
            List.of(),
            1,
            0
        );
    }

    private static void recordDeferredDetails(CourtDto court, MigrationContext context) {
        int deferred = size(court.getCourtLocalAuthorities())
            + present(court.getCourtSinglePointsOfEntry())
            + present(court.getCourtProfessionalInformation())
            + present(court.getCourtCodes())
            + size(court.getCourtDxCodes())
            + size(court.getCourtFax());
        if (deferred == 0) {
            return;
        }
        context.discarded(MigrationSection.SERVICE_CENTRE_DEFERRED_DETAILS, deferred, 0);
        context.finding(
            MigrationFindingSeverity.REVIEW,
            MigrationFindingType.DEFERRED,
            MigrationSection.SERVICE_CENTRE_DEFERRED_DETAILS,
            "SERVICE_CENTRE_DETAILS_DEFERRED_TO_MANUAL_WORKSTREAM",
            court.getId(),
            court.getSlug(),
            null,
            List.of(),
            List.of(),
            deferred,
            0
        );
    }

    private static int present(Object value) {
        return value == null ? 0 : 1;
    }

    private static int size(Collection<?> values) {
        return values == null ? 0 : values.size();
    }

    private static boolean isEmpty(Collection<?> values) {
        return values == null || values.isEmpty();
    }

    private static String sanitiseServiceCentreName(String name) {
        if (StringUtils.isBlank(name)) {
            return name;
        }
        String cleaned = name.replaceAll("[^A-Za-z&'()\\- ]", " ");
        return cleaned.replaceAll("\\s+", " ").trim();
    }

    private record ServiceAreaSelection(List<UUID> serviceAreaIds, Optional<CatchmentType> catchmentType) {
        private static final ServiceAreaSelection EMPTY = new ServiceAreaSelection(List.of(), Optional.empty());

        private static ServiceAreaSelection empty() {
            return EMPTY;
        }
    }
}
