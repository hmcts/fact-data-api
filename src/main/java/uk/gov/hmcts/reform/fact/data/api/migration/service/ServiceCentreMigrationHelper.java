package uk.gov.hmcts.reform.fact.data.api.migration.service;

import jakarta.validation.ConstraintViolationException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
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
                continue;
            }
            if (!SERVICE_CENTRE_NAME_PATTERN.matcher(serviceCentreName).matches()) {
                LOG.warn(
                    "Skipping service centre {} because sanitised name '{}' still fails validation regex",
                    dto.getSlug(),
                    serviceCentreName
                );
                continue;
            }
            if (StringUtils.length(serviceCentreName) < 5 || StringUtils.length(serviceCentreName) > 200) {
                LOG.warn("Skipping service centre {} because name length is outside 5-200 characters", dto.getSlug());
                continue;
            }

            ServiceAreaSelection serviceAreaSelection = selectServiceAreas(dto.getCourtServiceAreas(), context);
            ServiceCentre serviceCentre = ServiceCentre.builder()
                .name(serviceCentreName)
                .slug(dto.getSlug())
                .open(Boolean.FALSE)
                .serviceAreaIds(serviceAreaSelection.serviceAreaIds())
                .catchmentType(serviceAreaSelection.catchmentType().orElse(null))
                .build();

            ServiceCentre savedServiceCentre;
            try {
                savedServiceCentre = serviceCentreRepository.save(serviceCentre);
            } catch (ConstraintViolationException ex) {
                LOG.error("Validation failed while migrating service centre '{}': {}", dto.getName(), ex.getMessage());
                throw ex;
            }

            UUID serviceCentreId = savedServiceCentre.getId();
            persistServiceCentreAreasOfLaw(dto.getCourtAreasOfLaw(), serviceCentreId, context);
            total++;
        }
        return total;
    }

    private void persistServiceCentreAreasOfLaw(
        CourtAreasOfLawDto dto,
        UUID serviceCentreId,
        MigrationContext context
    ) {
        if (dto == null || isEmpty(dto.getAreaOfLawIds())) {
            return;
        }

        List<UUID> areasOfLaw = mapIds(
            dto.getAreaOfLawIds(),
            context.getAreaOfLawIds(),
            "service centre areas of law"
        );
        if (areasOfLaw.isEmpty()) {
            LOG.warn("Skipping service centre areas of law for {} because all area ids were unmapped", serviceCentreId);
            return;
        }

        serviceCentreAreasOfLawRepository.save(ServiceCentreAreasOfLaw.builder()
            .serviceCentreId(serviceCentreId)
            .areasOfLaw(areasOfLaw)
            .build());
        context.serviceCentreAreasOfLawMigrated++;
    }

    private ServiceAreaSelection selectServiceAreas(List<CourtServiceAreaDto> dtos, MigrationContext context) {
        if (isEmpty(dtos)) {
            return ServiceAreaSelection.empty();
        }

        return dtos.stream()
            .map(dto -> new ServiceAreaSelection(
                mapIds(dto.getServiceAreaIds(), context.getServiceAreaIds(), "service centre service area"),
                parseCatchmentType(dto.getCatchmentType())
            ))
            .filter(selection -> !selection.serviceAreaIds().isEmpty() || selection.catchmentType().isPresent())
            .min(Comparator.comparing(ServiceCentreMigrationHelper::catchmentPriority))
            .orElseGet(ServiceAreaSelection::empty);
    }

    private static int catchmentPriority(ServiceAreaSelection selection) {
        if (selection.catchmentType().isEmpty()) {
            return 3;
        }
        return switch (selection.catchmentType().get()) {
            case NATIONAL -> 0;
            case REGIONAL -> 1;
            case LOCAL -> 2;
        };
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

    private static List<UUID> mapIds(List<Integer> sourceIds, Map<Integer, UUID> lookup, String context) {
        if (sourceIds == null || sourceIds.isEmpty()) {
            return List.of();
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

        return results.isEmpty() ? List.of() : results;
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
        private static ServiceAreaSelection empty() {
            return new ServiceAreaSelection(List.of(), Optional.empty());
        }
    }
}
