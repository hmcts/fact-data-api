package uk.gov.hmcts.reform.fact.data.api.controllers.search;

import io.qameta.allure.Feature;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import uk.gov.hmcts.reform.fact.data.api.dto.ServiceAreaSearchResult;
import uk.gov.hmcts.reform.fact.data.api.entities.types.CatchmentType;
import uk.gov.hmcts.reform.fact.data.api.entities.types.SearchResultType;
import uk.gov.hmcts.reform.fact.data.api.errorhandling.exceptions.NotFoundException;
import uk.gov.hmcts.reform.fact.data.api.services.search.SearchServiceAreaService;

import java.util.List;
import java.util.UUID;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Feature("Search Service Area Controller")
@DisplayName("Search Service Area Controller")
@WebMvcTest(SearchServiceAreaController.class)
@AutoConfigureMockMvc(addFilters = false)
class SearchServiceAreaControllerTest {

    private static final UUID SERVICE_CENTRE_ID = UUID.fromString("123e4567-e89b-12d3-a456-426614174000");
    private static final UUID SERVICE_AREA_ID = UUID.fromString("123e4567-e89b-12d3-a456-426614174111");

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private SearchServiceAreaService searchServiceAreaService;

    @Test
    @DisplayName("GET /search/service-area/v1/{serviceAreaName} returns service-centre results")
    void getServiceAreaByNameReturnsServiceCentreResults() throws Exception {
        when(searchServiceAreaService.findByServiceAreaName("Money Claims"))
            .thenReturn(List.of(ServiceAreaSearchResult.builder()
                                    .id(SERVICE_CENTRE_ID)
                                    .serviceCentreId(SERVICE_CENTRE_ID)
                                    .serviceCentreName("National Business Centre")
                                    .serviceCentreSlug("national-business-centre")
                                    .serviceAreaIds(List.of(SERVICE_AREA_ID))
                                    .catchmentType(CatchmentType.NATIONAL)
                                    .type(SearchResultType.SERVICE_CENTRE)
                                    .build()));

        mockMvc.perform(get("/search/service-area/v1/{serviceAreaName}", "Money Claims"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].id").value(SERVICE_CENTRE_ID.toString()))
            .andExpect(jsonPath("$[0].serviceCentreId").value(SERVICE_CENTRE_ID.toString()))
            .andExpect(jsonPath("$[0].serviceCentreName").value("National Business Centre"))
            .andExpect(jsonPath("$[0].serviceCentreSlug").value("national-business-centre"))
            .andExpect(jsonPath("$[0].serviceAreaIds[0]").value(SERVICE_AREA_ID.toString()))
            .andExpect(jsonPath("$[0].catchmentType").value("NATIONAL"))
            .andExpect(jsonPath("$[0].type").value("SERVICE_CENTRE"));
    }

    @Test
    @DisplayName("GET /search/service-area/v1/{serviceAreaName} returns 404 when missing")
    void getServiceAreaByNameReturnsNotFound() throws Exception {
        when(searchServiceAreaService.findByServiceAreaName("Missing"))
            .thenThrow(new NotFoundException("Service area Missing not found"));

        mockMvc.perform(get("/search/service-area/v1/{serviceAreaName}", "Missing"))
            .andExpect(status().isNotFound());
    }
}
