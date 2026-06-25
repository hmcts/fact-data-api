package uk.gov.hmcts.reform.fact.data.api.services.search;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.fact.data.api.dto.ServiceAreaSearchResult;
import uk.gov.hmcts.reform.fact.data.api.entities.ServiceArea;
import uk.gov.hmcts.reform.fact.data.api.entities.ServiceCentre;
import uk.gov.hmcts.reform.fact.data.api.entities.types.SearchResultType;
import uk.gov.hmcts.reform.fact.data.api.repositories.ServiceCentreRepository;
import uk.gov.hmcts.reform.fact.data.api.services.ServiceAreaService;

import java.util.List;

@Service
@RequiredArgsConstructor
public class SearchServiceAreaService {

    private final ServiceAreaService serviceAreaService;
    private final ServiceCentreRepository serviceCentreRepository;

    /**
     * Finds service-centre search results by service area name.
     *
     * @param serviceAreaName the service area name
     * @return service centres linked to that service area
     */
    public List<ServiceAreaSearchResult> findByServiceAreaName(String serviceAreaName) {
        ServiceArea serviceArea = serviceAreaService.getServiceAreaByName(serviceAreaName);
        return serviceCentreRepository.findByServiceAreaId(serviceArea.getId()).stream()
            .map(this::toSearchResult)
            .toList();
    }

    private ServiceAreaSearchResult toSearchResult(ServiceCentre serviceCentre) {
        return ServiceAreaSearchResult.builder()
            .id(serviceCentre.getId())
            .serviceCentreId(serviceCentre.getId())
            .serviceCentreName(serviceCentre.getName())
            .serviceCentreSlug(serviceCentre.getSlug())
            .serviceAreaIds(serviceCentre.getServiceAreaIds())
            .catchmentType(serviceCentre.getCatchmentType())
            .type(SearchResultType.SERVICE_CENTRE)
            .build();
    }
}
