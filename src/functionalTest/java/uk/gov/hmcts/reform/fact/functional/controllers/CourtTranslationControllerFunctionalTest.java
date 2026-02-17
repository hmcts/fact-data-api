package uk.gov.hmcts.reform.fact.functional.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import io.qameta.allure.Feature;
import io.restassured.response.Response;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import uk.gov.hmcts.reform.fact.data.api.entities.CourtTranslation;
import uk.gov.hmcts.reform.fact.functional.helpers.AssertionHelper;
import uk.gov.hmcts.reform.fact.functional.helpers.TestDataHelper;
import uk.gov.hmcts.reform.fact.functional.http.HttpClient;

import java.time.ZonedDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.CREATED;
import static org.springframework.http.HttpStatus.NOT_FOUND;
import static org.springframework.http.HttpStatus.NO_CONTENT;
import static org.springframework.http.HttpStatus.OK;

@Feature("Court Translation Controller")
@DisplayName("Court Translation Controller")
public final class CourtTranslationControllerFunctionalTest {

    private static final HttpClient http = new HttpClient();
    private static final ObjectMapper mapper = new ObjectMapper()
        .registerModule(new JavaTimeModule())
        .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

    private void assertTranslationFields(CourtTranslation translation, UUID expectedId, UUID expectedCourtId,
                                         String expectedEmail, String expectedPhone) {
        if (expectedId != null) {
            assertThat(translation.getId())
                .as("Translation ID should match expected ID")
                .isEqualTo(expectedId);
        } else {
            assertThat(translation.getId())
                .as("Translation should have a generated ID")
                .isNotNull();
        }
        assertThat(translation.getCourtId())
            .as("Translation court ID should match")
            .isEqualTo(expectedCourtId);
        assertThat(translation.getEmail())
            .as("Translation email should match")
            .isEqualTo(expectedEmail);
        assertThat(translation.getPhoneNumber())
            .as("Translation phone number should match")
            .isEqualTo(expectedPhone);
    }

    @Test
    @DisplayName("POST /courts/{courtId}/v1/translation-services with email and phone")
    void shouldCreateTranslationWithEmailAndPhone() throws Exception {
        final UUID courtId = TestDataHelper.createCourt(http, "Test Court Translation Full", true);

        final ZonedDateTime timestampBeforeCreate = AssertionHelper.getCourtLastUpdatedAt(http, courtId);

        final CourtTranslation translation = new CourtTranslation();
        translation.setCourtId(courtId);
        translation.setEmail("translation@court.gov.uk");
        translation.setPhoneNumber("01234567890");

        final Response postResponse = http.doPost("/courts/" + courtId + "/v1/translation-services", translation);
        assertThat(postResponse.statusCode())
            .as("Expected 201 CREATED when creating translation for court %s", courtId)
            .isEqualTo(CREATED.value());

        final CourtTranslation createdTranslation = mapper.readValue(postResponse.asString(), CourtTranslation.class);
        assertTranslationFields(createdTranslation, null, courtId, "translation@court.gov.uk",
                                "01234567890");

        final Response getResponse = http.doGet("/courts/" + courtId + "/v1/translation-services");
        assertThat(getResponse.statusCode())
            .as("Expected 200 OK when retrieving translation for court %s", courtId)
            .isEqualTo(OK.value());

        final CourtTranslation retrievedTranslation = mapper.readValue(getResponse.asString(), CourtTranslation.class);
        assertTranslationFields(retrievedTranslation, createdTranslation.getId(), courtId,
                               "translation@court.gov.uk", "01234567890");

        final ZonedDateTime timestampAfterCreate = AssertionHelper.getCourtLastUpdatedAt(http, courtId);
        assertThat(timestampAfterCreate)
            .as("Court lastUpdatedAt should move forward after translation creation for court %s", courtId)
            .isAfter(timestampBeforeCreate);
    }

    @Test
    @DisplayName("POST /courts/{courtId}/v1/translation-services updates existing translation service")
    void shouldUpdateExistingTranslation() throws Exception {
        final UUID courtId = TestDataHelper.createCourt(http, "Test Court Translation Update", true);

        final CourtTranslation initialTranslation = new CourtTranslation();
        initialTranslation.setCourtId(courtId);
        initialTranslation.setEmail("initial@court.gov.uk");
        initialTranslation.setPhoneNumber("01111111111");

        final Response createResponse = http.doPost("/courts/"
                                                        + courtId + "/v1/translation-services", initialTranslation);
        assertThat(createResponse.statusCode())
            .as("Expected 201 CREATED when creating initial translation for court %s", courtId)
            .isEqualTo(CREATED.value());

        final CourtTranslation createdTranslation = mapper.readValue(createResponse.asString(), CourtTranslation.class);
        final UUID translationId = createdTranslation.getId();

        final CourtTranslation updatedTranslation = new CourtTranslation();
        updatedTranslation.setCourtId(courtId);
        updatedTranslation.setEmail("updated@court.gov.uk");
        updatedTranslation.setPhoneNumber("02222222222");

        final Response updateResponse = http.doPost("/courts/"
                                                        + courtId + "/v1/translation-services", updatedTranslation);
        assertThat(updateResponse.statusCode())
            .as("Expected 201 CREATED when updating translation for court %s", courtId)
            .isEqualTo(CREATED.value());

        final CourtTranslation modifiedTranslation = mapper.readValue(updateResponse
                                                                          .asString(), CourtTranslation.class);
        assertTranslationFields(modifiedTranslation, translationId, courtId, "updated@court.gov.uk",
                                "02222222222");

        final Response getResponse = http.doGet("/courts/" + courtId + "/v1/translation-services");
        assertThat(getResponse.statusCode())
            .as("Expected 200 OK when retrieving updated translation for court %s", courtId)
            .isEqualTo(OK.value());

        final CourtTranslation retrievedTranslation = mapper.readValue(getResponse.asString(), CourtTranslation.class);
        assertTranslationFields(retrievedTranslation, translationId, courtId, "updated@court.gov.uk",
                                "02222222222");
    }

    @Test
    @DisplayName("POST /courts/{courtId}/v1/translation-services fails with non-existent court")
    void shouldFailToCreateTranslationForNonExistentCourt() {
        final UUID nonExistentCourtId = UUID.randomUUID();

        final CourtTranslation translation = new CourtTranslation();
        translation.setCourtId(nonExistentCourtId);
        translation.setEmail("test@court.gov.uk");
        translation.setPhoneNumber("01234567890");

        final Response postResponse = http.doPost("/courts/"
                                                      + nonExistentCourtId + "/v1/translation-services", translation);
        assertThat(postResponse.statusCode())
            .as("Expected 404 NOT_FOUND for non-existent court %s", nonExistentCourtId)
            .isEqualTo(NOT_FOUND.value());
        assertThat(postResponse.jsonPath().getString("message"))
            .as("Error message should indicate court not found")
            .contains("Court not found, ID: " + nonExistentCourtId);
    }

    @Test
    @DisplayName("POST /courts/{courtId}/v1/translation-services fails with invalid email format")
    void shouldFailWithInvalidEmailFormat() {
        final UUID courtId = TestDataHelper.createCourt(http, "Test Court Translation Invalid Email", true);

        final CourtTranslation translation = new CourtTranslation();
        translation.setCourtId(courtId);
        translation.setEmail("invalid-email-no-at-sign");
        translation.setPhoneNumber("01234567890");

        final Response postResponse = http.doPost("/courts/" + courtId + "/v1/translation-services", translation);
        assertThat(postResponse.statusCode())
            .as("Expected 400 BAD_REQUEST for invalid email format")
            .isEqualTo(BAD_REQUEST.value());
        assertThat(postResponse.jsonPath().getString("email"))
            .as("Error message should indicate invalid email format")
            .contains("Email address must match the regex");
    }

    @Test
    @DisplayName("POST /courts/{courtId}/v1/translation-services fails with invalid phone")
    void shouldFailWithInvalidPhoneFormat() {
        final UUID courtId = TestDataHelper.createCourt(http, "Test Court Translation Invalid Phone", true);

        final CourtTranslation translation = new CourtTranslation();
        translation.setCourtId(courtId);
        translation.setEmail("valid@court.gov.uk");
        translation.setPhoneNumber("123");

        final Response postResponse = http.doPost("/courts/" + courtId + "/v1/translation-services", translation);
        assertThat(postResponse.statusCode())
            .as("Expected 400 BAD_REQUEST for invalid phone format")
            .isEqualTo(BAD_REQUEST.value());
        assertThat(postResponse.jsonPath().getString("phoneNumber"))
            .as("Error message should indicate invalid phone format")
            .contains("Phone Number must match the regex");
    }

    @Test
    @DisplayName("POST /courts/{courtId}/v1/translation-services fails with invalid UUID")
    void shouldFailPostWithInvalidUuid() {
        final String invalidUuid = "invalid-uuid";

        final CourtTranslation translation = new CourtTranslation();
        translation.setCourtId(UUID.randomUUID());
        translation.setEmail("test@court.gov.uk");
        translation.setPhoneNumber("01234567890");

        final Response postResponse = http.doPost("/courts/"
                                                      + invalidUuid + "/v1/translation-services", translation);
        assertThat(postResponse.statusCode())
            .as("Expected 400 BAD_REQUEST for invalid UUID")
            .isEqualTo(BAD_REQUEST.value());
        assertThat(postResponse.jsonPath().getString("message"))
            .as("Error message should indicate invalid UUID")
            .contains("Invalid UUID supplied: " + invalidUuid);
    }

    @Test
    @DisplayName("GET /courts/{courtId}/v1/translation-services retrieves existing service")
    void shouldRetrieveExistingTranslation() throws Exception {
        final UUID courtId = TestDataHelper.createCourt(http, "Test Court Translation Get", true);

        final CourtTranslation translation = new CourtTranslation();
        translation.setCourtId(courtId);
        translation.setEmail("get@court.gov.uk");
        translation.setPhoneNumber("01234567890");

        http.doPost("/courts/" + courtId + "/v1/translation-services", translation);

        final Response getResponse = http.doGet("/courts/" + courtId + "/v1/translation-services");
        assertThat(getResponse.statusCode())
            .as("Expected 200 OK when retrieving translation for court %s", courtId)
            .isEqualTo(OK.value());

        final CourtTranslation retrievedTranslation = mapper.readValue(getResponse.asString(), CourtTranslation.class);
        assertTranslationFields(retrievedTranslation, null, courtId, "get@court.gov.uk",
                                "01234567890");
    }

    @Test
    @DisplayName("GET /courts/{courtId}/v1/translation-services returns 204 when no translation exists")
    void shouldReturn204WhenNoTranslation() {
        final UUID courtId = TestDataHelper.createCourt(http, "Test Court No Translation", true);

        final Response getResponse = http.doGet("/courts/" + courtId + "/v1/translation-services");
        assertThat(getResponse.statusCode())
            .as("Expected 204 NO_CONTENT when no translation exists for court %s", courtId)
            .isEqualTo(NO_CONTENT.value());
    }

    @Test
    @DisplayName("GET /courts/{courtId}/v1/translation-services fails with non-existent court")
    void shouldFailToGetTranslationForNonExistentCourt() {
        final UUID nonExistentCourtId = UUID.randomUUID();

        final Response getResponse = http.doGet("/courts/" + nonExistentCourtId + "/v1/translation-services");
        assertThat(getResponse.statusCode())
            .as("Expected 404 NOT_FOUND for non-existent court %s", nonExistentCourtId)
            .isEqualTo(NOT_FOUND.value());
        assertThat(getResponse.jsonPath().getString("message"))
            .as("Error message should indicate court not found")
            .contains("Court not found, ID: " + nonExistentCourtId);
    }

    @AfterAll
    static void cleanUpTestData() {
        http.doDelete("/testing-support/courts/name-prefix/Test Court");
    }
}
