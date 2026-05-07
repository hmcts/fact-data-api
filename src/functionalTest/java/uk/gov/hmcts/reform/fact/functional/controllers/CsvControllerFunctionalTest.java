package uk.gov.hmcts.reform.fact.functional.controllers;

import io.qameta.allure.Feature;
import io.restassured.response.Response;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import uk.gov.hmcts.reform.fact.functional.http.HttpClient;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.HttpStatus.UNAUTHORIZED;

@Feature("CSV Controller")
@DisplayName("CSV Controller")
class CsvControllerFunctionalTest {

    private static final String CSV_ENDPOINT = "/csv/";
    private static final String INVALID_TOKEN = "invalid-token";
    private static final HttpClient http = new HttpClient();

    @Test
    @DisplayName("POST /csv/ without bearer token returns 401")
    void shouldReturnUnauthorizedWithoutToken() {
        final Response response = http.doPost(CSV_ENDPOINT, null, "");

        assertThat(response.statusCode()).isEqualTo(UNAUTHORIZED.value());
    }

    @Test
    @DisplayName("POST /csv/ with invalid bearer token returns 401")
    void shouldReturnUnauthorizedWithInvalidToken() {
        final Response response = http.doPost(CSV_ENDPOINT, null, INVALID_TOKEN);

        assertThat(response.statusCode()).isEqualTo(UNAUTHORIZED.value());
    }
}
