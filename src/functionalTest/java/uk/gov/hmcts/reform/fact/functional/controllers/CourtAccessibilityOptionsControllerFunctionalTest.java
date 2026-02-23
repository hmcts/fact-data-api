package uk.gov.hmcts.reform.fact.functional.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import io.qameta.allure.Feature;
import io.restassured.response.Response;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import uk.gov.hmcts.reform.fact.data.api.entities.CourtAccessibilityOptions;
import uk.gov.hmcts.reform.fact.data.api.entities.types.HearingEnhancementEquipment;
import uk.gov.hmcts.reform.fact.functional.helpers.AssertionHelper;
import uk.gov.hmcts.reform.fact.functional.helpers.TestDataHelper;
import uk.gov.hmcts.reform.fact.functional.http.HttpClient;

import java.time.ZonedDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.HttpStatus.CREATED;
import static org.springframework.http.HttpStatus.NO_CONTENT;
import static org.springframework.http.HttpStatus.NOT_FOUND;
import static org.springframework.http.HttpStatus.OK;

@Feature("Court Accessibility Options Controller")
@DisplayName("Court Accessibility Options Controller")
public final class CourtAccessibilityOptionsControllerFunctionalTest {

    private static final HttpClient http = new HttpClient();
    private static final ObjectMapper mapper = new ObjectMapper()
        .registerModule(new JavaTimeModule())
        .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

    @Test
    @DisplayName("POST /courts/{courtId}/v1/accessibility-options creates options and verifies persistence")
    void shouldCreateAccessibilityOptionsAndVerifyPersistence() throws Exception {
        final UUID courtId = TestDataHelper.createCourt(http, "Test Court Accessibility Options");

        final ZonedDateTime timestampBeforeCreate = AssertionHelper.getCourtLastUpdatedAt(http, courtId);

        final CourtAccessibilityOptions accessibilityOptions = CourtAccessibilityOptions.builder()
            .courtId(courtId)
            .accessibleParking(true)
            .accessibleParkingPhoneNumber("+44 20 7946 0958")
            .accessibleToiletDescription("Accessible toilets on ground floor")
            .accessibleEntrance(true)
            .accessibleEntrancePhoneNumber("020 7946 0959")
            .hearingEnhancementEquipment(HearingEnhancementEquipment.HEARING_LOOP_SYSTEMS)
            .lift(true)
            .liftDoorWidth(90)
            .liftDoorLimit(500)
            .quietRoom(true)
            .build();

        final Response createResponse = http.doPost(
            "/courts/" + courtId + "/v1/accessibility-options",
            accessibilityOptions
        );

        AssertionHelper.assertStatus(createResponse, CREATED);

        final CourtAccessibilityOptions createdOptions = mapper.readValue(
            createResponse.getBody().asString(),
            CourtAccessibilityOptions.class
        );

        assertThat(createdOptions.getId())
            .as("Created accessibility options should have an ID assigned")
            .isNotNull();

        final Response getResponse = http.doGet("/courts/" + courtId + "/v1/accessibility-options");

        AssertionHelper.assertStatus(getResponse, OK);

        final CourtAccessibilityOptions fetchedOptions = mapper.readValue(
            getResponse.getBody().asString(),
            CourtAccessibilityOptions.class
        );

        assertThat(fetchedOptions.getCourtId())
            .as("Fetched accessibility options should have correct court ID")
            .isEqualTo(courtId);
        assertThat(fetchedOptions.getAccessibleParking())
            .as("Accessible parking should be true")
            .isTrue();
        assertThat(fetchedOptions.getAccessibleParkingPhoneNumber())
            .as("Accessible parking phone number should match")
            .isEqualTo("+44 20 7946 0958");
        assertThat(fetchedOptions.getHearingEnhancementEquipment())
            .as("Hearing enhancement equipment should match")
            .isEqualTo(HearingEnhancementEquipment.HEARING_LOOP_SYSTEMS);
        assertThat(fetchedOptions.getLiftDoorWidth())
            .as("Lift door width should match")
            .isEqualTo(90);

        final ZonedDateTime timestampAfterCreate = AssertionHelper.getCourtLastUpdatedAt(http, courtId);
        assertThat(timestampAfterCreate)
            .as("Court lastUpdatedAt should move forward after accessibility options creation for court %s", courtId)
            .isAfter(timestampBeforeCreate);
    }

    @Test
    @DisplayName("POST /courts/{courtId}/v1/accessibility-options updates existing options")
    void shouldUpdateExistingAccessibilityOptions() throws Exception {
        final UUID courtId = TestDataHelper.createCourt(http, "Test Court Accessibility Update");

        final CourtAccessibilityOptions initialOptions = CourtAccessibilityOptions.builder()
            .courtId(courtId)
            .accessibleParking(false)
            .accessibleEntrance(false)
            .hearingEnhancementEquipment(HearingEnhancementEquipment.INFRARED_SYSTEMS)
            .lift(false)
            .quietRoom(false)
            .build();

        final Response createResponse = http.doPost(
            "/courts/" + courtId + "/v1/accessibility-options",
            initialOptions
        );

        AssertionHelper.assertStatus(createResponse, CREATED);

        final CourtAccessibilityOptions updatedOptions = CourtAccessibilityOptions.builder()
            .courtId(courtId)
            .accessibleParking(true)
            .accessibleParkingPhoneNumber("+44 161 496 0123")
            .accessibleEntrance(true)
            .accessibleEntrancePhoneNumber("0117 496 0456")
            .hearingEnhancementEquipment(HearingEnhancementEquipment.INFRARED_SYSTEMS_AND_HEARING_LOOP_SYSTEMS)
            .lift(true)
            .liftDoorWidth(100)
            .liftDoorLimit(800)
            .quietRoom(true)
            .build();

        final Response updateResponse = http.doPost(
            "/courts/" + courtId + "/v1/accessibility-options",
            updatedOptions
        );

        AssertionHelper.assertStatus(updateResponse, CREATED);

        final Response getResponse = http.doGet("/courts/" + courtId + "/v1/accessibility-options");

        AssertionHelper.assertStatus(getResponse, OK);

        final CourtAccessibilityOptions fetchedOptions = mapper.readValue(
            getResponse.getBody().asString(),
            CourtAccessibilityOptions.class
        );

        assertThat(fetchedOptions.getAccessibleParking())
            .as("Accessible parking should be updated to true")
            .isTrue();
        assertThat(fetchedOptions.getAccessibleParkingPhoneNumber())
            .as("Accessible parking phone number should be updated")
            .isEqualTo("+44 161 496 0123");
        assertThat(fetchedOptions.getHearingEnhancementEquipment())
            .as("Hearing enhancement equipment should be updated")
            .isEqualTo(HearingEnhancementEquipment.INFRARED_SYSTEMS_AND_HEARING_LOOP_SYSTEMS);
        assertThat(fetchedOptions.getLiftDoorWidth())
            .as("Lift door width should be updated")
            .isEqualTo(100);
    }

    @Test
    @DisplayName("GET /courts/{courtId}/v1/accessibility-options returns 404 for non-existent court")
    void shouldReturn404WhenGettingAccessibilityOptionsForNonExistentCourt() {
        final UUID nonExistentCourtId = UUID.randomUUID();

        final Response response = http.doGet("/courts/" + nonExistentCourtId + "/v1/accessibility-options");

        AssertionHelper.assertStatus(response, NOT_FOUND);
    }

    @Test
    @DisplayName("POST /courts/{courtId}/v1/accessibility-options returns 404 for non-existent court")
    void shouldReturn404WhenCreatingAccessibilityOptionsForNonExistentCourt() {
        final UUID nonExistentCourtId = UUID.randomUUID();

        final CourtAccessibilityOptions accessibilityOptions = CourtAccessibilityOptions.builder()
            .courtId(nonExistentCourtId)
            .accessibleParking(false)
            .accessibleEntrance(false)
            .hearingEnhancementEquipment(HearingEnhancementEquipment.HEARING_LOOP_SYSTEMS)
            .lift(false)
            .quietRoom(false)
            .build();

        final Response response = http.doPost(
            "/courts/" + nonExistentCourtId + "/v1/accessibility-options",
            accessibilityOptions
        );

        AssertionHelper.assertStatus(response, NOT_FOUND);
    }

    @Test
    @DisplayName("GET /courts/{courtId}/v1/accessibility-options returns 204 for court without options")
    void shouldReturn204WhenGettingAccessibilityOptionsForCourtWithoutOptions() {
        final UUID courtId = TestDataHelper.createCourt(http, "Test Court No Accessibility Options");

        final Response response = http.doGet("/courts/" + courtId + "/v1/accessibility-options");

        AssertionHelper.assertStatus(response, NO_CONTENT);
    }

    @AfterAll
    static void cleanUpTestData() {
        http.doDelete("/testing-support/courts/name-prefix/Test Court");
    }
}
