package uk.gov.hmcts.reform.fact.functional.controllers;

import io.qameta.allure.Feature;
import io.restassured.response.Response;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.cfg.DateTimeFeature;
import tools.jackson.databind.json.JsonMapper;
import uk.gov.hmcts.reform.fact.data.api.entities.Lock;
import uk.gov.hmcts.reform.fact.data.api.entities.User;
import uk.gov.hmcts.reform.fact.data.api.entities.types.Page;
import uk.gov.hmcts.reform.fact.data.api.entities.types.SubjectType;
import uk.gov.hmcts.reform.fact.data.api.entities.types.UserRole;
import uk.gov.hmcts.reform.fact.functional.helpers.TestDataHelper;
import uk.gov.hmcts.reform.fact.functional.http.HttpClient;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.HttpStatus.CREATED;
import static org.springframework.http.HttpStatus.NO_CONTENT;
import static org.springframework.http.HttpStatus.OK;

@Feature("User Controller")
@DisplayName("User Controller")
public final class UserControllerFunctionalTest {

    private static final HttpClient http = new HttpClient();
    private static final ObjectMapper mapper = JsonMapper.builder()
        .disable(DateTimeFeature.WRITE_DATES_AS_TIMESTAMPS)
        .build();

    @Test
    @DisplayName("POST /user/v1 creates user without the legacy favouriteCourts property")
    void shouldReturnCreatedUserSuccessfully() throws Exception {
        final User user = User.builder()
            .email("test.user." + System.currentTimeMillis() + "@justice.gov.uk")
            .ssoId(UUID.randomUUID())
            .role(UserRole.ADMIN)
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
        assertThat(createResponse.jsonPath().getMap("$"))
            .doesNotContainKeys("favouriteCourts", "favouriteServiceCentres");
    }

    @Test
    @DisplayName("DELETE /user/v1/{userId}/locks clears user locks successfully")
    void shouldReturnNoContentWhenClearingUserLocksSuccessfully() throws Exception {
        final UUID userId = TestDataHelper.createUser(http, "test.user.clear.locks");
        final UUID userId2 = TestDataHelper.createUser(http, "test.user2.clear.locks");
        final UUID court1Id = TestDataHelper
            .createCourt(http, generateUniqueCourtName("Test Court Clear Locks One"));
        final UUID court2Id = TestDataHelper
            .createCourt(http, generateUniqueCourtName("Test Court Clear Locks Two"));

        TestDataHelper.createCourtLock(http, court1Id, Page.GENERAL, userId);
        TestDataHelper.createCourtLock(http, court2Id, Page.ACCESSIBILITY, userId2);

        final Response getLocksBeforeClearResponse = http.doGet("/locks/" + SubjectType.COURT
                                                                    + "/" + court1Id + "/v1");

        assertThat(getLocksBeforeClearResponse.statusCode())
            .as("Expected 200 OK when getting locks before clearing")
            .isEqualTo(OK.value());

        final List<Lock> locksBeforeClear = getLocksBeforeClearResponse.jsonPath()
            .getList("", Lock.class);

        assertThat(locksBeforeClear)
            .as("Expected at least 1 lock before clearing for user %s", userId)
            .isNotEmpty();

        final Response clearLocksResponse = http.doDelete("/user/v1/" + userId + "/locks");

        assertThat(clearLocksResponse.statusCode())
            .as("Expected 204 NO CONTENT when clearing locks for user %s", userId)
            .isEqualTo(NO_CONTENT.value());

        final Response getLock1StatusResponse = http.doGet("/locks/" + SubjectType.COURT + "/"
                                                               + court1Id + "/v1/" + Page.GENERAL);

        assertThat(getLock1StatusResponse.statusCode())
            .as("Expected 204 OK when checking lock status after clearing")
            .isEqualTo(NO_CONTENT.value());

        assertThat(getLock1StatusResponse.getBody().asString())
            .as("Expected empty response after clearing user locks")
            .isEqualTo("");

        final Response clearLocksResponse2 = http.doDelete("/user/v1/" + userId2 + "/locks");

        assertThat(clearLocksResponse2.statusCode())
            .as("Expected 204 NO CONTENT when clearing locks for user %s", userId2)
            .isEqualTo(NO_CONTENT.value());

        final Response getLock2StatusResponse = http.doGet("/locks/" + SubjectType.COURT + "/"
                                                               + court2Id + "/v1/" + Page.ACCESSIBILITY);

        assertThat(getLock2StatusResponse.statusCode())
            .as("Expected 204 OK when checking second lock status after clearing")
            .isEqualTo(NO_CONTENT.value());

        assertThat(getLock2StatusResponse.getBody().asString())
            .as("Expected empty response for second lock after clearing user locks")
            .isEqualTo("");
    }

    @Test
    @DisplayName("DELETE /user/v1/retention deletes inactive users successfully")
    void shouldReturnNoContentWhenDeletingInactiveUsersSuccessfully() {
        final Response deleteInactiveUsersResponse = http.doDelete("/user/v1/retention");

        assertThat(deleteInactiveUsersResponse.statusCode())
            .as("Expected 204 NO CONTENT when deleting inactive users")
            .isEqualTo(NO_CONTENT.value());
    }

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
