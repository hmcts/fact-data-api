package uk.gov.hmcts.reform.fact.functional.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import io.qameta.allure.Feature;
import io.restassured.response.Response;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import uk.gov.hmcts.reform.fact.data.api.entities.Court;
import uk.gov.hmcts.reform.fact.data.api.entities.CourtLock;
import uk.gov.hmcts.reform.fact.data.api.entities.User;
import uk.gov.hmcts.reform.fact.data.api.entities.types.Page;
import uk.gov.hmcts.reform.fact.functional.helpers.TestDataHelper;
import uk.gov.hmcts.reform.fact.functional.http.HttpClient;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.HttpStatus.CREATED;
import static org.springframework.http.HttpStatus.NOT_FOUND;
import static org.springframework.http.HttpStatus.NO_CONTENT;
import static org.springframework.http.HttpStatus.OK;

@Feature("User Controller")
@DisplayName("User Controller")
public final class UserControllerFunctionalTest {

    private static final HttpClient http = new HttpClient();
    private static final ObjectMapper mapper = new ObjectMapper()
        .registerModule(new JavaTimeModule())
        .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

    @Test
    @DisplayName("POST /user/v1 creates user and returns created user with ID")
    void shouldReturnCreatedUserSuccessfully() throws Exception {
        final User user = User.builder()
            .email("test.user." + System.currentTimeMillis() + "@justice.gov.uk")
            .ssoId(UUID.randomUUID())
            .build();

        final Response createResponse = http.doPost("/user/v1", user);

        assertThat(createResponse.statusCode())
            .as("Expected 201 CREATED when creating user with valid payload")
            .isEqualTo(CREATED.value());

        assertThat(createResponse.contentType())
            .as("Expected JSON content type when creating user")
            .contains("json");

        final User createdUser = mapper.readValue(createResponse.getBody().asString(), User.class);

        assertThat(createdUser.getId())
            .as("Created user should have auto-generated ID")
            .isNotNull();
        assertThat(createdUser.getEmail())
            .as("Created user email should match request")
            .isEqualTo(user.getEmail());
        assertThat(createdUser.getSsoId())
            .as("Created user SSO ID should match request")
            .isEqualTo(user.getSsoId());
        assertThat(createdUser.getLastLogin())
            .as("Created user should have lastLogin timestamp set")
            .isNotNull();
    }

    @Test
    @DisplayName("POST /user/v1/{userId}/favourites adds two courts as favorites")
    void shouldReturnCreatedFavouritesSuccessfully() throws Exception {
        final UUID userId = TestDataHelper.createUser(http, "test.user.add.favourites");

        final UUID court1Id = TestDataHelper
            .createCourt(http, generateUniqueCourtName("Test Court Favourite One"));
        final UUID court2Id = TestDataHelper
            .createCourt(http, generateUniqueCourtName("Test Court Favourite Two"));

        final List<UUID> courtIds = List.of(court1Id, court2Id);

        final Response addFavouritesResponse = http.doPost(
            "/user/v1/" + userId + "/favourites",
            courtIds
        );

        assertThat(addFavouritesResponse.statusCode())
            .as("Expected 201 CREATED when adding favourite courts for user %s", userId)
            .isEqualTo(CREATED.value());
    }

    @Test
    @DisplayName("GET /user/v1/{userId}/favourites returns full Court objects")
    void shouldReturnFullCourtObjectsForFavouritesSuccessfully() throws Exception {
        final UUID userId = TestDataHelper.createUser(http, "test.user.favourites");

        final UUID court1Id = TestDataHelper
            .createCourt(http, generateUniqueCourtName("Test Court Get Favourites One"));
        final UUID court2Id = TestDataHelper
            .createCourt(http, generateUniqueCourtName("Test Court Get Favourites Two"));

        final List<UUID> courtIds = List.of(court1Id, court2Id);

        final Response addFavouritesResponse = http.doPost(
            "/user/v1/" + userId + "/favourites",
            courtIds
        );

        assertThat(addFavouritesResponse.statusCode())
            .as("Expected 201 CREATED when adding favourite courts")
            .isEqualTo(CREATED.value());

        final Response getFavouritesResponse = http.doGet("/user/v1/" + userId + "/favourites");

        assertThat(getFavouritesResponse.statusCode())
            .as("Expected 200 OK when getting favourites for user %s", userId)
            .isEqualTo(OK.value());

        assertThat(getFavouritesResponse.contentType())
            .as("Expected JSON content type when getting favourites for user %s", userId)
            .contains("json");

        final List<Court> favouriteCourts = getFavouritesResponse.jsonPath().getList("", Court.class);

        assertThat(favouriteCourts)
            .as("Expected 2 favourite courts for user %s", userId)
            .hasSize(2);

        assertThat(favouriteCourts)
            .as("Expected favourite courts to contain court IDs %s and %s", court1Id, court2Id)
            .extracting(Court::getId)
            .containsExactlyInAnyOrder(court1Id, court2Id);

        assertThat(favouriteCourts)
            .as("Expected favourite courts to have names populated")
            .allSatisfy(court -> assertThat(court.getName())
                .as("Expected favourite court name to be populated")
                .isNotNull());
    }

    @Test
    @DisplayName("GET /user/v1/{userId}/favourites returns 404 for non-existent user")
    void shouldReturnNotFoundWhenGettingFavouritesForNonExistentUserSuccessfully() {
        final UUID nonExistentUserId = UUID.randomUUID();

        final Response getFavouritesResponse = http.doGet("/user/v1/" + nonExistentUserId + "/favourites");

        assertThat(getFavouritesResponse.statusCode())
            .as("Expected 404 NOT FOUND for non-existent user %s", nonExistentUserId)
            .isEqualTo(NOT_FOUND.value());

        assertThat(getFavouritesResponse.contentType())
            .as("Expected JSON content type for non-existent user %s", nonExistentUserId)
            .contains("json");

        assertThat(getFavouritesResponse.jsonPath().getString("message"))
            .as("Error message should mention user not found")
            .contains("No user found");
    }

    @Test
    @DisplayName("POST /user/v1/{userId}/favourites returns 404 for non-existent user")
    void shouldReturnNotFoundWhenAddingFavouritesForNonExistentUserSuccessfully() {
        final UUID nonExistentUserId = UUID.randomUUID();
        final UUID courtId = TestDataHelper
            .createCourt(http, generateUniqueCourtName("Test Court Non-Existent User"));

        final List<UUID> courtIds = List.of(courtId);

        final Response addFavouritesResponse = http.doPost(
            "/user/v1/" + nonExistentUserId + "/favourites",
            courtIds
        );

        assertThat(addFavouritesResponse.statusCode())
            .as("Expected 404 NOT FOUND when adding favourites for non-existent user %s", nonExistentUserId)
            .isEqualTo(NOT_FOUND.value());

        assertThat(addFavouritesResponse.contentType())
            .as("Expected JSON content type for non-existent user %s", nonExistentUserId)
            .contains("json");

        assertThat(addFavouritesResponse.jsonPath().getString("message"))
            .as("Error message should mention user not found")
            .contains("No user found");
    }

    @Test
    @DisplayName("DELETE /user/v1/{userId}/favourites/{favouriteId} removes favourite successfully")
    void shouldReturnNoContentWhenRemovingFavouriteSuccessfully() throws Exception {
        final UUID userId = TestDataHelper.createUser(http, "test.user.remove.favourite");

        final UUID court1Id = TestDataHelper
            .createCourt(http, generateUniqueCourtName("Test Court Remove Favourite One"));
        final UUID court2Id = TestDataHelper
            .createCourt(http, generateUniqueCourtName("Test Court Remove Favourite Two"));

        final List<UUID> courtIds = List.of(court1Id, court2Id);

        final Response addFavouritesResponse = http.doPost(
            "/user/v1/" + userId + "/favourites",
            courtIds
        );

        assertThat(addFavouritesResponse.statusCode())
            .as("Expected 201 CREATED when adding favourite courts")
            .isEqualTo(CREATED.value());

        final Response deleteFavouriteResponse = http.doDelete(
            "/user/v1/" + userId + "/favourites/" + court1Id
        );

        assertThat(deleteFavouriteResponse.statusCode())
            .as("Expected 204 NO CONTENT when removing favourite court %s for user %s", court1Id, userId)
            .isEqualTo(NO_CONTENT.value());

        final Response getFavouritesResponse = http.doGet("/user/v1/" + userId + "/favourites");

        assertThat(getFavouritesResponse.statusCode())
            .as("Expected 200 OK when getting favourites after removal")
            .isEqualTo(OK.value());

        assertThat(getFavouritesResponse.contentType())
            .as("Expected JSON content type when getting favourites after removal")
            .contains("json");

        final List<Court> remainingFavourites = getFavouritesResponse.jsonPath().getList("", Court.class);

        assertThat(remainingFavourites)
            .as("Expected 1 favourite court remaining after removal")
            .hasSize(1);

        assertThat(remainingFavourites)
            .as("Expected only court %s to remain in favourites", court2Id)
            .extracting(Court::getId)
            .containsOnly(court2Id);
    }

    @Test
    @DisplayName("DELETE /user/v1/{userId}/favourites/{favouriteId} returns 404 for non-existent user")
    void shouldReturnNotFoundWhenRemovingFavouriteForNonExistentUserSuccessfully() {
        final UUID nonExistentUserId = UUID.randomUUID();
        final UUID courtId = UUID.randomUUID();

        final Response deleteFavouriteResponse = http.doDelete(
            "/user/v1/" + nonExistentUserId + "/favourites/" + courtId
        );

        assertThat(deleteFavouriteResponse.statusCode())
            .as("Expected 404 NOT FOUND when removing favourite for non-existent user %s", nonExistentUserId)
            .isEqualTo(NOT_FOUND.value());

        assertThat(deleteFavouriteResponse.contentType())
            .as("Expected JSON content type for non-existent user %s", nonExistentUserId)
            .contains("json");

        assertThat(deleteFavouriteResponse.jsonPath().getString("message"))
            .as("Error message should mention user not found")
            .contains("No user found");
    }

    @Test
    @DisplayName("DELETE /user/v1/{userId}/locks clears user locks successfully")
    void shouldReturnNoContentWhenClearingUserLocksSuccessfully() throws Exception {
        final UUID userId = TestDataHelper.createUser(http, "test.user.clear.locks");
        final UUID court1Id = TestDataHelper
            .createCourt(http, generateUniqueCourtName("Test Court Clear Locks One"));
        final UUID court2Id = TestDataHelper
            .createCourt(http, generateUniqueCourtName("Test Court Clear Locks Two"));

        TestDataHelper.createCourtLock(http, court1Id, Page.COURT, userId);
        TestDataHelper.createCourtLock(http, court2Id, Page.COURT_ACCESSIBILITY, userId);

        final Response getLocksBeforeClearResponse = http.doGet("/courts/" + court1Id + "/v1/locks");

        assertThat(getLocksBeforeClearResponse.statusCode())
            .as("Expected 200 OK when getting locks before clearing")
            .isEqualTo(OK.value());

        final List<CourtLock> locksBeforeClear = getLocksBeforeClearResponse.jsonPath()
            .getList("", CourtLock.class);

        assertThat(locksBeforeClear)
            .as("Expected at least 1 lock before clearing for user %s", userId)
            .isNotEmpty();

        final Response clearLocksResponse = http.doDelete("/user/v1/" + userId + "/locks");

        assertThat(clearLocksResponse.statusCode())
            .as("Expected 204 NO CONTENT when clearing locks for user %s", userId)
            .isEqualTo(NO_CONTENT.value());

        final Response getLock1StatusResponse = http.doGet("/courts/" + court1Id + "/v1/locks/" + Page.COURT);

        assertThat(getLock1StatusResponse.statusCode())
            .as("Expected 200 OK when checking lock status after clearing")
            .isEqualTo(OK.value());

        assertThat(getLock1StatusResponse.getBody().asString())
            .as("Expected null response after clearing user locks")
            .isEqualTo("null");

        final Response getLock2StatusResponse = http.doGet("/courts/"
                                                               + court2Id + "/v1/locks/" + Page.COURT_ACCESSIBILITY);

        assertThat(getLock2StatusResponse.statusCode())
            .as("Expected 200 OK when checking second lock status after clearing")
            .isEqualTo(OK.value());

        assertThat(getLock2StatusResponse.getBody().asString())
            .as("Expected null response for second lock after clearing user locks")
            .isEqualTo("null");
    }

    @Test
    @DisplayName("DELETE /user/v1/retention deletes inactive users successfully")
    void shouldReturnNoContentWhenDeletingInactiveUsersSuccessfully() {
        final Response deleteInactiveUsersResponse = http.doDelete("/user/v1/retention");

        assertThat(deleteInactiveUsersResponse.statusCode())
            .as("Expected 204 NO CONTENT when deleting inactive users")
            .isEqualTo(NO_CONTENT.value());
    }

    @Test
    @DisplayName("End-to-end: Create user, add favourites, get favourites, remove one, verify remaining")
    void shouldReturnEndToEndFavouriteWorkflowSuccessfully() throws Exception {
        // Setup: Create test user and courts
        final UUID userId = TestDataHelper.createUser(http, "test.user.endtoend");

        final UUID court1Id = TestDataHelper.createCourt(http, generateUniqueCourtName("Test Court One"));
        final UUID court2Id = TestDataHelper.createCourt(http, generateUniqueCourtName("Test Court Two"));
        final UUID court3Id = TestDataHelper.createCourt(http, generateUniqueCourtName("Test Court Three"));

        final List<UUID> courtIds = List.of(court1Id, court2Id, court3Id);

        // Add 3 courts as favourites
        final Response addFavouritesResponse = http.doPost(
            "/user/v1/" + userId + "/favourites",
            courtIds
        );

        assertThat(addFavouritesResponse.statusCode())
            .as("Expected 201 CREATED when adding 3 favourite courts")
            .isEqualTo(CREATED.value());

        // Verify all 3 favourites exist
        final Response getFavouritesResponse1 = http.doGet("/user/v1/" + userId + "/favourites");

        assertThat(getFavouritesResponse1.statusCode())
            .as("Expected 200 OK when getting favourites")
            .isEqualTo(OK.value());

        assertThat(getFavouritesResponse1.contentType())
            .as("Expected JSON content type when getting favourites")
            .contains("json");

        final List<Court> initialFavourites = getFavouritesResponse1.jsonPath().getList("", Court.class);

        assertThat(initialFavourites)
            .as("Expected 3 favourite courts after adding")
            .hasSize(3);

        // Remove one favourite
        final Response deleteFavouriteResponse = http.doDelete(
            "/user/v1/" + userId + "/favourites/" + court2Id
        );

        assertThat(deleteFavouriteResponse.statusCode())
            .as("Expected 204 NO CONTENT when removing favourite")
            .isEqualTo(NO_CONTENT.value());

        // Verify only 2 favourites remain
        final Response getFavouritesResponse2 = http.doGet("/user/v1/" + userId + "/favourites");

        assertThat(getFavouritesResponse2.statusCode())
            .as("Expected 200 OK when getting favourites after removal")
            .isEqualTo(OK.value());

        assertThat(getFavouritesResponse2.contentType())
            .as("Expected JSON content type when getting favourites after removal")
            .contains("json");

        final List<Court> remainingFavourites = getFavouritesResponse2.jsonPath().getList("", Court.class);

        assertThat(remainingFavourites)
            .as("Expected 2 favourite courts after removing one")
            .hasSize(2);

        assertThat(remainingFavourites)
            .as("Expected courts %s and %s to remain, %s to be removed", court1Id, court3Id, court2Id)
            .extracting(Court::getId)
            .containsExactlyInAnyOrder(court1Id, court3Id);
    }

    /**
     * Generates a unique court name for test courts.
     * Court names can only contain letters, spaces, apostrophes, hyphens, ampersands, and parentheses.
     *
     * @param baseName the base court name
     * @return unique court name with letter-only suffix
     */
    private static String generateUniqueCourtName(final String baseName) {
        final String alphabet = "abcdefghijklmnopqrstuvwxyz";
        final StringBuilder suffix = new StringBuilder(4);
        for (int i = 0; i < 4; i++) {
            suffix.append(alphabet.charAt(ThreadLocalRandom.current().nextInt(26)));
        }
        return baseName + " " + suffix;
    }

    @AfterAll
    static void cleanUpTestData() {
        http.doDelete("/testing-support/courts/name-prefix/Test Court");
    }
}
