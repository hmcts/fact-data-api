package uk.gov.hmcts.reform.fact.functional.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import io.qameta.allure.Feature;
import io.restassured.response.Response;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import uk.gov.hmcts.reform.fact.data.api.entities.Court;
import uk.gov.hmcts.reform.fact.functional.helpers.AssertionHelper;
import uk.gov.hmcts.reform.fact.functional.helpers.TestDataHelper;
import uk.gov.hmcts.reform.fact.functional.http.HttpClient;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.CREATED;
import static org.springframework.http.HttpStatus.NOT_FOUND;
import static org.springframework.http.HttpStatus.OK;

@Feature("Court Controller")
@DisplayName("Court Controller")
public final class CourtControllerFunctionalTest {

    private static final HttpClient http = new HttpClient();
    private static final ObjectMapper mapper = new ObjectMapper()
        .registerModule(new JavaTimeModule())
        .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    private static final String regionId = TestDataHelper.getRegionId(http);

    @Test
    @DisplayName("POST /courts/v1 creates court and verifies persistence")
    void shouldCreateCourtWithValidPayload() throws Exception {
        final Court court = new Court();
        court.setName("Test Court Create Valid");
        court.setRegionId(UUID.fromString(regionId));
        court.setIsServiceCentre(true);

        final Response createResponse = http.doPost("/courts/v1", court);

        assertThat(createResponse.statusCode()).isEqualTo(CREATED.value());
        final String courtId = createResponse.jsonPath().getString("id");
        assertThat(courtId).isNotNull();

        final Response getResponse = http.doGet("/courts/" + courtId + "/v1");
        assertThat(getResponse.statusCode()).isEqualTo(OK.value());

        final Court fetchedCourt = mapper.readValue(getResponse.getBody().asString(), Court.class);
        assertThat(fetchedCourt.getName()).isEqualTo("Test Court Create Valid");
        assertThat(fetchedCourt.getRegionId()).isEqualTo(UUID.fromString(regionId));
        assertThat(fetchedCourt.getIsServiceCentre()).isTrue();
    }

    @Test
    @DisplayName("POST /courts/v1 fails with non-existent region ID")
    void shouldFailWithNonExistentRegionId() throws Exception {
        final Court court = new Court();
        court.setName("Test Court Invalid Region");
        court.setRegionId(UUID.randomUUID());
        court.setIsServiceCentre(true);

        final Response response = http.doPost("/courts/v1", court);

        assertThat(response.statusCode()).isEqualTo(NOT_FOUND.value());
        assertThat(response.jsonPath().getString("message"))
            .contains("Region not found, ID: " + court.getRegionId());
    }

    @Test
    @DisplayName("POST /courts/v1 fails when name is missing")
    void shouldFailWithMissingName() throws Exception {
        final Court court = new Court();
        court.setRegionId(UUID.fromString(regionId));
        court.setIsServiceCentre(true);

        final Response response = http.doPost("/courts/v1", court);

        assertThat(response.statusCode()).isEqualTo(BAD_REQUEST.value());
        assertThat(response.jsonPath().getString("name"))
            .contains("Court name must be specified");
    }

    @Test
    @DisplayName("GET /courts/{courtId}/v1 fails with non-existent court ID")
    void shouldFailToRetrieveNonExistentCourt() {
        final String nonExistentCourtId = UUID.randomUUID().toString();

        final Response response = http.doGet("/courts/" + nonExistentCourtId + "/v1");

        assertThat(response.statusCode()).isEqualTo(NOT_FOUND.value());
        assertThat(response.jsonPath().getString("message"))
            .contains("Court not found, ID: " + nonExistentCourtId);
    }

    @Test
    @DisplayName("PUT /courts/{courtId}/v1 updates existing court and verifies changes")
    void shouldUpdateExistingCourt() throws Exception {
        final Court court = new Court();
        court.setName("Test Court Original");
        court.setRegionId(UUID.fromString(regionId));
        court.setIsServiceCentre(true);

        final Response createResponse = http.doPost("/courts/v1", court);
        final String courtId = createResponse.jsonPath().getString("id");

        final Court updatedCourt = new Court();
        updatedCourt.setName("Test Court Updated");
        updatedCourt.setRegionId(UUID.fromString(regionId));
        updatedCourt.setIsServiceCentre(true);

        final Response updateResponse = http.doPut("/courts/" + courtId + "/v1", updatedCourt);

        assertThat(updateResponse.statusCode()).isEqualTo(OK.value());

        final Response getResponse = http.doGet("/courts/" + courtId + "/v1");
        final Court fetchedCourt = mapper.readValue(getResponse.getBody().asString(), Court.class);
        assertThat(fetchedCourt.getName()).isEqualTo("Test Court Updated");
        assertThat(fetchedCourt.getIsServiceCentre()).isTrue();
    }

    @Test
    @DisplayName("PUT /courts/{courtId}/v1 fails with non-existent court ID")
    void shouldFailToUpdateNonExistentCourt() throws Exception {
        final String nonExistentCourtId = UUID.randomUUID().toString();

        final Court updatedCourt = new Court();
        updatedCourt.setName("Test Court Non-Existent court ID");
        updatedCourt.setRegionId(UUID.fromString(regionId));
        updatedCourt.setIsServiceCentre(true);

        final Response response = http.doPut("/courts/" + nonExistentCourtId + "/v1", updatedCourt);

        assertThat(response.statusCode()).isEqualTo(NOT_FOUND.value());
        assertThat(response.jsonPath().getString("message"))
            .contains("Court not found, ID: " + nonExistentCourtId);
    }

    @Test
    @DisplayName("PUT /courts/{courtId}/v1 update fails with non-existent regionId")
    void shouldFailToUpdateCourtWithNonExistentRegionId() throws Exception {
        final Court court = new Court();
        court.setName("Update Test Court Non-Existent Region ID");
        court.setRegionId(UUID.fromString(regionId));
        court.setIsServiceCentre(true);

        final Response createResponse = http.doPost("/courts/v1", court);
        final String courtId = createResponse.jsonPath().getString("id");

        final Court updatedCourt = new Court();
        updatedCourt.setName("Test Court Non-Existent Region ID Updated");
        updatedCourt.setRegionId(UUID.randomUUID());
        updatedCourt.setIsServiceCentre(true);

        final Response response = http.doPut("/courts/" + courtId + "/v1", updatedCourt);

        assertThat(response.statusCode()).isEqualTo(NOT_FOUND.value());
        assertThat(response.jsonPath().getString("message"))
            .contains("Region not found, ID: " + updatedCourt.getRegionId());
    }

    @Test
    @DisplayName("GET /courts/v1 without params returns paginated structure")
    void shouldReturnPaginatedStructureWithoutParams() {
        final Response response = http.doGet("/courts/v1");

        assertThat(response.statusCode()).isEqualTo(OK.value());
        assertThat(response.contentType()).contains("json");
        assertThat(response.jsonPath().getMap("page")).isNotNull();
        assertThat(response.jsonPath().getList("content")).isNotNull();
    }

    @Test
    @DisplayName("GET /courts/v1 with filters returns created court in list")
    void shouldReturnCreatedCourtInFilteredList() throws Exception {
        final Court court = new Court();
        court.setName("Test Court for List Retrieval");
        court.setRegionId(UUID.fromString(regionId));
        court.setIsServiceCentre(true);

        final Response createResponse = http.doPost("/courts/v1", court);
        assertThat(createResponse.statusCode()).isEqualTo(CREATED.value());
        final UUID courtId = UUID.fromString(createResponse.jsonPath().getString("id"));

        final Response listResponse = http.doGet("/courts/v1?pageNumber=0&pageSize=200&includeClosed=true");

        AssertionHelper.assertCourtIdInListResponse(listResponse, courtId);
    }

    @Test
    @DisplayName("GET /courts/v1 with includeClosed=false returns only open courts")
    void shouldReturnOnlyActiveCourts() throws Exception {
        final Court court = new Court();
        court.setName("Test Open Court");
        court.setRegionId(UUID.fromString(regionId));
        court.setIsServiceCentre(true);

        final Response createResponse = http.doPost("/courts/v1", court);
        assertThat(createResponse.statusCode()).isEqualTo(CREATED.value());
        final UUID courtId = UUID.fromString(createResponse.jsonPath().getString("id"));

        final Court updatedCourt = new Court();
        updatedCourt.setName("Test Open Court");
        updatedCourt.setRegionId(UUID.fromString(regionId));
        updatedCourt.setIsServiceCentre(true);
        updatedCourt.setOpen(true);

        final Response updateResponse = http.doPut("/courts/" + courtId + "/v1", updatedCourt);
        assertThat(updateResponse.statusCode()).isEqualTo(OK.value());

        final Response listResponse = http.doGet(
            "/courts/v1?pageNumber=0&pageSize=200&includeClosed=false"
        );

        AssertionHelper.assertCourtIdInListResponse(listResponse, courtId);
    }

    @Test
    @DisplayName("GET /courts/v1 filtered only by valid region id")
    void shouldReturnCourtsFilteredByValidRegionId() throws Exception {
        final Court court = new Court();
        court.setName("Test Court for regionId filtering");
        court.setRegionId(UUID.fromString(regionId));
        court.setIsServiceCentre(true);

        final Response createResponse = http.doPost("/courts/v1", court);
        assertThat(createResponse.statusCode()).isEqualTo(CREATED.value());
        final UUID courtId = UUID.fromString(createResponse.jsonPath().getString("id"));

        final Response listResponse = http.doGet(
            "/courts/v1?pageNumber=0&pageSize=200&includeClosed=true&regionId=" + regionId
        );

        AssertionHelper.assertCourtIdInListResponse(listResponse, courtId);
    }

    @Test
    @DisplayName("GET /courts/v1 filtered by partialCourtName")
    void shouldReturnCourtsFilteredByPartialCourtName() throws Exception {
        final Court court = new Court();
        court.setName("Functional Test Birmingham Court");
        court.setRegionId(UUID.fromString(regionId));
        court.setIsServiceCentre(true);

        final Response createResponse = http.doPost("/courts/v1", court);
        assertThat(createResponse.statusCode()).isEqualTo(CREATED.value());
        final UUID courtId = UUID.fromString(createResponse.jsonPath().getString("id"));

        final Response listResponse = http.doGet(
            "/courts/v1?pageNumber=0&pageSize=200&includeClosed=true&partialCourtName=Birmingham"
        );

        AssertionHelper.assertCourtIdInListResponse(listResponse, courtId);
    }

    @Test
    @DisplayName("GET /courts/v1 filtered by combined filters (regionId + partialCourtName + includeClosed)")
    void shouldReturnCourtsFilteredByCombinedFilters() throws Exception {
        final Court court = new Court();
        court.setName("Functional Test Manchester Court");
        court.setRegionId(UUID.fromString(regionId));
        court.setIsServiceCentre(true);

        final Response createResponse = http.doPost("/courts/v1", court);
        assertThat(createResponse.statusCode()).isEqualTo(CREATED.value());
        final UUID courtId = UUID.fromString(createResponse.jsonPath().getString("id"));

        final Response listResponse = http.doGet(
            "/courts/v1?pageNumber=0&pageSize=200&includeClosed=true&regionId=" + regionId
                + "&partialCourtName=Manchester"
        );

        AssertionHelper.assertCourtIdInListResponse(listResponse, courtId);
    }
}
