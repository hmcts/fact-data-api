package uk.gov.hmcts.reform.fact.functional.http;

import static io.restassured.RestAssured.given;

import java.io.File;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

import com.azure.core.credential.AccessToken;
import com.azure.core.credential.TokenRequestContext;
import com.azure.core.util.Configuration;
import com.azure.core.util.ConfigurationBuilder;
import com.azure.identity.DefaultAzureCredential;
import com.azure.identity.DefaultAzureCredentialBuilder;
import io.qameta.allure.restassured.AllureRestAssured;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;

/**
 * Simple HTTP wrapper for functional tests.
 */
public final class HttpClient {

    private final String baseUrl;
    private static final AtomicReference<String> factAdminBearerToken = new AtomicReference<>();

    public HttpClient() {
        this.baseUrl = System.getenv().getOrDefault("TEST_URL", "http://localhost:8989");
    }

    public static String getAdminBearerToken() {
        synchronized (factAdminBearerToken) {
            if (factAdminBearerToken.get() == null) {
                // Load the custom properties from the environment.
                // AZURE_TENANT_ID and AZURE_CLIENT_SECRET will be used directly from the environment
                String clientAppRegId = Optional.ofNullable(System.getenv("CLIENT_APP_REG_ID"))
                    .orElseThrow(() -> new IllegalStateException("No CLIENT_APP_REG_ID environment set"));
                String appRegId = Optional.ofNullable(System.getenv("APP_REG_ID"))
                    .orElseThrow(() -> new IllegalStateException("No APP_REG_ID environment set"));

                // set the scope up for the destination app
                TokenRequestContext requestContext = new TokenRequestContext();
                requestContext.addScopes(String.format("api://%s/.default", appRegId));

                // Create config override for the azure client id
                Configuration configuration = new ConfigurationBuilder()
                    .putProperty(Configuration.PROPERTY_AZURE_CLIENT_ID, clientAppRegId)
                    .build();

                // Create the credential object and request the token
                DefaultAzureCredential credentials = new DefaultAzureCredentialBuilder()
                    .managedIdentityClientId(clientAppRegId)
                    .configuration(configuration)
                    .build();

                factAdminBearerToken.set(
                    Optional.ofNullable(credentials.getTokenSync(requestContext))
                        .map(AccessToken::getToken)
                        .orElseThrow(() -> new IllegalStateException("Failed to get token"))
                );
            }
            return factAdminBearerToken.get();
        }
    }

    public Response doGet(final String path) {
        return doGet(path, getAdminBearerToken());
    }

    public Response doGet(final String path, final String bearerToken) {
        return given()
            .filter(new AllureRestAssured())
            .baseUri(baseUrl)
            .header("Authorization", "Bearer " + bearerToken)
            .when()
            .get(path)
            .thenReturn();
    }

    public Response doGet(final String path, final Map<String, Object> queryParams) {
        return doGet(path, queryParams, getAdminBearerToken());
    }

    /**
     * GET request with query parameters.
     * Example: doGet("/courts/v1", Map.of("pageNumber", 0, "pageSize", 25))
     */
    public Response doGet(final String path, final Map<String, Object> queryParams, final String bearerToken) {
        RequestSpecification request = given()
            .filter(new AllureRestAssured())
            .baseUri(baseUrl);

        for (Map.Entry<String, Object> entry : queryParams.entrySet()) {
            if (entry.getValue() != null) {
                request.queryParam(entry.getKey(), entry.getValue());
            }
        }

        return request
            .header("Authorization", "Bearer " + bearerToken)
            .when()
            .get(path)
            .thenReturn();
    }

    public Response doPost(final String path, final Object body) {
        return doPost(path, body, getAdminBearerToken());
    }

    public Response doPost(final String path, final Object body, final String bearerToken) {
        return given()
            .filter(new AllureRestAssured())
            .baseUri(baseUrl)
            .header("Authorization", "Bearer " + bearerToken)
            .contentType(ContentType.JSON)
            .body(body)
            .when()
            .post(path)
            .thenReturn();
    }

    public Response doPut(final String path, final Object body) {
        return doPut(path, body, getAdminBearerToken());
    }

    public Response doPut(final String path, final Object body, final String bearerToken) {
        return given()
            .filter(new AllureRestAssured())
            .baseUri(baseUrl)
            .header("Authorization", "Bearer " + bearerToken)
            .contentType(ContentType.JSON)
            .body(body)
            .when()
            .put(path)
            .thenReturn();
    }

    public Response doDelete(final String path) {
        return doDelete(path, getAdminBearerToken());
    }

    public Response doDelete(final String path, final String bearerToken) {
        return given()
            .filter(new AllureRestAssured())
            .baseUri(baseUrl)
            .header("Authorization", "Bearer " + bearerToken)
            .when()
            .delete(path)
            .thenReturn();
    }

    public Response doMultipartPost(final String path, final String fileParamName, final File file) {
        return doMultipartPost(path, fileParamName, file, getAdminBearerToken());
    }

    public Response doMultipartPost(final String path, final String fileParamName,
                                    final File file, final String bearerToken) {
        return given()
            .filter(new AllureRestAssured())
            .baseUri(baseUrl)
            .header("Authorization", "Bearer " + bearerToken)
            .contentType(ContentType.MULTIPART)
            .multiPart(fileParamName, file)
            .when()
            .post(path)
            .thenReturn();
    }
}
