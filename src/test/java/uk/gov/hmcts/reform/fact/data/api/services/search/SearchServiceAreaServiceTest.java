package uk.gov.hmcts.reform.fact.data.api.services.search;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.fact.data.api.dto.ServiceAreaSearchResult;
import uk.gov.hmcts.reform.fact.data.api.entities.ServiceArea;
import uk.gov.hmcts.reform.fact.data.api.entities.ServiceCentre;
import uk.gov.hmcts.reform.fact.data.api.entities.types.CatchmentType;
import uk.gov.hmcts.reform.fact.data.api.entities.types.SearchResultType;
import uk.gov.hmcts.reform.fact.data.api.repositories.ServiceCentreRepository;
import uk.gov.hmcts.reform.fact.data.api.services.ServiceAreaService;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SearchServiceAreaServiceTest {

    @Mock
    private ServiceAreaService serviceAreaService;

    @Mock
    private ServiceCentreRepository serviceCentreRepository;

    @InjectMocks
    private SearchServiceAreaService searchServiceAreaService;

    @Test
    void findByServiceAreaNameReturnsServiceCentreResults() {
        UUID serviceAreaId = UUID.randomUUID();
        UUID serviceCentreId = UUID.randomUUID();
        ServiceArea serviceArea = new ServiceArea();
        serviceArea.setId(serviceAreaId);
        ServiceCentre serviceCentre = ServiceCentre.builder()
            .id(serviceCentreId)
            .name("National Business Centre")
            .slug("national-business-centre")
            .serviceAreaIds(List.of(serviceAreaId))
            .catchmentType(CatchmentType.NATIONAL)
            .build();

        when(serviceAreaService.getServiceAreaByName("Money Claims")).thenReturn(serviceArea);
        when(serviceCentreRepository.findByServiceAreaId(serviceAreaId)).thenReturn(List.of(serviceCentre));

        List<ServiceAreaSearchResult> results = searchServiceAreaService.findByServiceAreaName("Money Claims");

        assertThat(results).hasSize(1);
        assertThat(results.getFirst().getId()).isEqualTo(serviceCentreId);
        assertThat(results.getFirst().getServiceCentreName()).isEqualTo("National Business Centre");
        assertThat(results.getFirst().getCatchmentType()).isEqualTo(CatchmentType.NATIONAL);
        assertThat(results.getFirst().getType()).isEqualTo(SearchResultType.SERVICE_CENTRE);
    }
}
