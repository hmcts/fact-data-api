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
import uk.gov.hmcts.reform.fact.data.api.entities.CourtAreasOfLaw;
import uk.gov.hmcts.reform.fact.functional.helpers.TestDataHelper;
import uk.gov.hmcts.reform.fact.functional.http.HttpClient;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.HttpStatus.CREATED;
import static org.springframework.http.HttpStatus.OK;

@Feature("Court Areas Of Law Controller")
@DisplayName("Court Areas Of Law Controller")
public final class CourtAreasOfLawControllerFunctionalTest {

    private static final HttpClient http = new HttpClient();
    private static final ObjectMapper mapper = new ObjectMapper()
        .registerModule(new JavaTimeModule())
        .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

    @Test
    @DisplayName("GET /courts/{courtId}/v1/areas-of-law returns 404 when no areas of law exist")
    void shouldReturn404WhenNoAreasOfLawExist() {
        final UUID courtId = TestDataHelper.createCourt(http, "Test Court No Areas");

        final Response getResponse = http.doGet("/courts/" + courtId + "/v1/areas-of-law");

        assertThat(getResponse.statusCode()).isEqualTo(404);
        assertThat(getResponse.jsonPath().getString("message"))
            .contains("No court areas of law found for court id: " + courtId);
    }

    @Test
    @DisplayName("PUT /courts/{courtId}/v1/areas-of-law sets Adoption to true")
    void shouldSetAdoptionToTrue() throws Exception {
        final UUID courtId = TestDataHelper.createCourt(http, "Test Court Areas Of Law Adoption");

        final Map<String, Boolean> initialAreasMap = initializeCourtAreasOfLaw(courtId);

        final UUID adoptionId = TestDataHelper.extractAreaOfLawTypeIdByName(initialAreasMap, "Adoption");

        final Response updatePutResponse = http.doPut("/courts/" + courtId + "/v1/areas-of-law",
                                                      buildCourtAreasOfLaw(courtId, List.of(adoptionId)));
        assertThat(updatePutResponse.statusCode()).isEqualTo(CREATED.value());

        final Response updatedGetResponse = http.doGet("/courts/" + courtId + "/v1/areas-of-law");
        assertThat(updatedGetResponse.statusCode()).isEqualTo(OK.value());

        final Map<String, Boolean> updatedAreasMap = mapper.readValue(
            updatedGetResponse.asString(),
            new TypeReference<Map<String, Boolean>>() {}
        );

        final boolean adoptionSelected = TestDataHelper.isAreaOfLawSelectedByName(updatedAreasMap, "Adoption");
        assertThat(adoptionSelected)
            .as("Adoption area of law should be selected")
            .isTrue();
    }

    @Test
    @DisplayName("PUT /courts/{courtId}/v1/areas-of-law replaces existing areas")
    void shouldReplaceAreasOfLawOnUpdate() throws Exception {
        final UUID courtId = TestDataHelper.createCourt(http, "Test Court Areas Of Law Replace");

        final Map<String, Boolean> initialAreasMap = initializeCourtAreasOfLaw(courtId);

        final UUID adoptionId = TestDataHelper.extractAreaOfLawTypeIdByName(initialAreasMap, "Adoption");
        final UUID divorceId = TestDataHelper.extractAreaOfLawTypeIdByName(initialAreasMap, "Divorce");
        final UUID immigrationId = TestDataHelper.extractAreaOfLawTypeIdByName(initialAreasMap, "Immigration");

        final Response firstUpdateResponse = http.doPut("/courts/" + courtId + "/v1/areas-of-law",
                                                        buildCourtAreasOfLaw(courtId, List.of(adoptionId, divorceId)));
        assertThat(firstUpdateResponse.statusCode()).isEqualTo(CREATED.value());

        final Response firstGetResponse = http.doGet("/courts/" + courtId + "/v1/areas-of-law");
        assertThat(firstGetResponse.statusCode()).isEqualTo(OK.value());

        final Map<String, Boolean> firstAreasMap = mapper.readValue(
            firstGetResponse.asString(),
            new TypeReference<Map<String, Boolean>>() {}
        );

        assertThat(TestDataHelper.isAreaOfLawSelectedByName(firstAreasMap, "Adoption"))
            .as("Adoption should be selected after first update")
            .isTrue();
        assertThat(TestDataHelper.isAreaOfLawSelectedByName(firstAreasMap, "Divorce"))
            .as("Divorce should be selected after first update")
            .isTrue();
        assertThat(TestDataHelper.isAreaOfLawSelectedByName(firstAreasMap, "Immigration"))
            .as("Immigration should not be selected after first update")
            .isFalse();

        final Response secondUpdateResponse = http.doPut("/courts/" + courtId + "/v1/areas-of-law",
                                                         buildCourtAreasOfLaw(courtId, List.of(immigrationId)));
        assertThat(secondUpdateResponse.statusCode()).isEqualTo(CREATED.value());

        final Response secondGetResponse = http.doGet("/courts/" + courtId + "/v1/areas-of-law");
        assertThat(secondGetResponse.statusCode()).isEqualTo(OK.value());

        final Map<String, Boolean> secondAreasMap = mapper.readValue(
            secondGetResponse.asString(),
            new TypeReference<Map<String, Boolean>>() {}
        );

        assertThat(TestDataHelper.isAreaOfLawSelectedByName(secondAreasMap, "Immigration"))
            .as("Immigration should be selected after second update")
            .isTrue();
        assertThat(TestDataHelper.isAreaOfLawSelectedByName(secondAreasMap, "Adoption"))
            .as("Adoption should be deselected after second update")
            .isFalse();
        assertThat(TestDataHelper.isAreaOfLawSelectedByName(secondAreasMap, "Divorce"))
            .as("Divorce should be deselected after second update")
            .isFalse();
    }

    @Test
    @DisplayName("GET /courts/{courtId}/v1/areas-of-law returns 404 for non-existent court")
    void shouldReturn404ForNonExistentCourtOnGet() {
        final UUID nonExistentCourtId = UUID.randomUUID();

        final Response getResponse = http.doGet("/courts/" + nonExistentCourtId + "/v1/areas-of-law");

        assertThat(getResponse.statusCode()).isEqualTo(404);
        assertThat(getResponse.jsonPath().getString("message"))
            .contains("Court not found, ID: " + nonExistentCourtId);
    }

    @Test
    @DisplayName("PUT /courts/{courtId}/v1/areas-of-law returns 404 for non-existent court")
    void shouldReturn404ForNonExistentCourtOnPut() {
        final UUID nonExistentCourtId = UUID.randomUUID();

        final Response putResponse = http.doPut("/courts/" + nonExistentCourtId + "/v1/areas-of-law",
                                                buildCourtAreasOfLaw(nonExistentCourtId, List.of()));

        assertThat(putResponse.statusCode()).isEqualTo(404);
        assertThat(putResponse.jsonPath().getString("message"))
            .contains("Court not found, ID: " + nonExistentCourtId);
    }

    /**
     * Builds a CourtAreasOfLaw object with the given parameters.
     *
     * @param courtId the court ID
     * @param areasOfLaw the list of area of law IDs
     * @return a CourtAreasOfLaw object
     */
    private static CourtAreasOfLaw buildCourtAreasOfLaw(final UUID courtId, final List<UUID> areasOfLaw) {
        final CourtAreasOfLaw courtAreasOfLaw = new CourtAreasOfLaw();
        courtAreasOfLaw.setCourtId(courtId);
        courtAreasOfLaw.setAreasOfLaw(areasOfLaw);
        return courtAreasOfLaw;
    }

    /**
     * Initializes a court with an empty areas of law list and returns all available areas.
     *
     * @param courtId the court ID to initialize
     * @return map of all areas of law with their availability status
     */
    private static Map<String, Boolean> initializeCourtAreasOfLaw(final UUID courtId) throws Exception {
        final Response initialPutResponse = http.doPut("/courts/" + courtId + "/v1/areas-of-law",
                                                       buildCourtAreasOfLaw(courtId, List.of()));
        assertThat(initialPutResponse.statusCode()).isEqualTo(CREATED.value());

        final Response initialGetResponse = http.doGet("/courts/" + courtId + "/v1/areas-of-law");
        assertThat(initialGetResponse.statusCode()).isEqualTo(OK.value());

        return mapper.readValue(
            initialGetResponse.asString(),
            new TypeReference<Map<String, Boolean>>() {}
        );
    }

    @AfterAll
    static void cleanUpTestData() {
        http.doDelete("/testing-support/courts/name-prefix/Test Court");
    }
}
