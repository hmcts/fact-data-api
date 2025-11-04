package uk.gov.hmcts.reform.fact.functional.helpers;

import io.restassured.response.Response;
import uk.gov.hmcts.reform.fact.functional.http.HttpClient;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.HttpStatus.OK;

/**
 * Helper methods for assertions in functional tests.
 */
public final class AssertionHelper {

    private AssertionHelper() {
    }

    /**
     * Performs a GET request to the specified endpoint and validates the response.
     * Asserts:
     * - Response status is 200 OK
     * - Content type is JSON
     * - Response body is a non-null JSON array
     *
     * @param http the HTTP client
     * @param endpoint the endpoint path to test
     */
    public static void assertSuccessfulJsonArrayResponse(final HttpClient http, final String endpoint) {
        final Response response = http.doGet(endpoint);
        assertThat(response.statusCode()).isEqualTo(OK.value());
        assertThat(response.contentType()).contains("json");
        assertThat(response.jsonPath().getList("$")).isNotNull();
    }
}
