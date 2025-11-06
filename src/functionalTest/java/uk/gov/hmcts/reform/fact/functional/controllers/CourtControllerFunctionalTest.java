package uk.gov.hmcts.reform.fact.functional.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import io.qameta.allure.Feature;
import io.restassured.response.Response;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import uk.gov.hmcts.reform.fact.data.api.entities.Court;
import uk.gov.hmcts.reform.fact.functional.data.CourtTestData;
import uk.gov.hmcts.reform.fact.functional.http.HttpClient;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.HttpStatus.CREATED;
import static org.springframework.http.HttpStatus.OK;

@Feature("Court Controller")
@DisplayName("Court Controller")
public final class CourtControllerFunctionalTest {

    private static final HttpClient http = new HttpClient();
    private static final ObjectMapper mapper = new ObjectMapper()
        .registerModule(new JavaTimeModule())
        .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    private static final String regionId = getRegionId();

    private static String getRegionId() {
        final Response response = http.doGet("/types/v1/regions");
        return response.jsonPath().getString("[0].id");
    }

    @Test
    @DisplayName("POST /courts/v1 creates court with valid payload")
    void shouldCreateCourtWithValidPayload() throws Exception {
        final CourtTestData court = new CourtTestData();
        court.setName("Functional Test Court I");
        court.setRegionId(regionId);
        court.setIsServiceCentre(true);

        final String json = mapper.writeValueAsString(court);
        final Response response = http.doPost("/courts/v1", json);

        assertThat(response.statusCode()).isEqualTo(CREATED.value());
        assertThat(response.jsonPath().getString("id")).isNotNull();
    }

    @Test
    @DisplayName("Example create court test")
    void shouldCreateCourt() throws Exception {
        Court court = new Court();
        court.setName("Functional Test Court test");
        court.setRegionId(UUID.fromString(regionId));
        court.setIsServiceCentre(true);

        Response createResponse = http.doPost("/courts/v1", court);
        assertThat(createResponse.statusCode()).isEqualTo(CREATED.value());

        String id = createResponse.jsonPath().getString("id");
        assertThat(id).isNotNull();

        Response getResponse = http.doGet("/courts/" + id + "/v1");
        assertThat(getResponse.statusCode()).isEqualTo(OK.value());

        Court fetchedCourt = mapper.readValue(getResponse.getBody().asString(), Court.class);

        assertThat(fetchedCourt.getName()).isEqualTo(court.getName());
        assertThat(fetchedCourt.getRegionId()).isEqualTo(court.getRegionId());
        assertThat(fetchedCourt.getIsServiceCentre()).isTrue();
    }
}
