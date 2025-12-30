package uk.gov.hmcts.reform.fact.data.api.services;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.fact.data.api.dto.CourtWithDistance;
import uk.gov.hmcts.reform.fact.data.api.entities.ServiceArea;
import uk.gov.hmcts.reform.fact.data.api.entities.types.SearchAction;
import uk.gov.hmcts.reform.fact.data.api.entities.types.SearchStrategy;
import uk.gov.hmcts.reform.fact.data.api.errorhandling.exceptions.NotFoundException;
import uk.gov.hmcts.reform.fact.data.api.os.OsDpa;
import uk.gov.hmcts.reform.fact.data.api.repositories.CourtAddressRepository;
import uk.gov.hmcts.reform.fact.data.api.repositories.ServiceAreaRepository;

import java.text.MessageFormat;
import java.util.List;

import static uk.gov.hmcts.reform.fact.data.api.entities.types.SearchAction.NEAREST;
import static uk.gov.hmcts.reform.fact.data.api.entities.types.SearchStrategy.DEFAULT_AOL_DISTANCE;

@Service
@Slf4j
public class SearchCourtService {

    private final OsService osService;
    private final ServiceAreaService serviceAreaService;
    private final CourtAddressRepository courtAddressRepository;

    public SearchCourtService(OsService osService,
                              ServiceAreaService serviceAreaService,
                              CourtAddressRepository courtAddressRepository) {
        this.osService = osService;
        this.serviceAreaService = serviceAreaService;
        this.courtAddressRepository = courtAddressRepository;
    }

    public List<CourtWithDistance> searchPostcodeOnly(String postcode, Integer limit) {
        log.info("searchPostcodeOnly");
        OsDpa osData = osService.getOsAddressByFullPostcode(postcode).getResults().getFirst().getDpa();
        return courtAddressRepository.findNearestCourts(osData.getLat(), osData.getLng(), limit);
    }

    public List<CourtWithDistance> searchWithServiceArea(String postcode, String serviceArea,
                                                         SearchAction action, Integer limit) {
        log.info("searchWithServiceArea");
        OsDpa osData = osService.getOsAddressByFullPostcode(postcode).getResults().getFirst().getDpa();
        ServiceArea serviceAreaFound = serviceAreaService.getServiceAreaByName(serviceArea);

        SearchStrategy searchStrategy = selectSearchStrategy(action);

        log.info(String.valueOf(serviceAreaFound));

        return null;
    }

    public SearchStrategy selectSearchStrategy(SearchAction action) {
        if (action.equals(NEAREST)) {
            log.info("nearest search strategy");
            return DEFAULT_AOL_DISTANCE;
        }

        log.info("default AOL_DISTANCE");
        return DEFAULT_AOL_DISTANCE;
    }
}
