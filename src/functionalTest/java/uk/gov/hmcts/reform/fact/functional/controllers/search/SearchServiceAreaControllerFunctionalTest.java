package uk.gov.hmcts.reform.fact.functional.controllers.search;

import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.cfg.DateTimeFeature;
import tools.jackson.databind.json.JsonMapper;
import io.qameta.allure.Feature;
import io.restassured.response.Response;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import uk.gov.hmcts.reform.fact.data.api.entities.Court;
import uk.gov.hmcts.reform.fact.functional.http.HttpClient;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.HttpStatus.NOT_FOUND;
import static org.springframework.http.HttpStatus.OK;

@Feature("Search Service Area Controller")
@DisplayName("Search Service Area Controller")
public final class SearchServiceAreaControllerFunctionalTest {

    private static final HttpClient http = new HttpClient();
    private static final ObjectMapper mapper = JsonMapper.builder()
        .disable(DateTimeFeature.WRITE_DATES_AS_TIMESTAMPS)
        .build();

    /**
     * This test asserts for an empty list at the moment because there is currently no API endpoint
     * to associate courts with service areas. The only way to create this link is by
     * directly updating the database manually or through a migration script, which is
     * not recommended for functional tests. Once an endpoint to link courts to service
     * areas is available, this test should be updated to create the association and
     * assert that the court is returned.
     */
    @Test
    @DisplayName("GET /search/service-area/v1/{serviceAreaName} returns empty list when no courts are linked")
    void shouldReturnEmptyListForServiceAreaWithNoCourts() {
        final String serviceAreaName = "Tax";
        final Response response = http.doGet("/search/service-area/v1/" + serviceAreaName);

        assertThat(response.statusCode())
            .as("Expected 200 OK for valid service area name '%s'", serviceAreaName)
            .isEqualTo(OK.value());

        final List<Court> courts = mapper.readValue(
            response.getBody().asString(),
            new TypeReference<List<Court>>() {}
        );

        assertThat(courts)
            .as("Expected empty list as no courts are currently linked to service area '%s'",
                serviceAreaName)
            .isEmpty();
    }

    @Test
    @DisplayName("GET /search/service-area/v1/{serviceAreaName} returns 404 for non-existent service area")
    void shouldReturn404ForNonExistentServiceArea() {
        final String nonExistentServiceArea = "Non Existent Service Area";
        final Response response = http.doGet("/search/service-area/v1/" + nonExistentServiceArea);

        assertThat(response.statusCode())
            .as("Expected 404 Not Found for non-existent service area '%s'", nonExistentServiceArea)
            .isEqualTo(NOT_FOUND.value());
    }
}
