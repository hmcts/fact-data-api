package uk.gov.hmcts.reform.fact.functional.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.restassured.response.Response;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import uk.gov.hmcts.reform.fact.functional.config.TestConfig;
import uk.gov.hmcts.reform.fact.functional.data.CourtTestData;
import uk.gov.hmcts.reform.fact.functional.http.HttpClient;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.HttpStatus.CREATED;

public final class CourtControllerFunctionalTest {

    private static HttpClient http;
    private static ObjectMapper mapper;
    private static String regionId;

    @BeforeAll
    static void setUp() {
        final TestConfig config = TestConfig.load();
        http = new HttpClient(config);
        mapper = new ObjectMapper();
        regionId = getRegionId();
    }

    private static String getRegionId() {
        final Response response = http.doGet("/types/v1/regions");
        return response.jsonPath().getString("[0].id");
    }

    @Test
    @DisplayName("POST /courts/v1 creates court with valid payload")
    void shouldCreateCourtWithValidPayload() throws Exception {
        final CourtTestData court = new CourtTestData();
        court.setName("Functional Test Court I");
        court.setRegionId(regionId);
        court.setIsServiceCentre(true);

        final String json = mapper.writeValueAsString(court);
        final Response response = http.doPost("/courts/v1", json);

        assertThat(response.statusCode()).isEqualTo(CREATED.value());
        assertThat(response.jsonPath().getString("id")).isNotNull();
    }
}
