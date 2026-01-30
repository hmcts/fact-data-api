package uk.gov.hmcts.reform.fact.data.api.controllers.search;

import io.qameta.allure.Feature;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import uk.gov.hmcts.reform.fact.data.api.os.OsData;
import uk.gov.hmcts.reform.fact.data.api.os.OsDpa;
import uk.gov.hmcts.reform.fact.data.api.os.OsResult;
import uk.gov.hmcts.reform.fact.data.api.services.OsService;

import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Feature("Search Address Controller")
@DisplayName("Search Address Controller")
@WebMvcTest(SearchAddressController.class)
class SearchAddressControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private OsService osService;

    @Test
    @DisplayName("GET /search/address/v1/postcode/{postcode} returns OS address data")
    void getAddressByPostcodeReturnsOk() throws Exception {
        OsData osData = OsData.builder()
            .results(List.of(OsResult.builder()
                .dpa(OsDpa.builder()
                    .lat(51.501)
                    .lng(-0.141)
                    .build())
                .build()))
            .build();

        when(osService.getOsAddressByFullPostcode("SW1A 1AA")).thenReturn(osData);

        mockMvc.perform(get("/search/address/v1/postcode/{postcode}", "SW1A 1AA"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.results[0].DPA.LAT").value(51.501))
            .andExpect(jsonPath("$.results[0].DPA.LNG").value(-0.141));
    }

    @Test
    @DisplayName("GET /search/address/v1/postcode/{postcode} returns 400 for invalid postcode")
    void getAddressByPostcodeReturnsBadRequestForInvalidPostcode() throws Exception {
        mockMvc.perform(get("/search/address/v1/postcode/{postcode}", "SW1A1AA"))
            .andExpect(status().isBadRequest());
    }
}
