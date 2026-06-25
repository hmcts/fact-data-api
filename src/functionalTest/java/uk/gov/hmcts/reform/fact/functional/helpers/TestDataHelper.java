package uk.gov.hmcts.reform.fact.functional.helpers;

import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.cfg.DateTimeFeature;
import tools.jackson.databind.json.JsonMapper;
import io.restassured.response.Response;
import uk.gov.hmcts.reform.fact.data.api.entities.Court;
import uk.gov.hmcts.reform.fact.data.api.entities.CourtFacilities;
import uk.gov.hmcts.reform.fact.data.api.entities.CourtLock;
import uk.gov.hmcts.reform.fact.data.api.entities.ServiceCentre;
import uk.gov.hmcts.reform.fact.data.api.entities.User;
import uk.gov.hmcts.reform.fact.data.api.entities.types.CatchmentType;
import uk.gov.hmcts.reform.fact.data.api.entities.types.Page;
import uk.gov.hmcts.reform.fact.data.api.entities.types.UserRole;
import uk.gov.hmcts.reform.fact.data.api.os.OsData;
import uk.gov.hmcts.reform.fact.data.api.os.OsDpa;
import uk.gov.hmcts.reform.fact.functional.http.HttpClient;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.security.SecureRandom;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.HttpStatus.CREATED;
import static org.springframework.http.HttpStatus.OK;

/**
 * Helper methods for fetching test data via API endpoints.
 */
public final class TestDataHelper {

    private static final ObjectMapper mapper = JsonMapper.builder()
        .disable(DateTimeFeature.WRITE_DATES_AS_TIMESTAMPS)
        .build();
    private static final SecureRandom random = new SecureRandom();

    private TestDataHelper() {
        // Utility class
    }

    /**
     * Fetches the first region ID from the regions' endpoint.
     *
     * @param http the HTTP client
     * @return the region ID as a string
     */
    public static String fetchFirstRegionId(final HttpClient http) {
        final Response response = http.doGet("/types/v1/regions");
        return response.jsonPath().getString("[0].id");
    }

    /**
     * Fetches a specific opening hour type ID by index from the opening hour types endpoint.
     * Valid indices are 0-9 for the 10 available opening hour types.
     *
     * @param http the HTTP client
     * @param index the index of the opening hour type (0-9)
     * @return the opening hour type ID at the specified index as a UUID
     */
    public static UUID getOpeningHourTypeId(final HttpClient http, final int index) {
        final Response response = http.doGet("/types/v1/opening-hours-types");
        return UUID.fromString(response.jsonPath().getString("[" + index + "].id"));
    }

    /**
     * Creates a test user with the given email prefix.
     *
     * @param http the HTTP client
     * @param emailPrefix the email prefix for the test user
     * @return the created user's UUID
     */
    public static UUID createUser(final HttpClient http, final String emailPrefix) {
        final User user = User.builder()
            .email(emailPrefix + "." + System.currentTimeMillis() + "@justice.gov.uk")
            .ssoId(UUID.randomUUID())
            .role(UserRole.ADMIN)
            .build();

        final Response createResponse = http.doPost("/user/v1", user);

        assertThat(createResponse.statusCode())
            .as("Expected 201 CREATED when creating user")
            .isEqualTo(CREATED.value());

        return UUID.fromString(createResponse.jsonPath().getString("id"));
    }

    /**
     * Appends a random 6-character lowercase suffix to the provided court name.
     *
     * @param courtName the base court name
     * @return the court name with random suffix appended
     */
    public static String appendRandomSuffixToCourtName(final String courtName) {
        return courtName + " " + generateRandomLowercaseString(6);
    }

    /**
     * Generates a random lowercase alphabetic string (a-z) with the requested length.
     *
     * @param length the length of random string
     * @return random lowercase string
     */
    public static String generateRandomLowercaseString(final int length) {
        final StringBuilder randomString = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            randomString.append((char) ('a' + random.nextInt(26)));
        }
        return randomString.toString();
    }

    /**
     * Creates a test court with the given name.
     *
     * @param http the HTTP client
     * @param courtName the name for the test court
     * @return the created court's UUID
     */
    public static UUID createCourt(final HttpClient http, final String courtName) {
        return createCourt(http, courtName, true);
    }

    /**
     * Creates a test court with the given name.
     *
     * @param http the HTTP client
     * @param courtName the name for the test court
     * @param isOpen whether the court is open
     * @return the created court's UUID
     */
    public static UUID createCourt(final HttpClient http, final String courtName, boolean isOpen) {
        final Court court = new Court();
        court.setName(appendRandomSuffixToCourtName(courtName));
        court.setRegionId(UUID.fromString(fetchFirstRegionId(http)));
        court.setOpen(isOpen);

        final Response createResponse = http.doPost("/courts/v1", court);
        assertThat(createResponse.statusCode()).isEqualTo(CREATED.value());

        return UUID.fromString(createResponse.jsonPath().getString("id"));
    }

    /**
     * Creates a test court with the given name.
     *
     * @param http the HTTP client
     * @param courtName the name for the test court
     * @param isOpen whether the court is open
     * @param mrdId the MRD ID
     * @param openOnCath whether the court is open on CaTH
     * @return the created court's UUID
     */
    public static UUID createCourt(
        final HttpClient http, final String courtName, boolean isOpen, final String mrdId, boolean openOnCath) {

        final Court court = new Court();
        court.setName(appendRandomSuffixToCourtName(courtName));
        court.setRegionId(UUID.fromString(fetchFirstRegionId(http)));
        court.setOpen(isOpen);
        court.setMrdId(mrdId);
        court.setOpenOnCath(openOnCath);

        final Response createResponse = http.doPost("/courts/v1", court);
        assertThat(createResponse.statusCode()).isEqualTo(CREATED.value());

        return UUID.fromString(createResponse.jsonPath().getString("id"));
    }

    /**
     * Builds a test service centre with the given name.
     *
     * @param http the HTTP client
     * @param serviceCentreName the name for the test service centre
     * @return the service centre body
     */
    public static ServiceCentre buildServiceCentre(final HttpClient http, final String serviceCentreName) {
        return buildServiceCentre(http, serviceCentreName, HttpClient.getAdminBearerToken());
    }

    /**
     * Builds a test service centre with the given name.
     *
     * @param http the HTTP client
     * @param serviceCentreName the name for the test service centre
     * @param bearerToken the bearer token to use when fetching reference data
     * @return the service centre body
     */
    public static ServiceCentre buildServiceCentre(final HttpClient http,
                                                  final String serviceCentreName,
                                                  final String bearerToken) {
        final ServiceCentre serviceCentre = new ServiceCentre();
        serviceCentre.setName(appendRandomSuffixToCourtName(serviceCentreName));
        serviceCentre.setOpen(false);
        serviceCentre.setServiceAreaIds(List.of(UUID.fromString(
            http.doGet("/types/v1/service-areas", bearerToken).jsonPath().getString("[0].id")
        )));
        serviceCentre.setCatchmentType(CatchmentType.NATIONAL);
        return serviceCentre;
    }

    /**
     * Creates a test service centre with the given name.
     *
     * @param http the HTTP client
     * @param serviceCentreName the name for the test service centre
     * @return the created service centre UUID
     */
    public static UUID createServiceCentre(final HttpClient http, final String serviceCentreName) {
        return createServiceCentre(http, serviceCentreName, HttpClient.getAdminBearerToken());
    }

    /**
     * Creates a test service centre with the given name.
     *
     * @param http the HTTP client
     * @param serviceCentreName the name for the test service centre
     * @param bearerToken the bearer token to use when creating the service centre
     * @return the created service centre UUID
     */
    public static UUID createServiceCentre(final HttpClient http,
                                           final String serviceCentreName,
                                           final String bearerToken) {
        final ServiceCentre serviceCentre = buildServiceCentre(http, serviceCentreName, bearerToken);

        final Response createResponse = http.doPost("/service-centres/v1", serviceCentre, bearerToken);
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
     * Fetches an area of law ID by name from the areas of law types endpoint.
     *
     * @param http the HTTP client
     * @param name the exact area of law name to match
     * @return the UUID for the requested area of law
     */
    public static UUID getAreaOfLawIdByName(final HttpClient http, final String name) {
        final Response response = http.doGet("/types/v1/areas-of-law");
        final List<Map<String, Object>> areas = response.jsonPath().getList("");

        for (Map<String, Object> area : areas) {
            if (name.equals(area.get("name"))) {
                return UUID.fromString((String) area.get("id"));
            }
        }

        throw new IllegalStateException(
            String.format("Area of law name not found: %s", name)
        );
    }

    /**
     * Builds a CourtAreasOfLaw object with the given parameters.
     *
     * @param courtId the court ID
     * @param areasOfLaw the list of area of law IDs
     * @return a CourtAreasOfLaw object
     */
    public static uk.gov.hmcts.reform.fact.data.api.entities.CourtAreasOfLaw buildCourtAreasOfLaw(
        final UUID courtId, final java.util.List<UUID> areasOfLaw) {
        final uk.gov.hmcts.reform.fact.data.api.entities.CourtAreasOfLaw courtAreasOfLaw =
            new uk.gov.hmcts.reform.fact.data.api.entities.CourtAreasOfLaw();
        courtAreasOfLaw.setCourtId(courtId);
        courtAreasOfLaw.setAreasOfLaw(areasOfLaw);
        return courtAreasOfLaw;
    }

    /**
     * Creates a court lock for a specific page.
     *
     * @param http the HTTP client
     * @param courtId the court ID
     * @param page the page to lock
     * @param userId the user ID
     * @return the created court lock
     */
    public static CourtLock createCourtLock(final HttpClient http, final UUID courtId,
                                            final Page page, final UUID userId) throws Exception {
        final Response response = http.doPost(
            "/courts/" + courtId + "/v1/locks/" + page,
            mapper.writeValueAsString(userId)
        );

        assertThat(response.statusCode())
            .as("Expected 201 CREATED when creating lock for court %s page %s", courtId, page)
            .isEqualTo(CREATED.value());

        return mapper.readValue(response.getBody().asString(), CourtLock.class);
    }

    /**
     * Fetches OS address data for a given postcode.
     *
     * @param http the HTTP client
     * @param postcode the postcode to search for
     * @return the OS data containing address results
     * @throws Exception if the API call or JSON parsing fails
     */
    public static OsData fetchOsDataForPostcode(final HttpClient http, final String postcode) throws Exception {
        final Response response = http.doGet("/search/address/v1/postcode/" + postcode);

        assertThat(response.statusCode())
            .as("Expected 200 OK when fetching OS data for postcode %s", postcode)
            .isEqualTo(OK.value());

        return mapper.readValue(response.getBody().asString(), OsData.class);
    }

    /**
     * Fetches the first Delivery Point Address (DPA) for a given postcode from the OS API.
     *
     * @param http the HTTP client
     * @param postcode the postcode to search for
     * @return the first DPA result for the postcode
     * @throws Exception if the API call or JSON parsing fails
     */
    public static OsDpa fetchFirstDpaForPostcode(final HttpClient http, final String postcode) throws Exception {
        return fetchOsDataForPostcode(http, postcode).getResults().getFirst().getDpa();
    }
}
