package uk.gov.hmcts.reform.fact.functional.helpers;

import io.restassured.response.Response;
import uk.gov.hmcts.reform.fact.functional.http.HttpClient;

/**
 * Helper methods for fetching test data via API endpoints.
 */
public final class TestDataHelper {

    private TestDataHelper() {
        // Utility class
    }

    /**
     * Fetches the first region ID from the regions' endpoint.
     *
     * @param http the HTTP client
     * @return the region ID as a string
     */
    public static String getRegionId(final HttpClient http) {
        final Response response = http.doGet("/types/v1/regions");
        return response.jsonPath().getString("[0].id");
    }
}
