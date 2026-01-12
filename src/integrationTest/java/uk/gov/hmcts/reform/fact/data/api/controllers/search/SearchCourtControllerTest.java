package uk.gov.hmcts.reform.fact.data.api.controllers.search;

import io.qameta.allure.Feature;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import uk.gov.hmcts.reform.fact.data.api.dto.CourtWithDistance;
import uk.gov.hmcts.reform.fact.data.api.entities.Court;
import uk.gov.hmcts.reform.fact.data.api.entities.types.SearchAction;
import uk.gov.hmcts.reform.fact.data.api.errorhandling.exceptions.InvalidParameterCombinationException;
import uk.gov.hmcts.reform.fact.data.api.services.CourtService;
import uk.gov.hmcts.reform.fact.data.api.services.search.SearchCourtService;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Feature("Search Court Controller")
@DisplayName("Search Court Controller")
@WebMvcTest(SearchCourtController.class)
class SearchCourtControllerTest {

    private static final UUID COURT_ID = UUID.fromString("123e4567-e89b-12d3-a456-426614174000");

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private SearchCourtService searchCourtService;

    @MockitoBean
    private CourtService courtService;

    @Test
    @DisplayName("GET /search/courts/v1/postcode returns courts by postcode")
    void getCourtsByPostcodeReturnsOk() throws Exception {
        List<CourtWithDistance> results = List.of(
            new CourtWithDistanceDto(COURT_ID, "Test Court", "test-court", BigDecimal.valueOf(1.25))
        );

        when(searchCourtService.getCourtsBySearchParameters("SW1A 1AA", null, null, 10))
            .thenReturn(results);

        mockMvc.perform(get("/search/courts/v1/postcode")
                            .param("postcode", "SW1A 1AA"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].courtId").value(COURT_ID.toString()))
            .andExpect(jsonPath("$[0].courtName").value("Test Court"))
            .andExpect(jsonPath("$[0].distance").value(1.25));

        verify(searchCourtService).getCourtsBySearchParameters("SW1A 1AA", null, null, 10);
    }

    @Test
    @DisplayName("GET /search/courts/v1/postcode accepts lowercase action via converter")
    void getCourtsByPostcodeAcceptsLowercaseAction() throws Exception {
        List<CourtWithDistance> results = List.of(
            new CourtWithDistanceDto(COURT_ID, "Test Court", "test-court", BigDecimal.valueOf(2.5))
        );

        when(searchCourtService.getCourtsBySearchParameters("SW1A 1AA", "Civil", SearchAction.NEAREST, 5))
            .thenReturn(results);

        mockMvc.perform(get("/search/courts/v1/postcode")
                            .param("postcode", "SW1A 1AA")
                            .param("serviceArea", "Civil")
                            .param("action", "nearest")
                            .param("limit", "5"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].courtSlug").value("test-court"))
            .andExpect(jsonPath("$[0].distance").value(2.5));

        verify(searchCourtService).getCourtsBySearchParameters("SW1A 1AA", "Civil", SearchAction.NEAREST, 5);
    }

    @Test
    @DisplayName("GET /search/courts/v1/postcode returns 400 for invalid postcode")
    void getCourtsByPostcodeReturnsBadRequestForInvalidPostcode() throws Exception {
        mockMvc.perform(get("/search/courts/v1/postcode")
                            .param("postcode", "SW1A1AA"))
            .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("GET /search/courts/v1/postcode returns 400 for invalid limit")
    void getCourtsByPostcodeReturnsBadRequestForInvalidLimit() throws Exception {
        mockMvc.perform(get("/search/courts/v1/postcode")
                            .param("postcode", "SW1A 1AA")
                            .param("limit", "0"))
            .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("GET /search/courts/v1/postcode returns 400 for invalid parameter combination")
    void getCourtsByPostcodeReturnsBadRequestForInvalidParameterCombination() throws Exception {
        when(searchCourtService.getCourtsBySearchParameters("SW1A 1AA", "Civil", null, 10))
            .thenThrow(new InvalidParameterCombinationException("Both 'serviceArea' and 'action' must be provided"));

        mockMvc.perform(get("/search/courts/v1/postcode")
                            .param("postcode", "SW1A 1AA")
                            .param("serviceArea", "Civil"))
            .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("GET /search/courts/v1/prefix returns courts by prefix")
    void getCourtsByPrefixReturnsOk() throws Exception {
        Court court = new Court();
        court.setId(COURT_ID);
        court.setName("Alpha Court");

        when(courtService.getCourtsByPrefixAndActiveSearch("A"))
            .thenReturn(List.of(court));

        mockMvc.perform(get("/search/courts/v1/prefix")
                            .param("prefix", "A"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].name").value("Alpha Court"));
    }

    @Test
    @DisplayName("GET /search/courts/v1/prefix returns 400 for invalid prefix")
    void getCourtsByPrefixReturnsBadRequestForInvalidPrefix() throws Exception {
        mockMvc.perform(get("/search/courts/v1/prefix")
                            .param("prefix", "AB"))
            .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("GET /search/courts/v1/name returns courts by query")
    void getCourtsByQueryReturnsOk() throws Exception {
        Court court = new Court();
        court.setId(COURT_ID);
        court.setName("Example Court");

        when(courtService.searchOpenCourtsByNameOrAddress("Example"))
            .thenReturn(List.of(court));

        mockMvc.perform(get("/search/courts/v1/name")
                            .param("q", "Example"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].name").value("Example Court"));
    }

    @Test
    @DisplayName("GET /search/courts/v1/name returns 400 for query too short")
    void getCourtsByQueryReturnsBadRequestForShortQuery() throws Exception {
        mockMvc.perform(get("/search/courts/v1/name")
                            .param("q", "ab"))
            .andExpect(status().isBadRequest());
    }

    private static final class CourtWithDistanceDto implements CourtWithDistance {
        private final UUID courtId;
        private final String courtName;
        private final String courtSlug;
        private final BigDecimal distance;

        private CourtWithDistanceDto(UUID courtId, String courtName, String courtSlug, BigDecimal distance) {
            this.courtId = courtId;
            this.courtName = courtName;
            this.courtSlug = courtSlug;
            this.distance = distance;
        }

        @Override
        public UUID getCourtId() {
            return courtId;
        }

        @Override
        public String getCourtName() {
            return courtName;
        }

        @Override
        public String getCourtSlug() {
            return courtSlug;
        }

        @Override
        public BigDecimal getDistance() {
            return distance;
        }
    }
}
