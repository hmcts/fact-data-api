package uk.gov.hmcts.reform.fact.data.api.services.search;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.fact.data.api.dto.ServiceCentreWithDistance;
import uk.gov.hmcts.reform.fact.data.api.entities.ServiceArea;
import uk.gov.hmcts.reform.fact.data.api.entities.types.CatchmentType;
import uk.gov.hmcts.reform.fact.data.api.entities.types.SearchAction;
import uk.gov.hmcts.reform.fact.data.api.errorhandling.exceptions.InvalidParameterCombinationException;
import uk.gov.hmcts.reform.fact.data.api.os.OsLocationData;
import uk.gov.hmcts.reform.fact.data.api.repositories.ServiceCentreRepository;
import uk.gov.hmcts.reform.fact.data.api.services.OsService;
import uk.gov.hmcts.reform.fact.data.api.services.ServiceAreaService;

import java.util.List;

@Service
@RequiredArgsConstructor
public class SearchServiceCentreService {

    private static final List<CatchmentType> POSTCODE_SEARCH_CATCHMENTS =
        List.of(CatchmentType.LOCAL, CatchmentType.REGIONAL);

    private final OsService osService;
    private final ServiceAreaService serviceAreaService;
    private final ServiceCentreRepository serviceCentreRepository;

    /**
     * Search service centres by postcode, service area and action.
     *
     * @param postcode the postcode to search from
     * @param serviceArea the service area name
     * @param action the search action
     * @param limit maximum number of results
     * @return matching service centres with distance
     */
    public List<ServiceCentreWithDistance> getServiceCentresBySearchParameters(String postcode,
                                                                               String serviceArea,
                                                                               SearchAction action,
                                                                               Integer limit) {
        boolean serviceAreaEmpty = serviceArea == null || serviceArea.isBlank();
        if (action == null ^ serviceAreaEmpty) {
            throw new InvalidParameterCombinationException(
                "Both 'serviceArea' and 'action' must be provided together if one is present."
            );
        }
        if (serviceAreaEmpty) {
            return List.of();
        }
        return searchWithServiceArea(postcode, serviceArea, limit);
    }

    private List<ServiceCentreWithDistance> searchWithServiceArea(String postcode,
                                                                  String serviceArea,
                                                                  Integer limit) {
        OsLocationData osLocationData = osService.getOsLonLatDistrictByPartial(postcode);
        ServiceArea serviceAreaFound = serviceAreaService.getServiceAreaByName(serviceArea);

        return serviceCentreRepository.findNearestByServiceAreaAndAreaOfLawAndCatchmentTypeIn(
            serviceAreaFound.getId(),
            serviceAreaFound.getAreaOfLawId(),
            POSTCODE_SEARCH_CATCHMENTS,
            osLocationData.getLatitude(),
            osLocationData.getLongitude(),
            limit
        );
    }
}
