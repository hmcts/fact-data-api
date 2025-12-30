package uk.gov.hmcts.reform.fact.data.api.services;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.fact.data.api.dto.CourtWithDistance;
import uk.gov.hmcts.reform.fact.data.api.entities.ServiceArea;
import uk.gov.hmcts.reform.fact.data.api.entities.types.SearchAction;
import uk.gov.hmcts.reform.fact.data.api.entities.types.SearchStrategy;
import uk.gov.hmcts.reform.fact.data.api.os.OsDpa;
import uk.gov.hmcts.reform.fact.data.api.os.OsLocationData;
import uk.gov.hmcts.reform.fact.data.api.repositories.CourtAddressRepository;
import uk.gov.hmcts.reform.fact.data.api.repositories.CourtServiceAreasRepository;
import uk.gov.hmcts.reform.fact.data.api.repositories.CourtSinglePointsOfEntryRepository;

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
    private final CourtAddressRepository courtAddressRepository;
    private final CourtServiceAreasRepository courtServiceAreasRepository;
    private final CourtSinglePointsOfEntryRepository courtSinglePointsOfEntryRepository;
    private static final String CHILDCARE_SERVICE_AREA = "Childcare arrangements if you separate from your partner";

    public SearchCourtService(OsService osService,
                              ServiceAreaService serviceAreaService,
                              CourtAddressRepository courtAddressRepository,
                              CourtServiceAreasRepository courtServiceAreasRepository,
                              CourtSinglePointsOfEntryRepository courtSinglePointsOfEntryRepository) {
        this.osService = osService;
        this.serviceAreaService = serviceAreaService;
        this.courtAddressRepository = courtAddressRepository;
        this.courtServiceAreasRepository = courtServiceAreasRepository;
        this.courtSinglePointsOfEntryRepository = courtSinglePointsOfEntryRepository;
    }

    public List<CourtWithDistance> searchPostcodeOnly(String postcode, Integer limit) {
        log.info("searchPostcodeOnly");
        OsDpa osData = osService.getOsAddressByFullPostcode(postcode).getResults().getFirst().getDpa();
        return courtAddressRepository.findNearestCourts(osData.getLat(), osData.getLng(), limit);
    }

    public List<CourtWithDistance> searchWithServiceArea(String postcode, String serviceArea,
                                                         SearchAction action, Integer limit) {
        log.info("searchWithServiceArea");
        log.info("postcode: {}", postcode);
        log.info("serviceArea: {}", serviceArea);
        log.info("action: {}", action);

        OsLocationData osLocationData = osService.getOsLonLatDistrictByPartial(postcode);
        ServiceArea serviceAreaFound = serviceAreaService.getServiceAreaByName(serviceArea);

        log.info("serviceAreaFound: {}", serviceAreaFound);

        if (serviceArea.equalsIgnoreCase(CHILDCARE_SERVICE_AREA)) {
            log.info("Found childcare service");
            return getChildcareCourtsSpoe(osLocationData.getLatitude(), osLocationData.getLongitude());
        }

        SearchStrategy searchStrategy = selectSearchStrategy(
            action,
            osLocationData.getAuthorityName(),
            serviceAreaFound
        );

        log.info(String.valueOf(searchStrategy));

        return null;
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

        switch (serviceArea.getType()) {
            case CIVIL -> {
                return CIVIL_POSTCODE_PREFERENCE;
            }
            case FAMILY -> {
                if (serviceArea.getCatchmentMethod().equals(LOCAL_AUTHORITY)
                    && !authorityName.isEmpty()) {
                    return courtServiceAreasRepository.findByServiceAreaId(serviceArea.getId())
                        .stream()
                        .anyMatch(courtService -> courtService.getCatchmentType().equals(REGIONAL))
                        ? FAMILY_REGIONAL : FAMILY_NON_REGIONAL;
                }
            }
        }
        return DEFAULT_AOL_DISTANCE;
    }

    public List<CourtWithDistance> getChildcareCourtsSpoe(double latitude, double longitude) {
        return courtSinglePointsOfEntryRepository.findNearestCourtBySpoeAndChildrenAreaOfLaw(latitude, longitude);
    }
}
