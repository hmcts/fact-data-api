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
import uk.gov.hmcts.reform.fact.functional.helpers.TestDataHelper;
import uk.gov.hmcts.reform.fact.functional.http.HttpClient;

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
        assertThat(getResponse.statusCode()).isEqualTo(OK.value());

        final List<AreaOfLawSelectionDto> areasOfLaw = mapper.readValue(
            getResponse.asString(),
            new TypeReference<List<AreaOfLawSelectionDto>>() {}
        );

        assertThat(areasOfLaw).hasSize(4);
        assertThat(areasOfLaw).allMatch(area -> area.getSelected().equals(false));
        assertThat(areasOfLaw).extracting(AreaOfLawSelectionDto::getName)
            .containsExactlyInAnyOrder("Adoption", "Children", "Civil partnership", "Divorce");
        assertThat(areasOfLaw).allMatch(area -> area.getId() != null);
        assertThat(areasOfLaw).allMatch(area -> area.getName() != null);
        assertThat(areasOfLaw).allMatch(area -> area.getNameCy() != null);
    }

    @Test
    @DisplayName("PUT /courts/{courtId}/v1/single-point-of-entry updates and persists selected areas of law")
    void shouldUpdateSinglePointsOfEntry() throws Exception {
        final UUID courtId = TestDataHelper.createCourt(http, "Test Court SPOE Update");

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
        assertThat(putResponse.statusCode()).isEqualTo(OK.value());

        final Response getResponse = http.doGet("/courts/" + courtId + "/v1/single-point-of-entry");
        assertThat(getResponse.statusCode()).isEqualTo(OK.value());

        final List<AreaOfLawSelectionDto> updatedAreasOfLaw = mapper.readValue(
            getResponse.asString(),
            new TypeReference<List<AreaOfLawSelectionDto>>() {}
        );

        assertThat(updatedAreasOfLaw).hasSize(4);

        assertThat(updatedAreasOfLaw)
            .filteredOn(area -> "Adoption".equals(area.getName()))
            .extracting(AreaOfLawSelectionDto::getSelected)
            .containsExactly(true);

        assertThat(updatedAreasOfLaw)
            .filteredOn(area -> "Children".equals(area.getName()))
            .extracting(AreaOfLawSelectionDto::getSelected)
            .containsExactly(true);

        assertThat(updatedAreasOfLaw)
            .filteredOn(area -> "Civil partnership".equals(area.getName()))
            .extracting(AreaOfLawSelectionDto::getSelected)
            .containsExactly(false);

        assertThat(updatedAreasOfLaw)
            .filteredOn(area -> "Divorce".equals(area.getName()))
            .extracting(AreaOfLawSelectionDto::getSelected)
            .containsExactly(false);
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
        assertThat(updateResponse.statusCode()).isEqualTo(OK.value());

        final Response finalGetResponse = http.doGet("/courts/" + courtId + "/v1/single-point-of-entry");
        final List<AreaOfLawSelectionDto> finalAreasOfLaw = mapper.readValue(
            finalGetResponse.asString(),
            new TypeReference<List<AreaOfLawSelectionDto>>() {}
        );

        assertThat(finalAreasOfLaw)
            .filteredOn(area -> "Adoption".equals(area.getName()))
            .extracting(AreaOfLawSelectionDto::getSelected)
            .containsExactly(false);

        assertThat(finalAreasOfLaw)
            .filteredOn(area -> "Divorce".equals(area.getName()))
            .extracting(AreaOfLawSelectionDto::getSelected)
            .containsExactly(true);

        assertThat(finalAreasOfLaw)
            .filteredOn(area -> "Civil partnership".equals(area.getName()))
            .extracting(AreaOfLawSelectionDto::getSelected)
            .containsExactly(true);

        assertThat(finalAreasOfLaw)
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
        assertThat(getResponse.statusCode()).isEqualTo(OK.value());

        final List<AreaOfLawSelectionDto> configuredAreasOfLaw = mapper.readValue(
            getResponse.asString(),
            new TypeReference<List<AreaOfLawSelectionDto>>() {}
        );

        assertThat(configuredAreasOfLaw).hasSize(4);

        assertThat(configuredAreasOfLaw)
            .filteredOn(area -> "Adoption".equals(area.getName()))
            .extracting(AreaOfLawSelectionDto::getSelected)
            .containsExactly(true);

        assertThat(configuredAreasOfLaw)
            .filteredOn(area -> "Divorce".equals(area.getName()))
            .extracting(AreaOfLawSelectionDto::getSelected)
            .containsExactly(true);

        assertThat(configuredAreasOfLaw)
            .filteredOn(area -> "Children".equals(area.getName()))
            .extracting(AreaOfLawSelectionDto::getSelected)
            .containsExactly(false);

        assertThat(configuredAreasOfLaw)
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
        assertThat(putResponse.statusCode()).isEqualTo(OK.value());

        final Response getResponse = http.doGet("/courts/" + courtId + "/v1/single-point-of-entry");
        final List<AreaOfLawSelectionDto> clearedAreasOfLaw = mapper.readValue(
            getResponse.asString(),
            new TypeReference<List<AreaOfLawSelectionDto>>() {}
        );

        assertThat(clearedAreasOfLaw).hasSize(4);
        assertThat(clearedAreasOfLaw).allMatch(area -> area.getSelected().equals(false));
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
        assertThat(putResponse.statusCode()).isEqualTo(BAD_REQUEST.value());
        assertThat(putResponse.jsonPath().getString("message"))
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
        assertThat(putResponse.statusCode()).isEqualTo(BAD_REQUEST.value());
        assertThat(putResponse.jsonPath().getString("message"))
            .contains("Invalid Area(s) of Law specified in Single Points of Entry configuration");
    }

    @AfterAll
    static void cleanUpTestData() {
        http.doDelete("/testing-support/courts/name-prefix/Test Court");
    }
}
