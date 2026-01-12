package uk.gov.hmcts.reform.fact.data.api.controllers.search;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import uk.gov.hmcts.reform.fact.data.api.entities.Service;
import uk.gov.hmcts.reform.fact.data.api.entities.ServiceArea;
import uk.gov.hmcts.reform.fact.data.api.services.search.SearchService;
import uk.gov.hmcts.reform.fact.data.api.services.ServiceAreaService;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SearchServiceControllerTest {

    @Mock
    private SearchService searchService;

    @Mock
    private ServiceAreaService serviceAreaService;

    @InjectMocks
    private SearchServiceController controller;

    @Test
    void getServicesShouldReturnOk() {
        Service service = new Service();
        service.setName("Money Claims");
        List<Service> services = List.of(service);

        when(searchService.getAllServices()).thenReturn(services);

        ResponseEntity<List<Service>> response = controller.getServices();

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo(services);
        verify(searchService).getAllServices();
    }

    @Test
    void getServiceAreaByNameShouldReturnOk() {
        ServiceArea area = new ServiceArea();
        area.setName("Civil");
        List<ServiceArea> areas = List.of(area);

        when(serviceAreaService.getAllServiceAreasForService("Family"))
            .thenReturn(areas);

        ResponseEntity<List<ServiceArea>> response = controller.getServiceAreaByName("Family");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo(areas);
        verify(serviceAreaService).getAllServiceAreasForService("Family");
    }
}
