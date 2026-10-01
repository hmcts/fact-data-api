package uk.gov.hmcts.reform.fact.data.api;

import com.azure.core.credential.AccessToken;
import com.azure.core.credential.TokenRequestContext;
import com.azure.identity.ClientSecretCredential;
import com.azure.identity.ClientSecretCredentialBuilder;
import io.restassured.RestAssured;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static io.restassured.RestAssured.given;

class ApiManagementGatewayTest {

    private static final String COURT_BY_SLUG_PATH = "/courts/slug/glasgow-crown-court/v1";
    private static final String TEST_URL = getRequiredEnv("TEST_URL");

    @BeforeAll
    static void setUp() {
        RestAssured.useRelaxedHTTPSValidation();
    }

    @Test
    void rejectsRequestsWithoutBearerToken() {
        given()
            .baseUri(TEST_URL)
            .when()
            .get(COURT_BY_SLUG_PATH)
            .then()
            .statusCode(401);
    }

    @Test
    void allowsAdminRoleToAccessGatewayEndpoint() {
        given()
            .baseUri(TEST_URL)
            .header("Authorization", "Bearer " + getBearerToken(
                "ADMIN_CLIENT_APP_REG_ID",
                "ADMIN_AZURE_CLIENT_SECRET"
            ))
            .when()
            .get(COURT_BY_SLUG_PATH)
            .then()
            .statusCode(200);
    }

    @Test
    void rejectsViewerRoleAtGateway() {
        given()
            .baseUri(TEST_URL)
            .header("Authorization", "Bearer " + getBearerToken(
                "VIEWER_CLIENT_APP_REG_ID",
                "VIEWER_AZURE_CLIENT_SECRET"
            ))
            .when()
            .get(COURT_BY_SLUG_PATH)
            .then()
            .statusCode(401);
    }

    private static String getBearerToken(String clientIdEnvVar, String clientSecretEnvVar) {
        ClientSecretCredential credential = new ClientSecretCredentialBuilder()
            .clientId(getRequiredEnv(clientIdEnvVar))
            .clientSecret(getRequiredEnv(clientSecretEnvVar))
            .tenantId(getRequiredEnv("AZURE_TENANT_ID"))
            .build();

        TokenRequestContext requestContext = new TokenRequestContext()
            .addScopes("api://" + getRequiredEnv("APP_REG_ID") + "/.default");

        return Optional.ofNullable(credential.getTokenSync(requestContext))
            .map(AccessToken::getToken)
            .orElseThrow(() -> new IllegalStateException("Failed to acquire bearer token"));
    }

    private static String getRequiredEnv(String name) {
        return Optional.ofNullable(System.getenv(name))
            .filter(value -> !value.isBlank())
            .orElseThrow(() -> new IllegalStateException("No " + name + " environment set"));
    }
}
