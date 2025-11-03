package uk.gov.hmcts.reform.fact.functional.helpers;

import io.restassured.response.Response;
import uk.gov.hmcts.reform.fact.functional.data.CourtTestData;
import uk.gov.hmcts.reform.fact.functional.http.HttpClient;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.HttpStatus.CREATED;

/**
 * Helper methods for creating test data via API endpoints.
 */
public final class TestDataHelper {

    private TestDataHelper() {
    }

    public static String createCourt(final HttpClient http, final CourtTestData court) {
        final Response response = http.doPost("/courts/v1", court);
        assertThat(response.statusCode()).isEqualTo(CREATED.value());
        return response.jsonPath().getString("id");
    }
}
