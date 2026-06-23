package uk.gov.hmcts.reform.fact.data.api.controllers.search;

import io.qameta.allure.Feature;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import uk.gov.hmcts.reform.fact.data.api.dto.SearchResult;
import uk.gov.hmcts.reform.fact.data.api.entities.types.SearchAction;
import uk.gov.hmcts.reform.fact.data.api.entities.types.SearchResultType;
import uk.gov.hmcts.reform.fact.data.api.services.search.SearchLocationService;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Feature("Search Location Controller")
@DisplayName("Search Location Controller")
@WebMvcTest(SearchLocationController.class)
@AutoConfigureMockMvc(addFilters = false)
class SearchLocationControllerTest {

    private static final UUID RESULT_ID = UUID.fromString("123e4567-e89b-12d3-a456-426614174000");

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private SearchLocationService searchLocationService;

    @Test
    @DisplayName("GET /search/locations/v1/postcode returns typed search results")
    void getLocationsByPostcodeReturnsTypedResults() throws Exception {
        List<SearchResult> results = List.of(
            SearchResult.builder()
                .id(RESULT_ID)
                .name("National Business Centre")
                .slug("national-business-centre")
                .distance(BigDecimal.valueOf(1.5))
                .type(SearchResultType.SERVICE_CENTRE)
                .build()
        );

        when(searchLocationService.getLocationsBySearchParameters(
            "SW1A 1AA",
            "Money Claims",
            SearchAction.DOCUMENTS,
            10
        )).thenReturn(results);

        mockMvc.perform(get("/search/locations/v1/postcode")
                            .param("postcode", "SW1A 1AA")
                            .param("serviceArea", "Money Claims")
                            .param("action", "documents"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].id").value(RESULT_ID.toString()))
            .andExpect(jsonPath("$[0].name").value("National Business Centre"))
            .andExpect(jsonPath("$[0].slug").value("national-business-centre"))
            .andExpect(jsonPath("$[0].type").value("SERVICE_CENTRE"))
            .andExpect(jsonPath("$[0].distance").value(1.5));

        verify(searchLocationService).getLocationsBySearchParameters(
            "SW1A 1AA",
            "Money Claims",
            SearchAction.DOCUMENTS,
            10
        );
    }
}
