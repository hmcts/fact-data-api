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
import uk.gov.hmcts.reform.fact.functional.helpers.AssertionHelper;
import uk.gov.hmcts.reform.fact.functional.helpers.TestDataHelper;
import uk.gov.hmcts.reform.fact.functional.http.HttpClient;

import java.time.ZonedDateTime;
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

        final ZonedDateTime timestampBeforeUpdate = AssertionHelper.getCourtLastUpdatedAt(http, courtId);

        final UUID adoptionId = TestDataHelper.getAreaOfLawIdByName(http, "Adoption");

        final Response updatePutResponse = http.doPut("/courts/" + courtId + "/v1/areas-of-law",
                                              TestDataHelper.buildCourtAreasOfLaw(courtId, List.of(adoptionId)));
        assertThat(updatePutResponse.statusCode()).isEqualTo(CREATED.value());

        final Response updatedGetResponse = http.doGet("/courts/" + courtId + "/v1/areas-of-law");
        assertThat(updatedGetResponse.statusCode()).isEqualTo(OK.value());

        final Map<String, Boolean> updatedAreasMap = mapper.readValue(
            updatedGetResponse.asString(),
            new TypeReference<Map<String, Boolean>>() {}
        );

        final boolean adoptionSelected = isAreaOfLawSelectedByName(updatedAreasMap, "Adoption");
        assertThat(adoptionSelected)
            .as("Adoption area of law should be selected")
            .isTrue();

        final ZonedDateTime timestampAfterUpdate = AssertionHelper.getCourtLastUpdatedAt(http, courtId);
        assertThat(timestampAfterUpdate)
            .as("Court lastUpdatedAt should move forward after areas of law update for court %s", courtId)
            .isAfter(timestampBeforeUpdate);
    }

    @Test
    @DisplayName("PUT /courts/{courtId}/v1/areas-of-law replaces existing areas")
    void shouldReplaceAreasOfLawOnUpdate() throws Exception {
        final UUID courtId = TestDataHelper.createCourt(http, "Test Court Areas Of Law Replace");

        final UUID adoptionId = TestDataHelper.getAreaOfLawIdByName(http, "Adoption");
        final UUID divorceId = TestDataHelper.getAreaOfLawIdByName(http, "Divorce");
        final UUID immigrationId = TestDataHelper.getAreaOfLawIdByName(http, "Immigration");

        final Response firstUpdateResponse = http.doPut("/courts/" + courtId + "/v1/areas-of-law",
                                    TestDataHelper.buildCourtAreasOfLaw(courtId, List.of(adoptionId, divorceId)));
        assertThat(firstUpdateResponse.statusCode()).isEqualTo(CREATED.value());

        final Response firstGetResponse = http.doGet("/courts/" + courtId + "/v1/areas-of-law");
        assertThat(firstGetResponse.statusCode()).isEqualTo(OK.value());

        final Map<String, Boolean> firstAreasMap = mapper.readValue(
            firstGetResponse.asString(),
            new TypeReference<Map<String, Boolean>>() {}
        );

        assertThat(isAreaOfLawSelectedByName(firstAreasMap, "Adoption"))
            .as("Adoption should be selected after first update")
            .isTrue();
        assertThat(isAreaOfLawSelectedByName(firstAreasMap, "Divorce"))
            .as("Divorce should be selected after first update")
            .isTrue();
        assertThat(isAreaOfLawSelectedByName(firstAreasMap, "Immigration"))
            .as("Immigration should not be selected after first update")
            .isFalse();

        final Response secondUpdateResponse = http.doPut("/courts/" + courtId + "/v1/areas-of-law",
                                                 TestDataHelper.buildCourtAreasOfLaw(courtId, List.of(immigrationId)));
        assertThat(secondUpdateResponse.statusCode()).isEqualTo(CREATED.value());

        final Response secondGetResponse = http.doGet("/courts/" + courtId + "/v1/areas-of-law");
        assertThat(secondGetResponse.statusCode()).isEqualTo(OK.value());

        final Map<String, Boolean> secondAreasMap = mapper.readValue(
            secondGetResponse.asString(),
            new TypeReference<Map<String, Boolean>>() {}
        );

        assertThat(isAreaOfLawSelectedByName(secondAreasMap, "Immigration"))
            .as("Immigration should be selected after second update")
            .isTrue();
        assertThat(isAreaOfLawSelectedByName(secondAreasMap, "Adoption"))
            .as("Adoption should be deselected after second update")
            .isFalse();
        assertThat(isAreaOfLawSelectedByName(secondAreasMap, "Divorce"))
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
                                                TestDataHelper.buildCourtAreasOfLaw(nonExistentCourtId, List.of()));

        assertThat(putResponse.statusCode()).isEqualTo(404);
        assertThat(putResponse.jsonPath().getString("message"))
            .contains("Court not found, ID: " + nonExistentCourtId);
    }

    /**
     * Finds the selected status for an area of law by name.
     *
     * @param areasOfLawMap the map returned by GET /courts/{courtId}/v1/areas-of-law
     * @param name the exact area of law name to match
     * @return true if selected, false if not selected
     */
    private static boolean isAreaOfLawSelectedByName(final Map<String, Boolean> areasOfLawMap, final String name) {
        for (String key : areasOfLawMap.keySet()) {
            if (matchesAreaOfLawName(key, name)) {
                return areasOfLawMap.get(key);
            }
        }

        throw new IllegalStateException(
            String.format("Area of law name not found in response: %s. Available keys: %s",
                          name, areasOfLawMap.keySet())
        );
    }

    /**
     * Checks if a map key matches the specified area of law name.
     *
     * @param key the map key to check
     * @param name the area of law name to match
     * @return true if the key contains the specified name
     */
    private static boolean matchesAreaOfLawName(final String key, final String name) {
        return key.contains("name=" + name);
    }

    @AfterAll
    static void cleanUpTestData() {
        http.doDelete("/testing-support/courts/name-prefix/Test Court");
    }
}
