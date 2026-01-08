package uk.gov.hmcts.reform.fact.functional.helpers;

import io.restassured.response.Response;
import uk.gov.hmcts.reform.fact.data.api.entities.Court;
import uk.gov.hmcts.reform.fact.data.api.entities.CourtFacilities;
import uk.gov.hmcts.reform.fact.functional.http.HttpClient;

import java.util.Map;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.HttpStatus.CREATED;

/**
 * Helper methods for fetching test data via API endpoints.
 */
public final class TestDataHelper {

    private static final Pattern AREA_OF_LAW_ID_PATTERN =
        Pattern.compile("id=([0-9a-fA-F-]{36})");

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
     * Fetches a specific opening hour type ID by index from the opening hour types endpoint.
     * Valid indices are 0-8 for the 9 available opening hour types.
     *
     * @param http the HTTP client
     * @param index the index of the opening hour type (0-8)
     * @return the opening hour type ID at the specified index as a UUID
     */
    public static UUID getOpeningHourTypeId(final HttpClient http, final int index) {
        final Response response = http.doGet("/types/v1/opening-hours-types");
        return UUID.fromString(response.jsonPath().getString("[" + index + "].id"));
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

    /**
     * Extracts a single area of law type ID by name from the response map keys.
     *
     * @param areasOfLawMap the map returned by GET /courts/{courtId}/v1/areas-of-law
     * @param name the exact area of law name to match
     * @return the UUID for the requested area of law name
     */
    public static UUID extractAreaOfLawTypeIdByName(final Map<String, Boolean> areasOfLawMap, final String name) {
        for (String key : areasOfLawMap.keySet()) {
            if (matchesAreaOfLawName(key, name)) {
                final Matcher matcher = AREA_OF_LAW_ID_PATTERN.matcher(key);
                if (matcher.find()) {
                    return UUID.fromString(matcher.group(1));
                }
            }
        }

        throw new IllegalStateException(
            String.format("Area of law name not found in response: %s. Available keys: %s",
                          name, areasOfLawMap.keySet())
        );
    }

    /**
     * Finds the selected status for an area of law by name.
     *
     * @param areasOfLawMap the map returned by GET /courts/{courtId}/v1/areas-of-law
     * @param name the exact area of law name to match
     * @return true if selected, false if not selected
     */
    public static boolean isAreaOfLawSelectedByName(final Map<String, Boolean> areasOfLawMap, final String name) {
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
}
