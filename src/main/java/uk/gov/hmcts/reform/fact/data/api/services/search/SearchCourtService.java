package uk.gov.hmcts.reform.fact.data.api.services.search;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.fact.data.api.dto.CourtWithDistance;
import uk.gov.hmcts.reform.fact.data.api.entities.ServiceArea;
import uk.gov.hmcts.reform.fact.data.api.entities.types.SearchAction;
import uk.gov.hmcts.reform.fact.data.api.entities.types.SearchStrategy;
import uk.gov.hmcts.reform.fact.data.api.errorhandling.exceptions.InvalidParameterCombinationException;
import uk.gov.hmcts.reform.fact.data.api.os.OsDpa;
import uk.gov.hmcts.reform.fact.data.api.os.OsLocationData;
import uk.gov.hmcts.reform.fact.data.api.services.CourtAddressService;
import uk.gov.hmcts.reform.fact.data.api.services.CourtServiceAreaService;
import uk.gov.hmcts.reform.fact.data.api.services.CourtSinglePointOfEntryService;
import uk.gov.hmcts.reform.fact.data.api.services.OsService;
import uk.gov.hmcts.reform.fact.data.api.services.ServiceAreaService;

import java.util.List;

import static uk.gov.hmcts.reform.fact.data.api.entities.types.CatchmentMethod.LOCAL_AUTHORITY;
import static uk.gov.hmcts.reform.fact.data.api.entities.types.CatchmentType.REGIONAL;
import static uk.gov.hmcts.reform.fact.data.api.entities.types.SearchAction.NEAREST;
import static uk.gov.hmcts.reform.fact.data.api.entities.types.SearchStrategy.CIVIL_POSTCODE_PREFERENCE;
import static uk.gov.hmcts.reform.fact.data.api.entities.types.SearchStrategy.DEFAULT_AOL_DISTANCE;
import static uk.gov.hmcts.reform.fact.data.api.entities.types.SearchStrategy.FAMILY_NON_REGIONAL;
import static uk.gov.hmcts.reform.fact.data.api.entities.types.SearchStrategy.FAMILY_REGIONAL;

@Service
@Slf4j
public class SearchCourtService {

    private final OsService osService;
    private final ServiceAreaService serviceAreaService;
    private final CourtSinglePointOfEntryService courtSinglePointOfEntryService;
    private final CourtServiceAreaService courtServiceAreaService;
    private final CourtAddressService courtAddressService;
    private final SearchExecuter searchExecuter;
    private static final String CHILDCARE_SERVICE_AREA = "Childcare arrangements if you separate from your partner";
    private static final String CHILDCARE_AOL = "Children";

    public SearchCourtService(OsService osService,
                              ServiceAreaService serviceAreaService,
                              CourtSinglePointOfEntryService courtSinglePointOfEntryService,
                              CourtServiceAreaService courtServiceAreaService,
                              CourtAddressService courtAddressService,
                              SearchExecuter searchExecuter) {
        this.osService = osService;
        this.serviceAreaService = serviceAreaService;
        this.courtSinglePointOfEntryService = courtSinglePointOfEntryService;
        this.courtServiceAreaService = courtServiceAreaService;
        this.courtAddressService = courtAddressService;
        this.searchExecuter = searchExecuter;
    }

    /**
     * Determine whether we do a search by postcode itself
     * or with the service area and action included.
     * Based on the result, filter based on
     *
     * @param postcode The postcode provided
     * @param serviceArea The service area provided, for example money claims
     * @param action The action, for example documents
     * @param limit The amount of rows to return
     * @return A list of CourtWithDistance objects
     */
    public List<CourtWithDistance> getCourtsBySearchParameters(String postcode, String serviceArea,
                                                       SearchAction action, Integer limit) {
        boolean serviceAreaEmpty = serviceArea == null || serviceArea.isBlank();
        if (action == null ^ serviceAreaEmpty) {
            throw new InvalidParameterCombinationException(
                "Both 'serviceArea' and 'action' must be provided together if one is present."
            );
        }
        return serviceAreaEmpty
            ? searchPostcodeOnly(postcode, limit)
            : searchWithServiceArea(postcode, serviceArea, action, limit);
    }

    /**
     * Searches for the nearest courts using a postcode only.
     *
     * @param postcode the postcode to search by
     * @param limit the maximum number of results
     * @return the nearest courts with distance data
     */
    public List<CourtWithDistance> searchPostcodeOnly(String postcode, Integer limit) {
        OsDpa osData = osService.getOsAddressByFullPostcode(postcode).getResults().getFirst().getDpa();
        return courtAddressService.findCourtWithDistanceByOsData(osData.getLat(), osData.getLng(), limit);
    }

    /**
     * Where we are not searching simply by nearest based on postcodes.
     * This will be where we have both a Service Area and Search Action provided.
     * The SearchStrategy will be found and executed according to the logic of business rules.
     *
     * @param postcode The postcode provided by the user
     * @param serviceArea The service area, for example money claims
     * @param action The action, for example documents
     * @param limit The amount of rows returned
     * @return A list of CourtWithDistances
     */
    public List<CourtWithDistance> searchWithServiceArea(String postcode, String serviceArea,
                                                         SearchAction action, Integer limit) {
        OsLocationData osLocationData = osService.getOsLonLatDistrictByPartial(postcode);
        ServiceArea serviceAreaFound = serviceAreaService.getServiceAreaByName(serviceArea);

        if (serviceArea.equalsIgnoreCase(CHILDCARE_SERVICE_AREA)) {
            return courtSinglePointOfEntryService
                .getCourtsSpoe(osLocationData.getLatitude(), osLocationData.getLongitude(), CHILDCARE_AOL);
        }

        return searchExecuter.executeSearchStrategy(
            osLocationData,
            serviceAreaFound,
            selectSearchStrategy(
                action,
                osLocationData.getAuthorityName(),
                serviceAreaFound
            ),
            action,
            limit
        );
    }

    /**
     * Determine the search strategy to be used.
     *
     * @param action        the action.
     * @param authorityName the authority name derived from OS lookup.
     * @param serviceArea   the service area sent from the frontend.
     * @return a search strategy used to return one or more courts.
     */
    public SearchStrategy selectSearchStrategy(SearchAction action,
                                               String authorityName,
                                               ServiceArea serviceArea) {
        if (action == NEAREST) {
            // Note that DOCUMENTS and UPDATE don't affect business rules here
            // they are only used for sorting logic
            return DEFAULT_AOL_DISTANCE;
        }

        return switch (serviceArea.getType()) {
            case CIVIL -> CIVIL_POSTCODE_PREFERENCE;
            case FAMILY -> getFamilyStrategy(action, serviceArea, authorityName);
            case OTHER -> DEFAULT_AOL_DISTANCE;
        };
    }

    /**
     * Returns either regional or non-regional family search strategy.
     *
     * @param action the action
     * @param serviceArea the service area
     * @param authorityName the authority name
     * @return The search strategy for family jurisdictions
     */
    private SearchStrategy getFamilyStrategy(SearchAction action, ServiceArea serviceArea, String authorityName) {
        if (LOCAL_AUTHORITY.equals(serviceArea.getCatchmentMethod())
                && !authorityName.isEmpty()) {
            return courtServiceAreaService.findByServiceAreaId(serviceArea.getId())
                    .stream()
                    .anyMatch(courtService -> courtService.getCatchmentType().equals(REGIONAL))
                    ? FAMILY_REGIONAL : FAMILY_NON_REGIONAL;
        }
        log.debug("Setting search strategy to default for {}, {}, {}",
                action, serviceArea, authorityName);
        return FAMILY_NON_REGIONAL;
    }
}
