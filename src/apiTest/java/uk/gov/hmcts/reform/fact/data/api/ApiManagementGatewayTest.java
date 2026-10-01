package uk.gov.hmcts.reform.fact.data.api;

import com.azure.core.credential.AccessToken;
import com.azure.core.credential.TokenRequestContext;
import com.azure.identity.ClientSecretCredential;
import com.azure.identity.ClientSecretCredentialBuilder;
import io.restassured.RestAssured;
import io.restassured.response.Response;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.UUID;

import static io.restassured.RestAssured.given;

class ApiManagementGatewayTest {

    private static final String TEST_URL = getRequiredEnv("TEST_URL");
    private static final String TESTING_SUPPORT_URL = getRequiredEnv("TESTING_SUPPORT_URL");
    private static final String COURT_NAME = "APIM Gateway Test " + UUID.randomUUID();

    private static String adminToken;
    private static String viewerToken;
    private static String courtBySlugPath;

    @BeforeAll
    static void setUp() {
        RestAssured.useRelaxedHTTPSValidation();
        adminToken = getBearerToken("ADMIN_CLIENT_APP_REG_ID", "ADMIN_AZURE_CLIENT_SECRET");
        viewerToken = getBearerToken("VIEWER_CLIENT_APP_REG_ID", "VIEWER_AZURE_CLIENT_SECRET");

        Response response = given()
            .baseUri(TESTING_SUPPORT_URL)
            .header("Authorization", "Bearer " + adminToken)
            .queryParam("courtName", COURT_NAME)
            .when()
            .get("/testing-support/courts")
            .then()
            .statusCode(201)
            .extract()
            .response();

        courtBySlugPath = "/courts/slug/" + response.jsonPath().getString("slug") + "/v1";
    }

    @AfterAll
    static void cleanUp() {
        if (courtBySlugPath == null) {
            return;
        }

        given()
            .baseUri(TESTING_SUPPORT_URL)
            .header("Authorization", "Bearer " + adminToken)
            .pathParam("courtNamePrefix", COURT_NAME)
            .when()
            .delete("/testing-support/courts/name-prefix/{courtNamePrefix}")
            .then()
            .statusCode(200);
    }

    @Test
    void rejectsRequestsWithoutBearerToken() {
        given()
            .baseUri(TEST_URL)
            .when()
            .get(courtBySlugPath)
            .then()
            .statusCode(401);
    }

    @Test
    void allowsAdminRoleToAccessGatewayEndpoint() {
        given()
            .baseUri(TEST_URL)
            .header("Authorization", "Bearer " + adminToken)
            .when()
            .get(courtBySlugPath)
            .then()
            .statusCode(200);
    }

    @Test
    void rejectsViewerRoleAtGateway() {
        given()
            .baseUri(TEST_URL)
            .header("Authorization", "Bearer " + viewerToken)
            .when()
            .get(courtBySlugPath)
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
