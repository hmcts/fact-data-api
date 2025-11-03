package uk.gov.hmcts.reform.fact.functional.http;

import io.restassured.http.ContentType;
import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;
import uk.gov.hmcts.reform.fact.functional.config.TestConfig;

import java.io.File;
import java.util.Map;

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

    /**
     * GET request with query parameters.
     * Example: doGet("/courts/v1", Map.of("pageNumber", 0, "pageSize", 25))
     */
    public Response doGet(final String path, final Map<String, Object> queryParams) {
        RequestSpecification request = given().baseUri(config.baseUrl());

        // Add each query parameter - RestAssured handles null values gracefully
        for (Map.Entry<String, Object> entry : queryParams.entrySet()) {
            if (entry.getValue() != null) {
                request.queryParam(entry.getKey(), entry.getValue());
            }
        }

        return request
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

    public Response doPut(final String path, final Object body) {
        return given()
            .baseUri(config.baseUrl())
            .contentType(ContentType.JSON)
            .body(body)
            .when()
            .put(path)
            .thenReturn();
    }

    public Response doDelete(final String path) {
        return given()
            .baseUri(config.baseUrl())
            .when()
            .delete(path)
            .thenReturn();
    }

    public Response doMultipartPost(final String path, final String fileParamName, final File file) {
        return given()
            .baseUri(config.baseUrl())
            .contentType(ContentType.MULTIPART)
            .multiPart(fileParamName, file)
            .when()
            .post(path)
            .thenReturn();
    }
}
