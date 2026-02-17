package uk.gov.hmcts.reform.fact.data.api.controllers.search;

import io.qameta.allure.Feature;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import uk.gov.hmcts.reform.fact.data.api.entities.Service;
import uk.gov.hmcts.reform.fact.data.api.entities.ServiceArea;
import uk.gov.hmcts.reform.fact.data.api.errorhandling.exceptions.NotFoundException;
import uk.gov.hmcts.reform.fact.data.api.repositories.ServiceRepository;
import uk.gov.hmcts.reform.fact.data.api.services.ServiceAreaService;

import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Feature("Search Service Controller")
@DisplayName("Search Service Controller")
@WebMvcTest(SearchServiceController.class)
@AutoConfigureMockMvc(addFilters = false)
class SearchServiceControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ServiceRepository serviceRepository;

    @MockitoBean
    private ServiceAreaService serviceAreaService;

    @Test
    @DisplayName("GET /search/services/v1 returns services")
    void getServicesReturnsOk() throws Exception {
        Service service = new Service();
        service.setName("Civil");
        List<Service> services = List.of(service);

        when(serviceRepository.findAll()).thenReturn(services);

        mockMvc.perform(get("/search/services/v1"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].name").value("Civil"));
    }

    @Test
    @DisplayName("GET /search/services/v1/{serviceName}/service-areas returns service areas")
    void getServiceAreasReturnsOk() throws Exception {
        ServiceArea area = new ServiceArea();
        area.setName("Family");
        List<ServiceArea> areas = List.of(area);

        when(serviceAreaService.getAllServiceAreasForService("Family"))
            .thenReturn(areas);

        mockMvc.perform(get("/search/services/v1/{serviceName}/service-areas", "Family"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].name").value("Family"));
    }

    @Test
    @DisplayName("GET /search/services/v1/{serviceName}/service-areas returns 404 when missing")
    void getServiceAreasReturnsNotFound() throws Exception {
        when(serviceAreaService.getAllServiceAreasForService("Family"))
            .thenThrow(new NotFoundException("No service areas found for service Family"));

        mockMvc.perform(get("/search/services/v1/{serviceName}/service-areas", "Family"))
            .andExpect(status().isNotFound());
    }
}
