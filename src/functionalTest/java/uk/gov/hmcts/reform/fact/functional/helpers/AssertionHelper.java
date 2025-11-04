package uk.gov.hmcts.reform.fact.functional.helpers;

import io.restassured.response.Response;
import org.springframework.http.HttpStatus;
import uk.gov.hmcts.reform.fact.functional.http.HttpClient;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Helper methods for assertions in functional tests.
 */
public final class AssertionHelper {

    private AssertionHelper() {
    }

    /**
     * Performs a GET request to the specified endpoint and validates the response.
     *
     * @param http the HTTP client
     * @param endpoint the endpoint path to test
     * @param expectedStatus the expected HTTP status code
     */
    public static void assertJsonArrayResponse(final HttpClient http,
                                                final String endpoint,
                                                final HttpStatus expectedStatus) {
        final Response response = http.doGet(endpoint);
        assertThat(response.statusCode()).isEqualTo(expectedStatus.value());
        assertThat(response.contentType()).contains("json");
        assertThat(response.jsonPath().getList("$")).isNotNull();
    }

    /**
     * Performs a GET request to the specified endpoint and validates the response contains expected data.
     * Useful for verifying endpoints are not misconfigured or returning the wrong data type.
     *
     * @param http the HTTP client
     * @param endpoint the endpoint path to test
     * @param expectedStatus the expected HTTP status code
     * @param fieldName the field name to check in the first array element
     * @param expectedValue the expected value for the field
     */
    public static void assertJsonArrayResponseContainsValue(final HttpClient http,
                                                             final String endpoint,
                                                             final HttpStatus expectedStatus,
                                                             final String fieldName,
                                                             final String expectedValue) {
        final Response response = http.doGet(endpoint);
        assertThat(response.statusCode()).isEqualTo(expectedStatus.value());
        assertThat(response.contentType()).contains("json");
        assertThat(response.jsonPath().getList("$")).isNotEmpty();
        assertThat(response.jsonPath().getString("[0]." + fieldName)).isEqualTo(expectedValue);
    }
}
