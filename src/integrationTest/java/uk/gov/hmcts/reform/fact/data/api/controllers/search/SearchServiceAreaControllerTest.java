package uk.gov.hmcts.reform.fact.data.api.controllers.search;

import io.qameta.allure.Feature;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import uk.gov.hmcts.reform.fact.data.api.entities.CourtServiceAreas;
import uk.gov.hmcts.reform.fact.data.api.entities.types.CatchmentType;
import uk.gov.hmcts.reform.fact.data.api.errorhandling.exceptions.NotFoundException;
import uk.gov.hmcts.reform.fact.data.api.services.CourtServiceAreaService;

import java.util.List;
import java.util.UUID;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Feature("Search Service Area Controller")
@DisplayName("Search Service Area Controller")
@WebMvcTest(SearchServiceAreaController.class)
class SearchServiceAreaControllerTest {

    private static final UUID COURT_ID = UUID.fromString("123e4567-e89b-12d3-a456-426614174000");
    private static final UUID SERVICE_AREA_ID = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CourtServiceAreaService courtServiceAreaService;

    @Test
    @DisplayName("GET /search/service-area/v1/{serviceAreaName} returns court service areas")
    void getServiceAreaReturnsOk() throws Exception {
        CourtServiceAreas area = new CourtServiceAreas();
        area.setCourtId(COURT_ID);
        area.setServiceAreaId(List.of(SERVICE_AREA_ID));
        area.setCatchmentType(CatchmentType.REGIONAL);

        when(courtServiceAreaService.findByServiceAreaName("Family"))
            .thenReturn(List.of(area));

        mockMvc.perform(get("/search/service-area/v1/{serviceAreaName}", "Family"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].courtId").value(COURT_ID.toString()))
            .andExpect(jsonPath("$[0].catchmentType").value("REGIONAL"));
    }

    @Test
    @DisplayName("GET /search/service-area/v1/{serviceAreaName} returns 404 when missing")
    void getServiceAreaReturnsNotFound() throws Exception {
        when(courtServiceAreaService.findByServiceAreaName("Family"))
            .thenThrow(new NotFoundException("Service area Family not found"));

        mockMvc.perform(get("/search/service-area/v1/{serviceAreaName}", "Family"))
            .andExpect(status().isNotFound());
    }
}
