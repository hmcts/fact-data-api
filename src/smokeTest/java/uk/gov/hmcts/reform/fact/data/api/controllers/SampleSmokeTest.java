package uk.gov.hmcts.reform.fact.data.api.controllers;

import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;

class SampleSmokeTest {

    private final String testUrl = System.getenv().getOrDefault("TEST_URL", "http://localhost:8989");

    @BeforeEach
    void setUp() {
        RestAssured.useRelaxedHTTPSValidation();
    }

    @Test
    void smokeTest() {
        Response response = given()
                .baseUri(testUrl)
                .contentType(ContentType.JSON)
                .when()
                .get("/")
                .then()
                .extract().response();

        Assertions.assertEquals(200, response.statusCode());
        Assertions.assertTrue(response.asString().startsWith("Welcome"));
    }

    @Test
    void healthCheck() {
        Response response = given()
            .baseUri(testUrl)
            .when()
            .get("/health")
            .then()
            .statusCode(200)
            .extract().response();

        System.out.println("Health Status: " + response.jsonPath().getString("status"));
        Assertions.assertEquals("UP", response.jsonPath().getString("status"));
    }

    @Test
    void testReadinessCheck() {
        Response response = given()
            .baseUri(testUrl)
            .when()
            .get("/health/readiness")
            .then()
            .statusCode(200)
            .extract().response();

        System.out.println("DB Status: " + response.jsonPath().getString("status"));
        System.out.println("DB Details: " + response.jsonPath().getString("components.db.status"));
        Assertions.assertEquals("UP", response.jsonPath().getString("status"));
    }
}
