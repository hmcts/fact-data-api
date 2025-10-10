package uk.gov.hmcts.reform.fact.functional.http;

import io.restassured.http.ContentType;
import io.restassured.response.Response;
import uk.gov.hmcts.reform.fact.functional.config.TestConfig;

import static io.restassured.RestAssured.given;

/**
 * Simple HTTP wrapper for functional tests.
 */
public final class HttpClient {

    private final TestConfig config;

    public HttpClient(final TestConfig config) {
        this.config = config;
    }

    public Response doGet(final String path) {
        return given()
            .baseUri(config.baseUrl())
            .when()
            .get(path)
            .thenReturn();
    }

    public Response doPost(final String path, final Object body) {
        return given()
            .baseUri(config.baseUrl())
            .contentType(ContentType.JSON)
            .body(body)
            .when()
            .post(path)
            .thenReturn();
    }
}
