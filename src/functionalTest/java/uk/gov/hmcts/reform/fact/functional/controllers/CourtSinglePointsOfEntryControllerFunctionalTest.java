package uk.gov.hmcts.reform.fact.functional.controllers;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import io.qameta.allure.Feature;
import io.restassured.response.Response;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import uk.gov.hmcts.reform.fact.data.api.models.AreaOfLawSelectionDto;
import uk.gov.hmcts.reform.fact.functional.helpers.AssertionHelper;
import uk.gov.hmcts.reform.fact.functional.helpers.TestDataHelper;
import uk.gov.hmcts.reform.fact.functional.http.HttpClient;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.OK;

@Feature("Court Single Points of Entry Controller")
@DisplayName("Court Single Points of Entry Controller")
public final class CourtSinglePointsOfEntryControllerFunctionalTest {

    private static final HttpClient http = new HttpClient();
    private static final ObjectMapper mapper = new ObjectMapper()
        .registerModule(new JavaTimeModule())
        .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

    @Test
    @DisplayName("GET /courts/{courtId}/v1/single-point-of-entry returns all areas of law unselected for new court")
    void shouldGetSinglePointsOfEntryForNewCourt() throws Exception {
        final UUID courtId = TestDataHelper.createCourt(http, "Test Court SPOE Get");

        final Response getResponse = http.doGet("/courts/" + courtId + "/v1/single-point-of-entry");

        AssertionHelper.assertStatus(getResponse, OK);

        final List<AreaOfLawSelectionDto> areasOfLaw = mapper.readValue(
            getResponse.asString(),
            new TypeReference<List<AreaOfLawSelectionDto>>() {}
        );

        assertThat(areasOfLaw)
            .as("Should return 4 areas of law")
            .hasSize(4);
        assertThat(areasOfLaw)
            .as("All areas should be unselected for new court")
            .allMatch(area -> area.getSelected().equals(false));
        assertThat(areasOfLaw)
            .as("Should contain expected areas of law")
            .extracting(AreaOfLawSelectionDto::getName)
            .containsExactlyInAnyOrder("Adoption", "Children", "Civil partnership", "Divorce");
        assertThat(areasOfLaw)
            .as("All areas should have an ID")
            .allMatch(area -> area.getId() != null);
        assertThat(areasOfLaw)
            .as("All areas should have a name")
            .allMatch(area -> area.getName() != null);
        assertThat(areasOfLaw)
            .as("All areas should have a Welsh name")
            .allMatch(area -> area.getNameCy() != null);
    }

    @Test
    @DisplayName("PUT /courts/{courtId}/v1/single-point-of-entry updates and persists selected areas of law")
    void shouldUpdateSinglePointsOfEntry() throws Exception {
        final UUID courtId = TestDataHelper.createCourt(http, "Test Court SPOE Update");

        final ZonedDateTime timestampBeforeUpdate = AssertionHelper.getCourtLastUpdatedAt(http, courtId);

        final Response initialGetResponse = http.doGet("/courts/" + courtId + "/v1/single-point-of-entry");
        final List<AreaOfLawSelectionDto> areasOfLaw = mapper.readValue(
            initialGetResponse.asString(),
            new TypeReference<List<AreaOfLawSelectionDto>>() {}
        );

        areasOfLaw.forEach(area -> {
            if ("Adoption".equals(area.getName()) || "Children".equals(area.getName())) {
                area.setSelected(true);
            } else {
                area.setSelected(false);
            }
        });

        final Response putResponse = http.doPut("/courts/" + courtId + "/v1/single-point-of-entry", areasOfLaw);

        AssertionHelper.assertStatus(putResponse, OK);

        final Response getResponse = http.doGet("/courts/" + courtId + "/v1/single-point-of-entry");

        AssertionHelper.assertStatus(getResponse, OK);

        final List<AreaOfLawSelectionDto> updatedAreasOfLaw = mapper.readValue(
            getResponse.asString(),
            new TypeReference<List<AreaOfLawSelectionDto>>() {}
        );

        assertThat(updatedAreasOfLaw)
            .as("Should return 4 areas of law")
            .hasSize(4);

        assertThat(updatedAreasOfLaw)
            .as("Adoption should be selected")
            .filteredOn(area -> "Adoption".equals(area.getName()))
            .extracting(AreaOfLawSelectionDto::getSelected)
            .containsExactly(true);

        assertThat(updatedAreasOfLaw)
            .as("Children should be selected")
            .filteredOn(area -> "Children".equals(area.getName()))
            .extracting(AreaOfLawSelectionDto::getSelected)
            .containsExactly(true);

        assertThat(updatedAreasOfLaw)
            .as("Civil partnership should not be selected")
            .filteredOn(area -> "Civil partnership".equals(area.getName()))
            .extracting(AreaOfLawSelectionDto::getSelected)
            .containsExactly(false);

        assertThat(updatedAreasOfLaw)
            .as("Divorce should not be selected")
            .filteredOn(area -> "Divorce".equals(area.getName()))
            .extracting(AreaOfLawSelectionDto::getSelected)
            .containsExactly(false);

        final ZonedDateTime timestampAfterUpdate = AssertionHelper.getCourtLastUpdatedAt(http, courtId);
        assertThat(timestampAfterUpdate)
            .as("Court lastUpdatedAt should move forward after SPOE update for court %s", courtId)
            .isAfter(timestampBeforeUpdate);
    }

    @Test
    @DisplayName("PUT /courts/{courtId}/v1/single-point-of-entry updates existing SPOE configuration")
    void shouldUpdateExistingSinglePointsOfEntry() throws Exception {
        final UUID courtId = TestDataHelper.createCourt(http, "Test Court SPOE Modify");

        final Response initialGetResponse = http.doGet("/courts/" + courtId + "/v1/single-point-of-entry");
        final List<AreaOfLawSelectionDto> initialAreasOfLaw = mapper.readValue(
            initialGetResponse.asString(),
            new TypeReference<List<AreaOfLawSelectionDto>>() {}
        );

        initialAreasOfLaw.forEach(area -> {
            if ("Adoption".equals(area.getName())) {
                area.setSelected(true);
            } else {
                area.setSelected(false);
            }
        });

        http.doPut("/courts/" + courtId + "/v1/single-point-of-entry", initialAreasOfLaw);

        final Response modifiedGetResponse = http.doGet("/courts/" + courtId + "/v1/single-point-of-entry");
        final List<AreaOfLawSelectionDto> modifiedAreasOfLaw = mapper.readValue(
            modifiedGetResponse.asString(),
            new TypeReference<List<AreaOfLawSelectionDto>>() {}
        );

        modifiedAreasOfLaw.forEach(area -> {
            if ("Divorce".equals(area.getName()) || "Civil partnership".equals(area.getName())) {
                area.setSelected(true);
            } else {
                area.setSelected(false);
            }
        });

        final Response updateResponse = http.doPut("/courts/" + courtId
                                                       + "/v1/single-point-of-entry", modifiedAreasOfLaw);

        AssertionHelper.assertStatus(updateResponse, OK);

        final Response finalGetResponse = http.doGet("/courts/" + courtId + "/v1/single-point-of-entry");
        final List<AreaOfLawSelectionDto> finalAreasOfLaw = mapper.readValue(
            finalGetResponse.asString(),
            new TypeReference<List<AreaOfLawSelectionDto>>() {}
        );

        assertThat(finalAreasOfLaw)
            .as("Adoption should now be unselected")
            .filteredOn(area -> "Adoption".equals(area.getName()))
            .extracting(AreaOfLawSelectionDto::getSelected)
            .containsExactly(false);

        assertThat(finalAreasOfLaw)
            .as("Divorce should now be selected")
            .filteredOn(area -> "Divorce".equals(area.getName()))
            .extracting(AreaOfLawSelectionDto::getSelected)
            .containsExactly(true);

        assertThat(finalAreasOfLaw)
            .as("Civil partnership should now be selected")
            .filteredOn(area -> "Civil partnership".equals(area.getName()))
            .extracting(AreaOfLawSelectionDto::getSelected)
            .containsExactly(true);

        assertThat(finalAreasOfLaw)
            .as("Children should remain unselected")
            .filteredOn(area -> "Children".equals(area.getName()))
            .extracting(AreaOfLawSelectionDto::getSelected)
            .containsExactly(false);
    }

    @Test
    @DisplayName("GET /courts/{courtId}/v1/single-point-of-entry returns configured areas of law for existing SPOE")
    void shouldGetSinglePointsOfEntryWithExistingConfiguration() throws Exception {
        final UUID courtId = TestDataHelper.createCourt(http, "Test Court SPOE Existing");

        final Response initialGetResponse = http.doGet("/courts/" + courtId + "/v1/single-point-of-entry");
        final List<AreaOfLawSelectionDto> areasOfLaw = mapper.readValue(
            initialGetResponse.asString(),
            new TypeReference<List<AreaOfLawSelectionDto>>() {}
        );

        areasOfLaw.forEach(area -> {
            if ("Adoption".equals(area.getName()) || "Divorce".equals(area.getName())) {
                area.setSelected(true);
            } else {
                area.setSelected(false);
            }
        });

        http.doPut("/courts/" + courtId + "/v1/single-point-of-entry", areasOfLaw);

        final Response getResponse = http.doGet("/courts/" + courtId + "/v1/single-point-of-entry");

        AssertionHelper.assertStatus(getResponse, OK);

        final List<AreaOfLawSelectionDto> configuredAreasOfLaw = mapper.readValue(
            getResponse.asString(),
            new TypeReference<List<AreaOfLawSelectionDto>>() {}
        );

        assertThat(configuredAreasOfLaw)
            .as("Should return 4 areas of law")
            .hasSize(4);

        assertThat(configuredAreasOfLaw)
            .as("Adoption should be selected")
            .filteredOn(area -> "Adoption".equals(area.getName()))
            .extracting(AreaOfLawSelectionDto::getSelected)
            .containsExactly(true);

        assertThat(configuredAreasOfLaw)
            .as("Divorce should be selected")
            .filteredOn(area -> "Divorce".equals(area.getName()))
            .extracting(AreaOfLawSelectionDto::getSelected)
            .containsExactly(true);

        assertThat(configuredAreasOfLaw)
            .as("Children should not be selected")
            .filteredOn(area -> "Children".equals(area.getName()))
            .extracting(AreaOfLawSelectionDto::getSelected)
            .containsExactly(false);

        assertThat(configuredAreasOfLaw)
            .as("Civil partnership should not be selected")
            .filteredOn(area -> "Civil partnership".equals(area.getName()))
            .extracting(AreaOfLawSelectionDto::getSelected)
            .containsExactly(false);
    }

    @Test
    @DisplayName("PUT /courts/{courtId}/v1/single-point-of-entry with all unselected clears configuration")
    void shouldClearSinglePointsOfEntryWhenAllUnselected() throws Exception {
        final UUID courtId = TestDataHelper.createCourt(http, "Test Court SPOE Clear");

        final Response initialGetResponse = http.doGet("/courts/" + courtId + "/v1/single-point-of-entry");
        final List<AreaOfLawSelectionDto> areasOfLaw = mapper.readValue(
            initialGetResponse.asString(),
            new TypeReference<List<AreaOfLawSelectionDto>>() {}
        );

        areasOfLaw.forEach(area -> area.setSelected(true));
        http.doPut("/courts/" + courtId + "/v1/single-point-of-entry", areasOfLaw);

        areasOfLaw.forEach(area -> area.setSelected(false));

        final Response putResponse = http.doPut("/courts/" + courtId + "/v1/single-point-of-entry", areasOfLaw);

        AssertionHelper.assertStatus(putResponse, OK);

        final Response getResponse = http.doGet("/courts/" + courtId + "/v1/single-point-of-entry");
        final List<AreaOfLawSelectionDto> clearedAreasOfLaw = mapper.readValue(
            getResponse.asString(),
            new TypeReference<List<AreaOfLawSelectionDto>>() {}
        );

        assertThat(clearedAreasOfLaw)
            .as("Should return 4 areas of law")
            .hasSize(4);
        assertThat(clearedAreasOfLaw)
            .as("All areas should be unselected after clearing")
            .allMatch(area -> area.getSelected().equals(false));
    }

    @Test
    @DisplayName("PUT /courts/{courtId}/v1/single-point-of-entry fails with duplicate area IDs")
    void shouldFailWithDuplicateAreaIds() throws Exception {
        final UUID courtId = TestDataHelper.createCourt(http, "Test Court SPOE Duplicate");

        final Response initialGetResponse = http.doGet("/courts/" + courtId + "/v1/single-point-of-entry");
        final List<AreaOfLawSelectionDto> areasOfLaw = mapper.readValue(
            initialGetResponse.asString(),
            new TypeReference<List<AreaOfLawSelectionDto>>() {}
        );

        final AreaOfLawSelectionDto adoption = areasOfLaw.stream()
            .filter(area -> "Adoption".equals(area.getName()))
            .findFirst()
            .orElseThrow();
        adoption.setSelected(true);

        final AreaOfLawSelectionDto duplicateAdoption = AreaOfLawSelectionDto.builder()
            .id(adoption.getId())
            .name("Adoption")
            .nameCy(adoption.getNameCy())
            .selected(true)
            .build();

        areasOfLaw.add(duplicateAdoption);

        final Response putResponse = http.doPut("/courts/" + courtId + "/v1/single-point-of-entry", areasOfLaw);

        AssertionHelper.assertStatus(putResponse, BAD_REQUEST);

        assertThat(putResponse.jsonPath().getString("message"))
            .as("Error message should indicate duplicate area of law")
            .contains("Duplicated Area of Law in selection");
    }

    @Test
    @DisplayName("PUT /courts/{courtId}/v1/single-point-of-entry fails with invalid area of law")
    void shouldFailWithInvalidAreaOfLaw() throws Exception {
        final UUID courtId = TestDataHelper.createCourt(http, "Test Court SPOE Invalid Area");

        final Response initialGetResponse = http.doGet("/courts/" + courtId + "/v1/single-point-of-entry");
        final List<AreaOfLawSelectionDto> areasOfLaw = mapper.readValue(
            initialGetResponse.asString(),
            new TypeReference<List<AreaOfLawSelectionDto>>() {}
        );

        final AreaOfLawSelectionDto invalidArea = AreaOfLawSelectionDto.builder()
            .id(UUID.randomUUID())
            .name("Invalid Area")
            .nameCy("Invalid Area Welsh")
            .selected(true)
            .build();

        areasOfLaw.add(invalidArea);

        final Response putResponse = http.doPut("/courts/" + courtId + "/v1/single-point-of-entry", areasOfLaw);

        AssertionHelper.assertStatus(putResponse, BAD_REQUEST);

        assertThat(putResponse.jsonPath().getString("message"))
            .as("Error message should indicate invalid area of law")
            .contains("Invalid Area(s) of Law specified in Single Points of Entry configuration");
    }

    @AfterAll
    static void cleanUpTestData() {
        http.doDelete("/testing-support/courts/name-prefix/Test Court");
    }
}
