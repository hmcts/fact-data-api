package uk.gov.hmcts.reform.fact.data.api.services.search;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.fact.data.api.dto.SearchResult;
import uk.gov.hmcts.reform.fact.data.api.entities.types.SearchAction;

import java.util.Comparator;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class SearchLocationService {

    private static final Set<String> COURT_ONLY_DOCUMENT_SERVICE_AREAS = Set.of(
        "Adoption",
        "Childcare arrangements if you separate from your partner"
    );
    private static final Set<String> COURT_ONLY_UPDATE_SERVICE_AREAS = Set.of(
        "Bankruptcy",
        "Female Genital Mutilation Protection Orders",
        "Forced marriage",
        "Housing"
    );

    private final SearchCourtService searchCourtService;
    private final SearchServiceCentreService searchServiceCentreService;

    /**
     * Search locations by postcode. Court results preserve the existing search behaviour;
     * service-centre results are included for service-area searches where the new service-centre
     * catchment data applies.
     *
     * @param postcode the postcode to search from
     * @param serviceArea optional service area name
     * @param action optional search action
     * @param limit maximum number of combined results
     * @return court and service-centre search results
     */
    public List<SearchResult> getLocationsBySearchParameters(String postcode,
                                                             String serviceArea,
                                                             SearchAction action,
                                                             Integer limit) {
        List<SearchResult> courts = searchCourtService
            .getCourtsBySearchParameters(postcode, serviceArea, action, limit)
            .stream()
            .map(SearchResult::fromCourt)
            .toList();

        if (isCourtOnlySearch(serviceArea, action)) {
            return courts;
        }

        List<SearchResult> serviceCentres = searchServiceCentreService
            .getServiceCentresBySearchParameters(postcode, serviceArea, action, limit)
            .stream()
            .map(SearchResult::fromServiceCentre)
            .toList();

        return java.util.stream.Stream.concat(courts.stream(), serviceCentres.stream())
            .sorted(Comparator.comparing(SearchResult::getDistance))
            .limit(limit)
            .toList();
    }

    private boolean isCourtOnlySearch(String serviceArea, SearchAction action) {
        if (serviceArea == null || action == null) {
            return false;
        }

        return switch (action) {
            case DOCUMENTS -> containsIgnoreCase(COURT_ONLY_DOCUMENT_SERVICE_AREAS, serviceArea);
            case UPDATE -> containsIgnoreCase(COURT_ONLY_UPDATE_SERVICE_AREAS, serviceArea);
            case NEAREST -> false;
        };
    }

    private boolean containsIgnoreCase(Set<String> serviceAreas, String serviceArea) {
        return serviceAreas.stream().anyMatch(serviceArea::equalsIgnoreCase);
    }
}
