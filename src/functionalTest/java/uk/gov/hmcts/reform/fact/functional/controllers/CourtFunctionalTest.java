package uk.gov.hmcts.reform.fact.functional.controllers;

import io.restassured.response.Response;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import uk.gov.hmcts.reform.fact.functional.config.TestConfig;
import uk.gov.hmcts.reform.fact.functional.data.CourtTestData;
import uk.gov.hmcts.reform.fact.functional.data.CourtTestDataBuilder;
import uk.gov.hmcts.reform.fact.functional.helpers.DatabaseHelper;
import uk.gov.hmcts.reform.fact.functional.helpers.TestDataHelper;
import uk.gov.hmcts.reform.fact.functional.http.HttpClient;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.HttpStatus.CREATED;
import static org.springframework.http.HttpStatus.OK;

/**
 * Functional tests for Court Controller endpoints.
 */
public final class CourtFunctionalTest {

    private static HttpClient http;
    private static String regionId;

    @BeforeAll
    static void setUp() {
        final var config = TestConfig.load();
        http = new HttpClient(config);
        regionId = DatabaseHelper.getAnyRegionId(config);
    }

    @Test
    @DisplayName("POST /courts/v1 creates court with valid payload")
    void shouldCreateCourtWithValidPayload() {
        final CourtTestData court = CourtTestDataBuilder.validCourt()
            .withRegionId(regionId)
            .withName("Functional Test Court")
            .build();

        final Response response = http.doPost("/courts/v1", court);

        assertThat(response.statusCode()).isEqualTo(CREATED.value());
        assertThat(response.jsonPath().getString("id")).isNotNull();
        assertThat(response.jsonPath().getString("name")).isEqualTo("Functional Test Court");
        assertThat(response.jsonPath().getString("regionId")).isEqualTo(regionId);
        assertThat(response.jsonPath().getBoolean("isServiceCentre")).isTrue();
    }

    @Test
    @DisplayName("GET /courts/{courtId}/v1 returns court details")
    void shouldGetCourtById() {
        final CourtTestData court = CourtTestDataBuilder.validCourt()
            .withRegionId(regionId)
            .withName("Court for GET test")
            .build();

        final String courtId = TestDataHelper.createCourt(http, court);
        final Response response = http.doGet("/courts/" + courtId + "/v1");

        assertThat(response.statusCode()).isEqualTo(OK.value());
        assertThat(response.jsonPath().getString("id")).isEqualTo(courtId);
        assertThat(response.jsonPath().getString("name")).isEqualTo("Court for GET test");
        assertThat(response.jsonPath().getString("regionId")).isEqualTo(regionId);
        assertThat(response.jsonPath().getBoolean("isServiceCentre")).isTrue();
    }
}
