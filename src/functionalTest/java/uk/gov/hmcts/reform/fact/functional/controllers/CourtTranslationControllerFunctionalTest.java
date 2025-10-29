package uk.gov.hmcts.reform.fact.functional.controllers;

import io.restassured.response.Response;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import uk.gov.hmcts.reform.fact.functional.config.TestConfig;
import uk.gov.hmcts.reform.fact.functional.http.HttpClient;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

public final class CourtTranslationControllerFunctionalTest {

    private static final UUID COURT_WITH_TRANSLATION = UUID.fromString("550e8400-e29b-41d4-a716-446655440001");
    private static final UUID COURT_WITHOUT_TRANSLATION = UUID.fromString("550e8400-e29b-41d4-a716-446655440002");
    //private static final UUID NON_EXISTENT_COURT = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID VALID_JSON_UUID = UUID.fromString("3fa85f64-5717-4562-b3fc-2c963f66afa6");
    private static final String INVALID_UUID = "invalid-uuid";

    private static HttpClient http;

    @BeforeAll
    static void setUp() {
        final var config = TestConfig.load();
        http = new HttpClient(config);
    }

    @Test
    void shouldUpdateTranslationServices() {
        String translationJson = """
            {
                "courtId": "%s",
                "email": "updated@court.com",
                "phoneNumber": "01234567890"
            }
            """.formatted(VALID_JSON_UUID);

        Response postResponse = http.doPost(
            "/courts/" + COURT_WITH_TRANSLATION + "/v1/translation-services",
            translationJson
        );

        assertThat(postResponse.statusCode()).isEqualTo(201);
        assertThat(postResponse.jsonPath().getString("email")).isEqualTo("updated@court.com");
        assertThat(postResponse.jsonPath().getString("phoneNumber")).isEqualTo("01234567890");
    }

    @Test
    void shouldRetrieveExistingTranslationServices() {
        Response getResponse = http.doGet("/courts/" + COURT_WITH_TRANSLATION + "/v1/translation-services");

        assertThat(getResponse.statusCode()).isEqualTo(200);
        assertThat(getResponse.jsonPath().getString("email")).isNotNull();
        assertThat(getResponse.jsonPath().getString("phoneNumber")).isNotNull();
    }

    @Test
    void shouldReturn204WhenNoTranslationServices() {
        Response getResponse = http.doGet("/courts/" + COURT_WITHOUT_TRANSLATION + "/v1/translation-services");

        assertThat(getResponse.statusCode()).isEqualTo(204);
    }

    @Test
    void shouldReturn404ForNonExistentCourt() {
        Response getResponse = http.doGet("/courts/" + VALID_JSON_UUID + "/v1/translation-services");

        assertThat(getResponse.statusCode()).isEqualTo(404);

        String translationJson = """
            {
                "courtId": "%s",
                "email": "test@court.com",
                "phoneNumber": "01234567890"
            }
            """.formatted(VALID_JSON_UUID);

        Response postResponse = http.doPost(
            "/courts/" + VALID_JSON_UUID + "/v1/translation-services",
            translationJson
        );

        assertThat(postResponse.statusCode()).isEqualTo(404);
    }

    @Test
    void shouldRejectInvalidEmailFormat() {
        String invalidEmailJson = """
            {
                "courtId": "%s",
                "email": "invalid-email-no-at-sign",
                "phoneNumber": "01234567890"
            }
            """.formatted(VALID_JSON_UUID);

        Response postResponse = http.doPost(
            "/courts/" + COURT_WITH_TRANSLATION + "/v1/translation-services",
            invalidEmailJson
        );

        assertThat(postResponse.statusCode()).isEqualTo(400);
    }

    @Test
    void shouldRejectInvalidPhoneFormat() {
        String invalidPhoneJson = """
            {
                "courtId": "%s",
                "email": "test@court.com",
                "phoneNumber": "123"
            }
            """.formatted(VALID_JSON_UUID);

        Response postResponse = http.doPost(
            "/courts/" + COURT_WITH_TRANSLATION + "/v1/translation-services",
            invalidPhoneJson
        );

        assertThat(postResponse.statusCode()).isEqualTo(400);
    }

    @Test
    void shouldReturn400ForInvalidUuidFormat() {
        Response getResponse = http.doGet("/courts/" + INVALID_UUID + "/v1/translation-services");
        assertThat(getResponse.statusCode()).isEqualTo(400);
    }
}
