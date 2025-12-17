package uk.gov.hmcts.reform.fact.functional.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import io.qameta.allure.Feature;
import io.restassured.response.Response;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import uk.gov.hmcts.reform.fact.data.api.entities.CourtFacilities;
import uk.gov.hmcts.reform.fact.functional.helpers.TestDataHelper;
import uk.gov.hmcts.reform.fact.functional.http.HttpClient;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.CREATED;
import static org.springframework.http.HttpStatus.NOT_FOUND;
import static org.springframework.http.HttpStatus.NO_CONTENT;
import static org.springframework.http.HttpStatus.OK;

@Feature("Court Facilities Controller")
@DisplayName("Court Facilities Controller")
@Disabled
public final class CourtFacilitiesControllerFunctionalTest {

    private static final HttpClient http = new HttpClient();
    private static final ObjectMapper mapper = new ObjectMapper()
        .registerModule(new JavaTimeModule())
        .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

    @Test
    @DisplayName("POST /courts/{courtId}/v1/building-facilities with all valid fields")
    void shouldCreateFacilitiesWithAllValidFields() throws Exception {
        final UUID courtId = TestDataHelper.createCourt(http, "Test Court Facilities Full");

        final CourtFacilities facilities = TestDataHelper.buildFacilities(courtId);
        facilities.setFreeWaterDispensers(false);
        facilities.setDrinkVendingMachines(false);
        facilities.setWaitingAreaChildren(false);
        facilities.setBabyChanging(false);

        final Response postResponse = http.doPost("/courts/" + courtId + "/v1/building-facilities", facilities);
        assertThat(postResponse.statusCode()).isEqualTo(CREATED.value());

        final CourtFacilities createdFacilities = mapper.readValue(postResponse.asString(), CourtFacilities.class);
        assertThat(createdFacilities.getId()).isNotNull();
        assertThat(createdFacilities.getCourtId()).isEqualTo(courtId);
        assertThat(createdFacilities.getParking()).isTrue();
        assertThat(createdFacilities.getFreeWaterDispensers()).isFalse();
        assertThat(createdFacilities.getWifi()).isTrue();

        final Response getResponse = http.doGet("/courts/" + courtId + "/v1/building-facilities");
        assertThat(getResponse.statusCode()).isEqualTo(OK.value());

        final CourtFacilities retrievedFacilities = mapper.readValue(getResponse.asString(), CourtFacilities.class);
        assertThat(retrievedFacilities.getId()).isEqualTo(createdFacilities.getId());
        assertThat(retrievedFacilities.getCourtId()).isEqualTo(courtId);
        assertThat(retrievedFacilities.getParking()).isTrue();
        assertThat(retrievedFacilities.getFreeWaterDispensers()).isFalse();
        assertThat(retrievedFacilities.getWifi()).isTrue();
    }

    @Test
    @DisplayName("POST /courts/{courtId}/v1/building-facilities updates existing facilities")
    void shouldUpdateExistingFacilities() throws Exception {
        final UUID courtId = TestDataHelper.createCourt(http, "Test Court Facilities Update");

        final CourtFacilities initialFacilities = TestDataHelper.buildFacilities(courtId);

        final Response createResponse = http.doPost("/courts/" + courtId + "/v1/building-facilities",
                                                     initialFacilities);
        assertThat(createResponse.statusCode()).isEqualTo(CREATED.value());

        final CourtFacilities createdFacilities = mapper.readValue(createResponse.asString(), CourtFacilities.class);
        final UUID facilitiesId = createdFacilities.getId();

        final CourtFacilities updatedFacilities = TestDataHelper.buildFacilities(courtId);
        updatedFacilities.setParking(false);
        updatedFacilities.setFreeWaterDispensers(false);
        updatedFacilities.setSnackVendingMachines(false);
        updatedFacilities.setDrinkVendingMachines(false);
        updatedFacilities.setCafeteria(false);
        updatedFacilities.setWaitingArea(false);
        updatedFacilities.setWaitingAreaChildren(false);
        updatedFacilities.setQuietRoom(false);
        updatedFacilities.setBabyChanging(false);
        updatedFacilities.setWifi(false);

        final Response updateResponse = http.doPost("/courts/" + courtId + "/v1/building-facilities",
                                                     updatedFacilities);
        assertThat(updateResponse.statusCode()).isEqualTo(CREATED.value());

        final CourtFacilities modifiedFacilities = mapper.readValue(updateResponse.asString(), CourtFacilities.class);
        assertThat(modifiedFacilities.getId()).isEqualTo(facilitiesId);
        assertThat(modifiedFacilities.getCourtId()).isEqualTo(courtId);
        assertThat(modifiedFacilities.getParking()).isFalse();
        assertThat(modifiedFacilities.getWifi()).isFalse();

        final Response getResponse = http.doGet("/courts/" + courtId + "/v1/building-facilities");
        assertThat(getResponse.statusCode()).isEqualTo(OK.value());

        final CourtFacilities retrievedFacilities = mapper.readValue(getResponse.asString(), CourtFacilities.class);
        assertThat(retrievedFacilities.getId()).isEqualTo(facilitiesId);
        assertThat(retrievedFacilities.getParking()).isFalse();
        assertThat(retrievedFacilities.getWifi()).isFalse();
    }

    @Test
    @DisplayName("POST /courts/{courtId}/v1/building-facilities fails with non-existent court")
    void shouldFailToCreateFacilitiesForNonExistentCourt() {
        final UUID nonExistentCourtId = UUID.randomUUID();

        final CourtFacilities facilities = TestDataHelper.buildFacilities(nonExistentCourtId);

        final Response postResponse = http.doPost("/courts/" + nonExistentCourtId + "/v1/building-facilities",
                                                   facilities);
        assertThat(postResponse.statusCode()).isEqualTo(NOT_FOUND.value());
        assertThat(postResponse.jsonPath().getString("message"))
            .contains("Court not found, ID: " + nonExistentCourtId);
    }

    @Test
    @DisplayName("POST /courts/{courtId}/v1/building-facilities fails with null parking field")
    void shouldFailWithNullParkingField() {
        final UUID courtId = TestDataHelper.createCourt(http, "Test Court Facilities Null Parking");

        final CourtFacilities facilities = TestDataHelper.buildFacilities(courtId);
        facilities.setParking(null);

        final Response postResponse = http.doPost("/courts/" + courtId + "/v1/building-facilities", facilities);
        assertThat(postResponse.statusCode()).isEqualTo(BAD_REQUEST.value());
        assertThat(postResponse.jsonPath().getString("parking"))
            .contains("must not be null");
    }

    @Test
    @DisplayName("POST /courts/{courtId}/v1/building-facilities fails with invalid UUID")
    void shouldFailPostWithInvalidUuid() {
        final String invalidUuid = "invalid-uuid";

        final CourtFacilities facilities = TestDataHelper.buildFacilities(UUID.randomUUID());

        final Response postResponse = http.doPost("/courts/" + invalidUuid
                                                      + "/v1/building-facilities", facilities);
        assertThat(postResponse.statusCode()).isEqualTo(BAD_REQUEST.value());
        assertThat(postResponse.jsonPath().getString("message"))
            .contains("Invalid UUID supplied: " + invalidUuid);
    }

    @Test
    @DisplayName("GET /courts/{courtId}/v1/building-facilities returns 204 when no facilities exist")
    void shouldReturn204WhenNoFacilities() {
        final UUID courtId = TestDataHelper.createCourt(http, "Test Court No Facilities");

        final Response getResponse = http.doGet("/courts/" + courtId + "/v1/building-facilities");
        assertThat(getResponse.statusCode()).isEqualTo(NO_CONTENT.value());
    }

    @Test
    @DisplayName("GET /courts/{courtId}/v1/building-facilities fails with non-existent court")
    void shouldFailToGetFacilitiesForNonExistentCourt() {
        final UUID nonExistentCourtId = UUID.randomUUID();

        final Response getResponse = http.doGet("/courts/" + nonExistentCourtId + "/v1/building-facilities");
        assertThat(getResponse.statusCode()).isEqualTo(NOT_FOUND.value());
        assertThat(getResponse.jsonPath().getString("message"))
            .contains("Court not found, ID: " + nonExistentCourtId);
    }

    @Test
    @DisplayName("GET /courts/{courtId}/v1/building-facilities fails with invalid UUID")
    void shouldFailGetWithInvalidUuid() {
        final String invalidUuid = "invalid-uuid";

        final Response getResponse = http.doGet("/courts/" + invalidUuid + "/v1/building-facilities");
        assertThat(getResponse.statusCode()).isEqualTo(BAD_REQUEST.value());
        assertThat(getResponse.jsonPath().getString("message"))
            .contains("Invalid UUID supplied: " + invalidUuid);
    }

    @AfterAll
    static void cleanUpTestData() {
        http.doDelete("/testing-support/courts/name-prefix/Test Court");
    }
}
