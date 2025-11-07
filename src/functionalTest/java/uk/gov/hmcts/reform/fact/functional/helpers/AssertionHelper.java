package uk.gov.hmcts.reform.fact.functional.helpers;

import io.restassured.response.Response;
import org.springframework.http.HttpStatus;
import uk.gov.hmcts.reform.fact.functional.http.HttpClient;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Helper methods for assertions in functional tests.
 */
public final class AssertionHelper {

    private AssertionHelper() {
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

    /**
     * Asserts that a paginated list response has a valid structure (200 OK + page object).
     *
     * @param response the response to validate
     */
    public static void assertPaginatedResponseValid(final Response response) {
        assertThat(response.statusCode()).isEqualTo(HttpStatus.OK.value());
        assertThat(response.jsonPath().getMap("page")).isNotNull();
    }

    /**
     * Extracts court IDs from a paginated list response content array.
     *
     * @param response the response containing the paginated list
     * @return list of UUIDs extracted from content ID.
     */
    public static List<UUID> extractCourtIdsFromResponse(final Response response) {
        final List<String> courtIdStrings = response.jsonPath().getList("content.id", String.class);
        return courtIdStrings.stream()
            .map(UUID::fromString)
            .toList();
    }

    /**
     * Asserts that a court ID is present in a paginated list response.
     *
     * @param response the response containing the paginated list
     * @param expectedCourtId the court ID to find
     */
    public static void assertCourtIdInListResponse(final Response response, final UUID expectedCourtId) {
        assertPaginatedResponseValid(response);
        final List<UUID> courtIds = extractCourtIdsFromResponse(response);
        assertThat(courtIds).contains(expectedCourtId);
    }
}
