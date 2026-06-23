package uk.gov.hmcts.reform.fact.data.api.services.search;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.fact.data.api.dto.SearchResult;
import uk.gov.hmcts.reform.fact.data.api.entities.types.SearchAction;

import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
public class SearchLocationService {

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
}
