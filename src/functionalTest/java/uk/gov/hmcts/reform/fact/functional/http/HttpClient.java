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
import lombok.extern.slf4j.Slf4j;

/**
 * Simple HTTP wrapper for functional tests.
 */
@Slf4j
public final class HttpClient {

    private static final String ADMIN_CLIENT_APP_REG_ID = "ADMIN_CLIENT_APP_REG_ID";
    private static final String VIEWER_CLIENT_APP_REG_ID = "VIEWER_CLIENT_APP_REG_ID";
    private static final String ADMIN_AZURE_CLIENT_SECRET = "ADMIN_AZURE_CLIENT_SECRET";
    private static final String VIEWER_AZURE_CLIENT_SECRET = "VIEWER_AZURE_CLIENT_SECRET";

    private final String baseUrl;
    private static final AtomicReference<String> factAdminBearerToken = new AtomicReference<>();
    private static final AtomicReference<String> factViewerBearerToken = new AtomicReference<>();

    public HttpClient() {
        this.baseUrl = System.getenv().getOrDefault("TEST_URL", "http://localhost:8989");
    }

    public static String getAdminBearerToken() {
        return getBearerToken(ADMIN_CLIENT_APP_REG_ID, ADMIN_AZURE_CLIENT_SECRET, factAdminBearerToken);
    }

    public static String getViewerBearerToken() {
        return getBearerToken(VIEWER_CLIENT_APP_REG_ID, VIEWER_AZURE_CLIENT_SECRET, factViewerBearerToken);
    }

    private static String getBearerToken(final String clientAppRegEnvVar,
                                         final String clientSecretEnvVar,
                                         final AtomicReference<String> tokenCache) {
        synchronized (tokenCache) {
            if (tokenCache.get() == null) {
                // Load the custom properties from the environment.
                // AZURE_TENANT_ID and the role-specific client secret are used from the environment.
                final String clientAppRegId = getRequiredEnv(clientAppRegEnvVar);
                final String appRegId = Optional.ofNullable(System.getenv("APP_REG_ID"))
                    .orElseThrow(() -> new IllegalStateException("No APP_REG_ID environment set"));
                final String tenantId = Optional.ofNullable(System.getenv("AZURE_TENANT_ID"))
                    .orElseThrow(() -> new IllegalStateException("No AZURE_TENANT_ID environment set"));
                final String clientSecret = getRequiredEnv(clientSecretEnvVar);

                log.info("All authentication env variables set for {} using {}", clientAppRegEnvVar,
                         clientSecretEnvVar);

                // set the scope up for the destination app
                TokenRequestContext requestContext = new TokenRequestContext();
                requestContext.addScopes(String.format("api://%s/.default", appRegId));

                // Create config override for the azure client id
                Configuration configuration = new ConfigurationBuilder()
                    .putProperty(Configuration.PROPERTY_AZURE_CLIENT_ID, clientAppRegId)
                    .putProperty(Configuration.PROPERTY_AZURE_CLIENT_SECRET, clientSecret)
                    .putProperty(Configuration.PROPERTY_AZURE_TENANT_ID, tenantId)
                    .build();

                // Create the credential object and request the token
                DefaultAzureCredential credentials = new DefaultAzureCredentialBuilder()
                    .managedIdentityClientId(clientAppRegId)
                    .configuration(configuration)
                    .build();

                tokenCache.set(
                    Optional.ofNullable(credentials.getTokenSync(requestContext))
                        .map(AccessToken::getToken)
                        .orElseThrow(() -> new IllegalStateException("Failed to get token"))
                );
            }
            return tokenCache.get();
        }
    }

    private static String getRequiredEnv(final String varName) {
        final String value = System.getenv(varName);
        if (value != null && !value.isBlank()) {
            return value;
        }

        throw new IllegalStateException("No " + varName + " environment set");
    }

    private RequestSpecification requestWithOptionalAuthorization(final String bearerToken) {
        RequestSpecification request = given()
            .filter(new AllureRestAssured())
            .baseUri(baseUrl);

        if (bearerToken != null && !bearerToken.isBlank()) {
            request = request.header("Authorization", "Bearer " + bearerToken);
        }

        return request;
    }

    public Response doGet(final String path) {
        return doGet(path, getAdminBearerToken());
    }

    public Response doGet(final String path, final String bearerToken) {
        return requestWithOptionalAuthorization(bearerToken)
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
        RequestSpecification request = requestWithOptionalAuthorization(bearerToken);

        for (Map.Entry<String, Object> entry : queryParams.entrySet()) {
            if (entry.getValue() != null) {
                request.queryParam(entry.getKey(), entry.getValue());
            }
        }

        return request.when()
            .get(path)
            .thenReturn();
    }

    public Response doPost(final String path, final Object body) {
        return doPost(path, body, getAdminBearerToken());
    }

    public Response doPost(final String path, final Object body, final String bearerToken) {
        return requestWithOptionalAuthorization(bearerToken)
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
        return requestWithOptionalAuthorization(bearerToken)
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
        return requestWithOptionalAuthorization(bearerToken)
            .when()
            .delete(path)
            .thenReturn();
    }

    public Response doMultipartPost(final String path, final String fileParamName, final File file) {
        return doMultipartPost(path, fileParamName, file, getAdminBearerToken());
    }

    public Response doMultipartPost(final String path, final String fileParamName,
                                    final File file, final String bearerToken) {
        return requestWithOptionalAuthorization(bearerToken)
            .contentType(ContentType.MULTIPART)
            .multiPart(fileParamName, file)
            .when()
            .post(path)
            .thenReturn();
    }
}
