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
import uk.gov.hmcts.reform.fact.data.api.dto.CourtProfessionalInformationDetailsDto;
import uk.gov.hmcts.reform.fact.data.api.dto.ProfessionalInformationDto;
import uk.gov.hmcts.reform.fact.data.api.entities.Court;
import uk.gov.hmcts.reform.fact.data.api.entities.CourtAccessibilityOptions;
import uk.gov.hmcts.reform.fact.data.api.entities.CourtAddress;
import uk.gov.hmcts.reform.fact.data.api.entities.CourtContactDetails;
import uk.gov.hmcts.reform.fact.data.api.entities.CourtCounterServiceOpeningHours;
import uk.gov.hmcts.reform.fact.data.api.entities.CourtFacilities;
import uk.gov.hmcts.reform.fact.data.api.entities.CourtOpeningHours;
import uk.gov.hmcts.reform.fact.data.api.entities.CourtTranslation;
import uk.gov.hmcts.reform.fact.data.api.entities.User;
import uk.gov.hmcts.reform.fact.data.api.entities.types.AddressType;
import uk.gov.hmcts.reform.fact.data.api.entities.types.DayOfTheWeek;
import uk.gov.hmcts.reform.fact.data.api.entities.types.HearingEnhancementEquipment;
import uk.gov.hmcts.reform.fact.data.api.entities.types.Page;
import uk.gov.hmcts.reform.fact.data.api.models.AreaOfLawSelectionDto;
import uk.gov.hmcts.reform.fact.data.api.models.CourtLocalAuthorityDto;
import uk.gov.hmcts.reform.fact.functional.helpers.TestDataHelper;
import uk.gov.hmcts.reform.fact.functional.http.HttpClient;

import java.io.File;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.HttpStatus.FORBIDDEN;
import static org.springframework.http.HttpStatus.OK;
import static org.springframework.http.HttpStatus.UNAUTHORIZED;

@Feature("Auth")
@DisplayName("Auth Functional Tests")
public class AuthFunctionalTest {

    private static final HttpClient http = new HttpClient();
    private static final ObjectMapper mapper = new ObjectMapper()
        .registerModule(new JavaTimeModule())
        .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

    private static final String adminToken = HttpClient.getAdminBearerToken();
    private static final String viewerToken = HttpClient.getViewerBearerToken();
    private static final String invalidToken = "invalid-token";

    private static String getRegionId() {
        return TestDataHelper.fetchFirstRegionId(http);
    }

    private static UUID createUserAsAdmin(String prefix) {
        User user = User.builder()
            .email(prefix + "." + System.currentTimeMillis() + "@justice.gov.uk")
            .ssoId(UUID.randomUUID())
            .build();
        Response response = http.doPost("/user/v1", user, adminToken);
        assertThat(response.statusCode()).isEqualTo(201);
        return UUID.fromString(response.jsonPath().getString("id"));
    }

    private static UUID createCourtAsAdmin(String name) {
        Court court = new Court();
        court.setName(name + " " + buildAlphabeticSuffix());
        court.setRegionId(UUID.fromString(getRegionId()));
        court.setIsServiceCentre(true);
        Response createResponse = http.doPost("/courts/v1", court, adminToken);
        assertThat(createResponse.statusCode()).isEqualTo(201);
        return UUID.fromString(createResponse.jsonPath().getString("id"));
    }

    private static String buildAlphabeticSuffix() {
        final String lettersOnly = UUID.randomUUID().toString().replaceAll("[^A-Za-z]", "a");
        return lettersOnly.substring(0, 8);
    }

    private static void assertViewerAllowed(Response adminResponse, Response viewerResponse, String endpoint) {
        assertThat(adminResponse.statusCode())
            .as("Admin status should not be 401 for %s", endpoint)
            .isNotEqualTo(UNAUTHORIZED.value());
        assertThat(viewerResponse.statusCode())
            .as("Viewer should match admin status for %s", endpoint)
            .isEqualTo(adminResponse.statusCode());
    }

    private static void assertViewerForbidden(Response viewerResponse, String endpoint) {
        assertThat(viewerResponse.statusCode())
            .as("Viewer should be forbidden for %s", endpoint)
            .isEqualTo(FORBIDDEN.value());
    }

    private static void assertUnauthenticated(Response noTokenResponse, String endpoint) {
        assertThat(noTokenResponse.statusCode())
            .as("No token should return 401 for %s", endpoint)
            .isEqualTo(UNAUTHORIZED.value());
    }

    private static void assertInvalidToken(Response invalidTokenResponse, String endpoint) {
        assertThat(invalidTokenResponse.statusCode())
            .as("Invalid token should return 401 for %s", endpoint)
            .isEqualTo(UNAUTHORIZED.value());
    }

    @Test
    @DisplayName("Public endpoints remain accessible without auth")
    void publicEndpointsShouldRemainOpen() {
        Response root = http.doGet("/", "");
        Response health = http.doGet("/health", "");
        Response docs = http.doGet("/v3/api-docs", "");

        assertThat(root.statusCode()).isEqualTo(OK.value());
        assertThat(health.statusCode()).isEqualTo(OK.value());
        assertThat(docs.statusCode()).isEqualTo(OK.value());
    }

    @Test
    @DisplayName("Types endpoints allow admin and viewer, reject unauthenticated")
    void typesEndpointsAuth() {
        String[] endpoints = new String[]{
            "/types/v1/areas-of-law",
            "/types/v1/court-types",
            "/types/v1/opening-hours-types",
            "/types/v1/contact-description-types",
            "/types/v1/regions",
            "/types/v1/service-areas"
        };

        for (String endpoint : endpoints) {
            Response admin = http.doGet(endpoint, adminToken);
            Response viewer = http.doGet(endpoint, viewerToken);
            Response noToken = http.doGet(endpoint, "");
            Response badToken = http.doGet(endpoint, invalidToken);
            assertViewerAllowed(admin, viewer, endpoint);
            assertUnauthenticated(noToken, endpoint);
            assertInvalidToken(badToken, endpoint);
        }
    }

    @Test
    @DisplayName("Court controller endpoints enforce admin-only writes")
    void courtControllerAuth() {
        UUID courtId = createCourtAsAdmin("Test Court Auth Core");
        Court updateCourt = new Court();
        updateCourt.setName("Test Court Auth Updated");
        updateCourt.setRegionId(UUID.fromString(getRegionId()));
        updateCourt.setIsServiceCentre(true);

        String[] readEndpoints = new String[]{
            "/courts/" + courtId + "/v1",
            "/courts/" + courtId + ".json",
            "/courts/slug/" + "test-court-auth-updated" + "/v1",
            "/courts/all/v1",
            "/courts/all.json",
            "/courts/v1?pageNumber=0&pageSize=10&includeClosed=true"
        };

        for (String endpoint : readEndpoints) {
            Response admin = http.doGet(endpoint, adminToken);
            Response viewer = http.doGet(endpoint, viewerToken);
            assertViewerAllowed(admin, viewer, endpoint);
        }

        Response createAdmin = http.doPost("/courts/v1", updateCourt, adminToken);
        Response createViewer = http.doPost("/courts/v1", updateCourt, viewerToken);
        assertThat(createAdmin.statusCode()).isEqualTo(201);
        assertViewerForbidden(createViewer, "/courts/v1 [POST]");

        Response updateAdmin = http.doPut("/courts/" + courtId + "/v1", updateCourt, adminToken);
        Response updateViewer = http.doPut("/courts/" + courtId + "/v1", updateCourt, viewerToken);
        assertThat(updateAdmin.statusCode()).isEqualTo(200);
        assertViewerForbidden(updateViewer, "/courts/{courtId}/v1 [PUT]");

        assertUnauthenticated(http.doGet("/courts/v1", ""), "/courts/v1 [GET]");
        assertUnauthenticated(http.doPost("/courts/v1", updateCourt, ""), "/courts/v1 [POST]");
    }

    @Test
    @DisplayName("Court address endpoints enforce admin-only writes")
    void courtAddressAuth() {
        UUID courtId = createCourtAsAdmin("Test Court Auth Address");
        CourtAddress address = CourtAddress.builder()
            .courtId(courtId)
            .addressLine1("1 Auth Street")
            .townCity("London")
            .postcode("NW10 4DX")
            .addressType(AddressType.VISIT_US)
            .build();

        Response createAdmin = http.doPost("/courts/" + courtId + "/v1/address", address, adminToken);
        assertThat(createAdmin.statusCode())
            .as("Admin POST address failed. Body: %s", createAdmin.asString())
            .isEqualTo(201);
        UUID addressId = UUID.fromString(createAdmin.jsonPath().getString("id"));

        Response listAdmin = http.doGet("/courts/" + courtId + "/v1/address", adminToken);
        Response listViewer = http.doGet("/courts/" + courtId + "/v1/address", viewerToken);
        assertViewerAllowed(listAdmin, listViewer, "/courts/{courtId}/v1/address [GET]");

        Response byIdAdmin = http.doGet("/courts/" + courtId + "/v1/address/" + addressId, adminToken);
        Response byIdViewer = http.doGet("/courts/" + courtId + "/v1/address/" + addressId, viewerToken);
        assertViewerAllowed(byIdAdmin, byIdViewer, "/courts/{courtId}/v1/address/{addressId} [GET]");

        Response createViewer = http.doPost("/courts/" + courtId + "/v1/address", address, viewerToken);
        assertViewerForbidden(createViewer, "/courts/{courtId}/v1/address [POST]");

        Response updateAdmin = http.doPut("/courts/" + courtId + "/v1/address/" + addressId, address, adminToken);
        Response updateViewer = http.doPut("/courts/" + courtId + "/v1/address/" + addressId, address, viewerToken);
        assertThat(updateAdmin.statusCode())
            .as("Admin PUT address failed. Body: %s", updateAdmin.asString())
            .isEqualTo(200);
        assertViewerForbidden(updateViewer, "/courts/{courtId}/v1/address/{addressId} [PUT]");

        Response deleteAdmin = http.doDelete("/courts/" + courtId + "/v1/address/" + addressId, adminToken);
        Response deleteViewer = http.doDelete("/courts/" + courtId + "/v1/address/" + addressId, viewerToken);
        assertThat(deleteAdmin.statusCode()).isIn(200, 204);
        assertViewerForbidden(deleteViewer, "/courts/{courtId}/v1/address/{addressId} [DELETE]");
    }

    @Test
    @DisplayName("Court contact details endpoints enforce admin-only writes")
    void courtContactDetailsAuth() {
        UUID courtId = createCourtAsAdmin("Test Court Auth Contact");
        UUID contactTypeId = UUID.fromString(http.doGet("/types/v1/contact-description-types", adminToken)
                                                 .jsonPath().getString("[0].id"));
        CourtContactDetails details = new CourtContactDetails();
        details.setCourtId(courtId);
        details.setCourtContactDescriptionId(contactTypeId);
        details.setExplanation("Auth test");
        details.setEmail("auth@test.gov.uk");
        details.setPhoneNumber("01234567890");

        Response createAdmin = http.doPost("/courts/" + courtId + "/v1/contact-details", details, adminToken);
        assertThat(createAdmin.statusCode()).isEqualTo(201);
        UUID contactId = UUID.fromString(createAdmin.jsonPath().getString("id"));

        assertViewerAllowed(
            http.doGet("/courts/" + courtId + "/v1/contact-details", adminToken),
            http.doGet("/courts/" + courtId + "/v1/contact-details", viewerToken),
            "/courts/{courtId}/v1/contact-details [GET]"
        );
        assertViewerAllowed(
            http.doGet("/courts/" + courtId + "/v1/contact-details/" + contactId, adminToken),
            http.doGet("/courts/" + courtId + "/v1/contact-details/" + contactId, viewerToken),
            "/courts/{courtId}/v1/contact-details/{contactId} [GET]"
        );
        assertViewerForbidden(
            http.doPost("/courts/" + courtId + "/v1/contact-details", details, viewerToken),
            "/courts/{courtId}/v1/contact-details [POST]"
        );
        assertViewerForbidden(
            http.doPut("/courts/" + courtId + "/v1/contact-details/" + contactId, details, viewerToken),
            "/courts/{courtId}/v1/contact-details/{contactId} [PUT]"
        );
        assertViewerForbidden(
            http.doDelete("/courts/" + courtId + "/v1/contact-details/" + contactId, viewerToken),
            "/courts/{courtId}/v1/contact-details/{contactId} [DELETE]"
        );
    }

    @Test
    @DisplayName("Court accessibility options endpoints enforce admin-only writes")
    void courtAccessibilityOptionsAuth() {
        UUID courtId = createCourtAsAdmin("Test Court Auth Access");
        CourtAccessibilityOptions payload = CourtAccessibilityOptions.builder()
            .courtId(courtId)
            .accessibleParking(true)
            .accessibleParkingPhoneNumber("+44 20 7946 0958")
            .accessibleEntrance(true)
            .accessibleEntrancePhoneNumber("020 7946 0959")
            .hearingEnhancementEquipment(HearingEnhancementEquipment.HEARING_LOOP_SYSTEMS)
            .lift(true)
            .liftDoorWidth(90)
            .liftDoorLimit(500)
            .quietRoom(true)
            .build();

        Response createAdmin = http.doPost("/courts/" + courtId + "/v1/accessibility-options", payload, adminToken);
        assertThat(createAdmin.statusCode())
            .as("Admin POST accessibility-options failed. Body: %s", createAdmin.asString())
            .isEqualTo(201);

        assertViewerAllowed(
            http.doGet("/courts/" + courtId + "/v1/accessibility-options", adminToken),
            http.doGet("/courts/" + courtId + "/v1/accessibility-options", viewerToken),
            "/courts/{courtId}/v1/accessibility-options [GET]"
        );
        assertViewerForbidden(
            http.doPost("/courts/" + courtId + "/v1/accessibility-options", payload, viewerToken),
            "/courts/{courtId}/v1/accessibility-options [POST]"
        );
    }

    @Test
    @DisplayName("Court areas-of-law endpoints enforce admin-only writes")
    void courtAreasOfLawAuth() {
        UUID courtId = createCourtAsAdmin("Test Court Auth AOL");
        UUID areaId = TestDataHelper.getAreaOfLawIdByName(http, "Adoption");
        Object payload = TestDataHelper.buildCourtAreasOfLaw(courtId, List.of(areaId));

        Response putAdmin = http.doPut("/courts/" + courtId + "/v1/areas-of-law", payload, adminToken);
        assertThat(putAdmin.statusCode()).isEqualTo(201);

        assertViewerAllowed(
            http.doGet("/courts/" + courtId + "/v1/areas-of-law", adminToken),
            http.doGet("/courts/" + courtId + "/v1/areas-of-law", viewerToken),
            "/courts/{courtId}/v1/areas-of-law [GET]"
        );
        assertViewerForbidden(
            http.doPut("/courts/" + courtId + "/v1/areas-of-law", payload, viewerToken),
            "/courts/{courtId}/v1/areas-of-law [PUT]"
        );
    }

    @Test
    @DisplayName("Court facilities endpoints enforce admin-only writes")
    void courtFacilitiesAuth() {
        UUID courtId = createCourtAsAdmin("Test Court Auth Facilities");
        CourtFacilities payload = TestDataHelper.buildFacilities(courtId);

        Response postAdmin = http.doPost("/courts/" + courtId + "/v1/building-facilities", payload, adminToken);
        assertThat(postAdmin.statusCode()).isEqualTo(201);

        assertViewerAllowed(
            http.doGet("/courts/" + courtId + "/v1/building-facilities", adminToken),
            http.doGet("/courts/" + courtId + "/v1/building-facilities", viewerToken),
            "/courts/{courtId}/v1/building-facilities [GET]"
        );
        assertViewerForbidden(
            http.doPost("/courts/" + courtId + "/v1/building-facilities", payload, viewerToken),
            "/courts/{courtId}/v1/building-facilities [POST]"
        );
    }

    @Test
    @DisplayName("Court local authorities endpoints enforce admin-only writes")
    void courtLocalAuthoritiesAuth() throws Exception {
        final UUID courtId = createCourtAsAdmin("Test Court Auth Local");
        final UUID adoptionId = TestDataHelper.getAreaOfLawIdByName(http, "Adoption");
        final UUID childrenId = TestDataHelper.getAreaOfLawIdByName(http, "Children");
        final UUID cpId = TestDataHelper.getAreaOfLawIdByName(http, "Civil partnership");
        final UUID divorceId = TestDataHelper.getAreaOfLawIdByName(http, "Divorce");
        final Object aolPayload = TestDataHelper.buildCourtAreasOfLaw(
            courtId,
            List.of(adoptionId, childrenId, cpId, divorceId)
        );
        final Response enableAol = http.doPut("/courts/" + courtId + "/v1/areas-of-law", aolPayload, adminToken);
        assertThat(enableAol.statusCode()).isEqualTo(201);

        Response getAdmin = http.doGet("/courts/" + courtId + "/v1/local-authorities", adminToken);
        Response getViewer = http.doGet("/courts/" + courtId + "/v1/local-authorities", viewerToken);
        assertViewerAllowed(getAdmin, getViewer, "/courts/{courtId}/v1/local-authorities [GET]");

        final List<CourtLocalAuthorityDto> authorities = mapper.readValue(
            getAdmin.asString(), new TypeReference<List<CourtLocalAuthorityDto>>() { }
        );
        final Response putAdmin = http.doPut("/courts/" + courtId + "/v1/local-authorities", authorities, adminToken);
        final Response putViewer = http.doPut("/courts/" + courtId + "/v1/local-authorities", authorities, viewerToken);
        assertThat(putAdmin.statusCode()).isEqualTo(200);
        assertViewerForbidden(putViewer, "/courts/{courtId}/v1/local-authorities [PUT]");
    }

    @Test
    @DisplayName("Court lock endpoints enforce admin-only writes")
    void courtLockAuth() throws Exception {
        UUID courtId = createCourtAsAdmin("Test Court Auth Lock");
        UUID userId = createUserAsAdmin("test.user.auth.lock");
        String lockPath = "/courts/" + courtId + "/v1/locks/" + Page.COURT;

        Response postAdmin = http.doPost(lockPath, mapper.writeValueAsString(userId), adminToken);
        assertThat(postAdmin.statusCode()).isIn(200, 201);

        assertViewerAllowed(
            http.doGet("/courts/" + courtId + "/v1/locks", adminToken),
            http.doGet("/courts/" + courtId + "/v1/locks", viewerToken),
            "/courts/{courtId}/v1/locks [GET]"
        );
        assertViewerAllowed(
            http.doGet(lockPath, adminToken),
            http.doGet(lockPath, viewerToken),
            "/courts/{courtId}/v1/locks/{page} [GET]"
        );
        assertViewerForbidden(
            http.doPost(lockPath, mapper.writeValueAsString(userId), viewerToken),
            "/courts/{courtId}/v1/locks/{page} [POST]"
        );
        assertViewerForbidden(
            http.doDelete(lockPath, viewerToken),
            "/courts/{courtId}/v1/locks/{page} [DELETE]"
        );
    }

    @Test
    @DisplayName("Court opening hours endpoints enforce admin-only writes")
    void courtOpeningHoursAuth() {
        UUID courtId = createCourtAsAdmin("Test Court Auth OH");
        UUID typeId = TestDataHelper.getOpeningHourTypeId(http, 0);
        List<CourtOpeningHours> hours = List.of(
            CourtOpeningHours.builder()
                .courtId(courtId)
                .openingHourTypeId(typeId)
                .dayOfWeek(DayOfTheWeek.MONDAY)
                .openingHour(LocalTime.of(9, 0))
                .closingHour(LocalTime.of(17, 0))
                .build()
        );
        List<CourtCounterServiceOpeningHours> counter = List.of(
            CourtCounterServiceOpeningHours.builder()
                .courtId(courtId)
                .dayOfWeek(DayOfTheWeek.MONDAY)
                .openingHour(LocalTime.of(9, 0))
                .closingHour(LocalTime.of(16, 0))
                .counterService(true)
                .assistWithForms(true)
                .assistWithDocuments(true)
                .assistWithSupport(false)
                .appointmentNeeded(false)
                .build()
        );

        Response putAdmin = http.doPut("/courts/" + courtId + "/v1/opening-hours/" + typeId, hours, adminToken);
        assertThat(putAdmin.statusCode()).isEqualTo(200);
        Response counterAdmin = http.doPut(
            "/courts/" + courtId + "/v1/opening-hours/counter-service",
            counter,
            adminToken
        );
        assertThat(counterAdmin.statusCode())
            .as("Admin PUT opening-hours counter-service failed. Body: %s", counterAdmin.asString())
            .isEqualTo(200);

        assertViewerAllowed(
            http.doGet("/courts/" + courtId + "/v1/opening-hours", adminToken),
            http.doGet("/courts/" + courtId + "/v1/opening-hours", viewerToken),
            "/courts/{courtId}/v1/opening-hours [GET]"
        );
        assertViewerAllowed(
            http.doGet("/courts/" + courtId + "/v1/opening-hours/" + typeId, adminToken),
            http.doGet("/courts/" + courtId + "/v1/opening-hours/" + typeId, viewerToken),
            "/courts/{courtId}/v1/opening-hours/{openingHourTypeId} [GET]"
        );
        assertViewerAllowed(
            http.doGet("/courts/" + courtId + "/v1/opening-hours/counter-service", adminToken),
            http.doGet("/courts/" + courtId + "/v1/opening-hours/counter-service", viewerToken),
            "/courts/{courtId}/v1/opening-hours/counter-service [GET]"
        );
        assertViewerForbidden(
            http.doPut("/courts/" + courtId + "/v1/opening-hours/" + typeId, hours, viewerToken),
            "/courts/{courtId}/v1/opening-hours/{openingHourTypeId} [PUT]"
        );
        assertViewerForbidden(
            http.doPut("/courts/" + courtId + "/v1/opening-hours/counter-service", counter, viewerToken),
            "/courts/{courtId}/v1/opening-hours/counter-service [PUT]"
        );
        assertViewerForbidden(
            http.doDelete("/courts/" + courtId + "/v1/opening-hours/" + typeId, viewerToken),
            "/courts/{courtId}/v1/opening-hours/{openingHourTypeId} [DELETE]"
        );
    }

    @Test
    @DisplayName("Court photo endpoints enforce admin-only writes")
    void courtPhotoAuth() {
        UUID courtId = createCourtAsAdmin("Test Court Auth Photo");
        File image = new File("src/functionalTest/resources/test-images/test valid jpg 1.2 MB.jpg");

        Response uploadAdmin = http.doMultipartPost("/courts/" + courtId + "/v1/photo", "file", image, adminToken);
        assertThat(uploadAdmin.statusCode()).isEqualTo(201);

        assertViewerAllowed(
            http.doGet("/courts/" + courtId + "/v1/photo", adminToken),
            http.doGet("/courts/" + courtId + "/v1/photo", viewerToken),
            "/courts/{courtId}/v1/photo [GET]"
        );
        assertViewerForbidden(
            http.doMultipartPost("/courts/" + courtId + "/v1/photo", "file", image, viewerToken),
            "/courts/{courtId}/v1/photo [POST]"
        );
        assertViewerForbidden(
            http.doDelete("/courts/" + courtId + "/v1/photo", viewerToken),
            "/courts/{courtId}/v1/photo [DELETE]"
        );
    }

    @Test
    @DisplayName("Court professional information endpoints enforce admin-only writes")
    void courtProfessionalInformationAuth() {
        final UUID courtId = createCourtAsAdmin("Test Court Auth PI");
        final ProfessionalInformationDto pi = new ProfessionalInformationDto();
        pi.setInterviewRooms(true);
        pi.setInterviewRoomCount(2);
        pi.setInterviewPhoneNumber("0207 123 4567");
        pi.setVideoHearings(true);
        pi.setCommonPlatform(true);
        pi.setAccessScheme(true);
        final CourtProfessionalInformationDetailsDto payload = new CourtProfessionalInformationDetailsDto();
        payload.setProfessionalInformation(pi);

        Response postAdmin = http.doPost("/courts/" + courtId + "/v1/professional-information", payload, adminToken);
        assertThat(postAdmin.statusCode())
            .as("Admin POST professional-information failed. Body: %s", postAdmin.asString())
            .isEqualTo(201);

        assertViewerAllowed(
            http.doGet("/courts/" + courtId + "/v1/professional-information", adminToken),
            http.doGet("/courts/" + courtId + "/v1/professional-information", viewerToken),
            "/courts/{courtId}/v1/professional-information [GET]"
        );
        assertViewerForbidden(
            http.doPost("/courts/" + courtId + "/v1/professional-information", payload, viewerToken),
            "/courts/{courtId}/v1/professional-information [POST]"
        );
    }

    @Test
    @DisplayName("Court single points of entry endpoints enforce admin-only writes")
    void courtSinglePointsOfEntryAuth() throws Exception {
        UUID courtId = createCourtAsAdmin("Test Court Auth SPOE");

        Response getAdmin = http.doGet("/courts/" + courtId + "/v1/single-point-of-entry", adminToken);
        Response getViewer = http.doGet("/courts/" + courtId + "/v1/single-point-of-entry", viewerToken);
        assertViewerAllowed(getAdmin, getViewer, "/courts/{courtId}/v1/single-point-of-entry [GET]");

        List<AreaOfLawSelectionDto> payload = mapper.readValue(
            getAdmin.asString(),
            new TypeReference<List<AreaOfLawSelectionDto>>() { }
        );
        payload.forEach(item -> item.setSelected("Adoption".equals(item.getName())));

        Response putAdmin = http.doPut("/courts/" + courtId + "/v1/single-point-of-entry", payload, adminToken);
        Response putViewer = http.doPut("/courts/" + courtId + "/v1/single-point-of-entry", payload, viewerToken);
        assertThat(putAdmin.statusCode()).isEqualTo(200);
        assertViewerForbidden(putViewer, "/courts/{courtId}/v1/single-point-of-entry [PUT]");
    }

    @Test
    @DisplayName("Court translation endpoints enforce admin-only writes")
    void courtTranslationAuth() {
        UUID courtId = createCourtAsAdmin("Test Court Auth Translation");
        CourtTranslation payload = new CourtTranslation();
        payload.setCourtId(courtId);
        payload.setEmail("translation@test.gov.uk");
        payload.setPhoneNumber("01234567890");

        Response postAdmin = http.doPost("/courts/" + courtId + "/v1/translation-services", payload, adminToken);
        assertThat(postAdmin.statusCode()).isEqualTo(201);

        assertViewerAllowed(
            http.doGet("/courts/" + courtId + "/v1/translation-services", adminToken),
            http.doGet("/courts/" + courtId + "/v1/translation-services", viewerToken),
            "/courts/{courtId}/v1/translation-services [GET]"
        );
        assertViewerForbidden(
            http.doPost("/courts/" + courtId + "/v1/translation-services", payload, viewerToken),
            "/courts/{courtId}/v1/translation-services [POST]"
        );
    }

    @Test
    @DisplayName("User endpoints enforce admin-only writes and viewer-readable favourites")
    void userControllerAuth() {
        final UUID userId = createUserAsAdmin("test.user.auth.main");
        final UUID courtId = createCourtAsAdmin("Test Court Auth User Favs");
        final List<UUID> favourites = List.of(courtId);

        Response addFavAdmin = http.doPost("/user/v1/" + userId + "/favourites", favourites, adminToken);
        assertThat(addFavAdmin.statusCode()).isIn(200, 201, 204);

        assertViewerAllowed(
            http.doGet("/user/v1/" + userId + "/favourites", adminToken),
            http.doGet("/user/v1/" + userId + "/favourites", viewerToken),
            "/user/v1/{userId}/favourites [GET]"
        );

        final User postBody = User.builder()
            .email("test.user.auth.write." + System.currentTimeMillis() + "@justice.gov.uk")
            .ssoId(UUID.randomUUID())
            .build();

        assertViewerForbidden(
            http.doPost("/user/v1/" + userId + "/favourites", favourites, viewerToken),
            "/user/v1/{userId}/favourites [POST]"
        );
        assertViewerForbidden(
            http.doDelete("/user/v1/" + userId + "/favourites/" + courtId, viewerToken),
            "/user/v1/{userId}/favourites/{favouriteId} [DELETE]"
        );
        assertViewerForbidden(
            http.doDelete("/user/v1/" + userId + "/locks", viewerToken),
            "/user/v1/{userId}/locks [DELETE]"
        );
        assertViewerForbidden(
            http.doPost("/user/v1", postBody, viewerToken),
            "/user/v1 [POST]"
        );
        assertViewerForbidden(
            http.doDelete("/user/v1/retention", viewerToken),
            "/user/v1/retention [DELETE]"
        );
    }

    @Test
    @DisplayName("Search endpoints require auth but allow admin and viewer")
    void searchEndpointsAuth() {
        Map<String, String> endpoints = Map.of(
            "/search/address/v1/postcode/SW1A1AA", "address",
            "/search/courts/v1/postcode?postcode=SW1A1AA&limit=5", "courts by postcode",
            "/search/courts/v1/prefix?prefix=A", "courts by prefix",
            "/search/courts/v1/name?q=court", "courts by name",
            "/search/service-area/v1/adoption", "service area",
            "/search/services/v1", "services",
            "/search/services/v1/divorce/service-areas", "service areas by service"
        );

        for (String endpoint : endpoints.keySet()) {
            Response admin = http.doGet(endpoint, adminToken);
            Response viewer = http.doGet(endpoint, viewerToken);
            Response noToken = http.doGet(endpoint, "");
            assertViewerAllowed(admin, viewer, endpoint);
            assertUnauthenticated(noToken, endpoint);
        }
    }

    @Test
    @DisplayName("Testing support endpoint requires auth and allows both roles")
    void testingSupportEndpointAuth() {
        String endpoint = "/testing-support/courts/name-prefix/Auth Test Cleanup";
        Response admin = http.doDelete(endpoint, adminToken);
        Response viewer = http.doDelete(endpoint, viewerToken);
        Response noToken = http.doDelete(endpoint, "");
        assertViewerAllowed(admin, viewer, endpoint);
        assertUnauthenticated(noToken, endpoint);
    }

    @AfterAll
    static void cleanUp() {
        http.doDelete("/testing-support/courts/name-prefix/Test Court", adminToken);
        http.doDelete("/testing-support/courts/name-prefix/Auth Test", adminToken);
    }
}
