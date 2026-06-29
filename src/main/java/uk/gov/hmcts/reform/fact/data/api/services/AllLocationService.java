package uk.gov.hmcts.reform.fact.data.api.services;

import org.apache.commons.lang3.StringUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.fact.data.api.dto.AllLocation;
import uk.gov.hmcts.reform.fact.data.api.dto.AllLocationDetails;
import uk.gov.hmcts.reform.fact.data.api.entities.Court;
import uk.gov.hmcts.reform.fact.data.api.entities.Region;
import uk.gov.hmcts.reform.fact.data.api.entities.ServiceCentre;
import uk.gov.hmcts.reform.fact.data.api.errorhandling.exceptions.InvalidParameterCombinationException;
import uk.gov.hmcts.reform.fact.data.api.repositories.CourtDetailsRepository;
import uk.gov.hmcts.reform.fact.data.api.repositories.CourtRepository;
import uk.gov.hmcts.reform.fact.data.api.repositories.ServiceCentreDetailsRepository;
import uk.gov.hmcts.reform.fact.data.api.repositories.ServiceCentreRepository;

import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.function.Predicate;
import java.util.stream.Stream;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AllLocationService {

    private static final String SORT_BY_NAME = "name";
    private static final String SORT_BY_LAST_UPDATED = "lastupdated";
    private static final String SORT_ORDER_ASC = "asc";
    private static final String SORT_ORDER_DESC = "desc";

    private final CourtRepository courtRepository;
    private final ServiceCentreRepository serviceCentreRepository;
    private final CourtDetailsRepository courtDetailsRepository;
    private final ServiceCentreDetailsRepository serviceCentreDetailsRepository;
    private final RegionService regionService;
    private final CourtDetailsViewService courtDetailsViewService;
    private final ServiceCentreDetailsViewService serviceCentreDetailsViewService;

    public Page<AllLocation> getFilteredAndPaginatedLocations(int pageNumber, int pageSize, Boolean includeClosed,
                                                              Boolean onlyServiceCentres, String regionId,
                                                              String partialName,
                                                              String sortBy, String sortOrder) {
        UUID regionFilter = resolveRegionFilter(regionId);
        String nameFilter = partialName == null ? "" : partialName.toLowerCase(Locale.ROOT);
        Stream<AllLocation> locations = Boolean.TRUE.equals(onlyServiceCentres)
            ? getFilteredServiceCentres(includeClosed, regionFilter, nameFilter).map(AllLocation::fromServiceCentre)
            : Stream.concat(
                getFilteredCourts(includeClosed, regionFilter, nameFilter).map(AllLocation::fromCourt),
                getFilteredServiceCentres(includeClosed, regionFilter, nameFilter).map(AllLocation::fromServiceCentre)
        );

        return page(sortIfRequested(locations, sortBy, sortOrder), pageNumber, pageSize);
    }

    public Page<AllLocation> getFilteredAndPaginatedCourts(int pageNumber, int pageSize, Boolean includeClosed,
                                                           String regionId, String partialName,
                                                           String sortBy, String sortOrder) {
        UUID regionFilter = resolveRegionFilter(regionId);
        String nameFilter = partialName == null ? "" : partialName.toLowerCase(Locale.ROOT);
        Stream<AllLocation> locations = getFilteredCourts(includeClosed, regionFilter, nameFilter)
            .map(AllLocation::fromCourt);

        return page(sortIfRequested(locations, sortBy, sortOrder), pageNumber, pageSize);
    }

    public Page<AllLocation> getFilteredAndPaginatedServiceCentres(int pageNumber, int pageSize, Boolean includeClosed,
                                                                   String regionId, String partialName,
                                                                   String sortBy, String sortOrder) {
        UUID regionFilter = resolveRegionFilter(regionId);
        String nameFilter = partialName == null ? "" : partialName.toLowerCase(Locale.ROOT);
        Stream<AllLocation> locations = getFilteredServiceCentres(includeClosed, regionFilter, nameFilter)
            .map(AllLocation::fromServiceCentre);

        return page(sortIfRequested(locations, sortBy, sortOrder), pageNumber, pageSize);
    }

    public List<AllLocationDetails> getAllLocationDetails() {
        return Stream.concat(
                getAllCourtDetails().stream(),
                getAllServiceCentreDetails().stream()
            )
            .toList();
    }

    public List<AllLocationDetails> getAllCourtDetails() {
        return courtDetailsRepository.findAll()
            .stream()
            .map(courtDetailsViewService::prepareDetailsView)
            .map(AllLocationDetails::fromCourt)
            .toList();
    }

    public List<AllLocationDetails> getAllServiceCentreDetails() {
        return serviceCentreDetailsRepository.findAll()
            .stream()
            .map(serviceCentreDetailsViewService::prepareDetailsView)
            .map(AllLocationDetails::fromServiceCentre)
            .toList();
    }

    private Stream<Court> getFilteredCourts(Boolean includeClosed, UUID regionFilter, String nameFilter) {
        Predicate<Court> predicate = court -> matchesOpen(court.getOpen(), includeClosed)
            && matchesName(court.getName(), nameFilter)
            && (regionFilter == null || regionFilter.equals(court.getRegionId()));

        return courtRepository.findAll().stream().filter(predicate);
    }

    private Stream<ServiceCentre> getFilteredServiceCentres(Boolean includeClosed, UUID regionFilter, String nameFilter) {
        if (regionFilter != null) {
            return Stream.empty();
        }

        Predicate<ServiceCentre> predicate = serviceCentre -> matchesOpen(serviceCentre.getOpen(), includeClosed)
            && matchesName(serviceCentre.getName(), nameFilter);

        return serviceCentreRepository.findAll().stream().filter(predicate);
    }

    private UUID resolveRegionFilter(String regionId) {
        if (StringUtils.isBlank(regionId)) {
            return null;
        }

        return regionService.getRegionById(UUID.fromString(regionId)).getId();
    }

    private boolean matchesOpen(Boolean open, Boolean includeClosed) {
        return Boolean.TRUE.equals(includeClosed) || Boolean.TRUE.equals(open);
    }

    private boolean matchesName(String name, String nameFilter) {
        return StringUtils.isBlank(nameFilter) || name.toLowerCase(Locale.ROOT).contains(nameFilter);
    }

    private Comparator<AllLocation> buildComparator(String sortBy, String sortOrder) {
        String normalizedSortBy = sortBy.trim().toLowerCase(Locale.ROOT);
        String normalizedSortOrder = StringUtils.isBlank(sortOrder) ? SORT_ORDER_ASC
            : sortOrder.trim().toLowerCase(Locale.ROOT);

        if (!SORT_ORDER_ASC.equals(normalizedSortOrder) && !SORT_ORDER_DESC.equals(normalizedSortOrder)) {
            throw new InvalidParameterCombinationException("sortOrder must be one of: asc, desc");
        }

        return switch (normalizedSortBy) {
            case SORT_BY_NAME -> byName(normalizedSortOrder);
            case SORT_BY_LAST_UPDATED -> byLastUpdated(normalizedSortOrder);
            default -> throw new InvalidParameterCombinationException("sortBy must be one of: name, lastUpdated");
        };
    }

    private List<AllLocation> sortIfRequested(Stream<AllLocation> locations, String sortBy, String sortOrder) {
        if (StringUtils.isBlank(sortBy)) {
            if (!StringUtils.isBlank(sortOrder)) {
                throw new InvalidParameterCombinationException("sortOrder cannot be provided without sortBy");
            }
            return locations.toList();
        }

        return locations.sorted(buildComparator(sortBy, sortOrder)).toList();
    }

    private Comparator<AllLocation> byName(String sortOrder) {
        Comparator<AllLocation> comparator = Comparator
            .comparing((AllLocation location) -> location.getName().toLowerCase(Locale.ROOT))
            .thenComparing(AllLocation::getLocationType)
            .thenComparing(AllLocation::getId);
        return SORT_ORDER_DESC.equals(sortOrder) ? comparator.reversed() : comparator;
    }

    private Comparator<AllLocation> byLastUpdated(String sortOrder) {
        Comparator<AllLocation> comparator = Comparator
            .comparing(
                AllLocation::getLastUpdatedAt,
                Comparator.nullsLast(Comparator.naturalOrder())
            )
            .thenComparing((AllLocation location) -> location.getName().toLowerCase(Locale.ROOT))
            .thenComparing(AllLocation::getId);
        return SORT_ORDER_DESC.equals(sortOrder) ? comparator.reversed() : comparator;
    }

    private Page<AllLocation> page(List<AllLocation> locations, int pageNumber, int pageSize) {
        int fromIndex = Math.min(pageNumber * pageSize, locations.size());
        int toIndex = Math.min(fromIndex + pageSize, locations.size());

        return new PageImpl<>(
            locations.subList(fromIndex, toIndex),
            PageRequest.of(pageNumber, pageSize),
            locations.size()
        );
    }
}
