package uk.gov.hmcts.reform.fact.functional.helpers;

import io.restassured.response.Response;
import uk.gov.hmcts.reform.fact.data.api.entities.Court;
import uk.gov.hmcts.reform.fact.data.api.entities.CourtFacilities;
import uk.gov.hmcts.reform.fact.functional.http.HttpClient;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.HttpStatus.CREATED;

/**
 * Helper methods for fetching test data via API endpoints.
 */
public final class TestDataHelper {

    private TestDataHelper() {
        // Utility class
    }

    /**
     * Fetches the first region ID from the regions' endpoint.
     *
     * @param http the HTTP client
     * @return the region ID as a string
     */
    public static String getRegionId(final HttpClient http) {
        final Response response = http.doGet("/types/v1/regions");
        return response.jsonPath().getString("[0].id");
    }

    /**
     * Creates a test court with the given name.
     *
     * @param http the HTTP client
     * @param courtName the name for the test court
     * @return the created court's UUID
     */
    public static UUID createCourt(final HttpClient http, final String courtName) {
        final Court court = new Court();
        court.setName(courtName);
        court.setRegionId(UUID.fromString(getRegionId(http)));
        court.setIsServiceCentre(true);

        final Response createResponse = http.doPost("/courts/v1", court);
        assertThat(createResponse.statusCode()).isEqualTo(CREATED.value());

        return UUID.fromString(createResponse.jsonPath().getString("id"));
    }

    /**
     * Builds a CourtFacilities object with all fields set to true by default.
     *
     * @param courtId the court ID
     * @return a CourtFacilities object with all facility fields set to true
     */
    public static CourtFacilities buildFacilities(final UUID courtId) {
        final CourtFacilities facilities = new CourtFacilities();
        facilities.setCourtId(courtId);
        facilities.setParking(true);
        facilities.setFreeWaterDispensers(true);
        facilities.setSnackVendingMachines(true);
        facilities.setDrinkVendingMachines(true);
        facilities.setCafeteria(true);
        facilities.setWaitingArea(true);
        facilities.setWaitingAreaChildren(true);
        facilities.setQuietRoom(true);
        facilities.setBabyChanging(true);
        facilities.setWifi(true);
        return facilities;
    }
}
