package uk.gov.hmcts.reform.fact.functional.controllers;

import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.cfg.DateTimeFeature;
import tools.jackson.databind.json.JsonMapper;
import io.qameta.allure.Feature;
import io.restassured.response.Response;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import uk.gov.hmcts.reform.fact.data.api.entities.Lock;
import uk.gov.hmcts.reform.fact.data.api.entities.types.SubjectType;
import uk.gov.hmcts.reform.fact.data.api.entities.types.Page;
import uk.gov.hmcts.reform.fact.functional.helpers.TestDataHelper;
import uk.gov.hmcts.reform.fact.functional.http.HttpClient;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.HttpStatus.CONFLICT;
import static org.springframework.http.HttpStatus.NO_CONTENT;
import static org.springframework.http.HttpStatus.OK;

@Feature("Court Lock Controller")
@DisplayName("Court Lock Controller")
public final class LockControllerFunctionalTest {

    private static final HttpClient http = new HttpClient();
    private static final ObjectMapper mapper = JsonMapper.builder()
        .disable(DateTimeFeature.WRITE_DATES_AS_TIMESTAMPS)
        .build();

    @Test
    @DisplayName("POST /locks/{subjectType}/{subjectId}/v1/{page} creates lock successfully")
    void shouldCreateCourtLockSuccessfully() throws Exception {
        final UUID userId = TestDataHelper.createUser(http, "test.user.create");
        final UUID courtId = TestDataHelper.createCourt(http, "Test Lock Create");

        final Lock createdLock = TestDataHelper.createCourtLock(http, courtId, Page.GENERAL, userId);

        assertThat(createdLock.getId())
            .as("Created lock should have auto-generated ID")
            .isNotNull();
        assertThat(createdLock.getSubjectId())
            .as("Created lock court ID should match request court ID %s", courtId)
            .isEqualTo(courtId);
        assertThat(createdLock.getUserId())
            .as("Created lock user ID should match request user ID %s", userId)
            .isEqualTo(userId);
        assertThat(createdLock.getPage())
            .as("Created lock page should match request page %s", Page.GENERAL)
            .isEqualTo(Page.GENERAL);
        assertThat(createdLock.getLockAcquired())
            .as("Created lock should have lockAcquired timestamp set")
            .isNotNull();
    }

    @Test
    @DisplayName("GET /locks/{subjectType}/{subjectId}/v1/{page} returns lock status successfully")
    void shouldGetCourtLockStatusSuccessfully() throws Exception {
        final UUID userId = TestDataHelper.createUser(http, "test.user.getstatus");
        final UUID courtId = TestDataHelper.createCourt(http, "Test Lock Get Status");

        TestDataHelper.createCourtLock(http, courtId, Page.ACCESSIBILITY, userId);

        final Response getLockStatusResponse = http.doGet(
            "/locks/" + SubjectType.COURT + "/" + courtId + "/v1/" + Page.ACCESSIBILITY
        );

        assertThat(getLockStatusResponse.statusCode())
            .as("Expected 200 OK when getting lock status for court %s page %s",
                courtId, Page.ACCESSIBILITY
            )
            .isEqualTo(OK.value());

        assertThat(getLockStatusResponse.contentType())
            .as("Expected JSON content type when getting lock status")
            .contains("json");

        final Lock fetchedLock = mapper.readValue(
            getLockStatusResponse.getBody().asString(),
            Lock.class
        );

        assertThat(fetchedLock.getSubjectId())
            .as("Fetched lock court ID should match %s", courtId)
            .isEqualTo(courtId);
        assertThat(fetchedLock.getUserId())
            .as("Fetched lock user ID should match %s", userId)
            .isEqualTo(userId);
        assertThat(fetchedLock.getPage())
            .as("Fetched lock page should match %s", Page.ACCESSIBILITY)
            .isEqualTo(Page.ACCESSIBILITY);
        assertThat(fetchedLock.getLockAcquired())
            .as("Fetched lock should have lockAcquired timestamp")
            .isNotNull();
    }

    @Test
    @DisplayName("DELETE /locks/{subjectType}/{subjectId}/v1/{page} deletes lock successfully")
    void shouldDeleteCourtLockSuccessfully() throws Exception {
        final UUID userId = TestDataHelper.createUser(http, "test.user.delete");
        final UUID courtId = TestDataHelper.createCourt(http, "Test Lock Delete");

        TestDataHelper.createCourtLock(http, courtId, Page.LOCAL_AUTHORITIES, userId);

        final Response deleteLockResponse = http.doDelete(
            "/locks/" + SubjectType.COURT + "/" + courtId + "/v1/" + Page.LOCAL_AUTHORITIES
        );

        assertThat(deleteLockResponse.statusCode())
            .as("Expected 204 NO CONTENT when deleting lock for court %s page %s",
                courtId, Page.LOCAL_AUTHORITIES)
            .isEqualTo(NO_CONTENT.value());

        final Response getLockStatusResponse = http.doGet(
            "/locks/" + SubjectType.COURT + "/" + courtId + "/v1/" + Page.LOCAL_AUTHORITIES
        );

        assertThat(getLockStatusResponse.statusCode())
            .as("Expected 204 OK when checking lock status after deletion")
            .isEqualTo(NO_CONTENT.value());

        assertThat(getLockStatusResponse.getBody().asString())
            .as("Expected null response body after lock deletion indicating no lock exists")
            .isEqualTo("");
    }

    @Test
    @DisplayName("GET /locks/{subjectType}/{subjectId}/v1 returns all locks for a court")
    void shouldGetAllLocksForCourtSuccessfully() throws Exception {
        final UUID userId = TestDataHelper.createUser(http, "test.user.getall");
        final UUID courtId = TestDataHelper.createCourt(http, "Test Lock Get All");

        TestDataHelper.createCourtLock(http, courtId, Page.GENERAL, userId);
        TestDataHelper.createCourtLock(http, courtId, Page.ACCESSIBILITY, userId);

        final Response getAllLocksResponse = http.doGet("/locks/" + SubjectType.COURT + "/" + courtId + "/v1");

        assertThat(getAllLocksResponse.statusCode())
            .as("Expected 200 OK when getting all locks for court %s", courtId)
            .isEqualTo(OK.value());

        assertThat(getAllLocksResponse.contentType())
            .as("Expected JSON content type when getting all locks")
            .contains("json");

        final List<Lock> locks = getAllLocksResponse.jsonPath().getList("", Lock.class);

        assertThat(locks)
            .as("Expected 1 locks for court %s", courtId)
            .hasSize(1);

        assertThat(locks)
            .as("All locks should belong to court %s", courtId)
            .allSatisfy(lock -> assertThat(lock.getSubjectId())
                .as("Lock court ID should be %s", courtId)
                .isEqualTo(courtId));

        assertThat(locks)
            .as("Expected locks for pages COURT and COURT_ACCESSIBILITY")
            .extracting(Lock::getPage)
            .containsExactlyInAnyOrder(Page.ACCESSIBILITY);
    }

    @Test
    @DisplayName("POST /locks/{subjectType/{subjectId}/v1/{page} returns 409 when page already locked by another user")
    void shouldReturnConflictWhenPageLockedByAnotherUser() throws Exception {
        final UUID userAId = TestDataHelper.createUser(http, "test.user.conflict.usera");
        final UUID userBId = TestDataHelper.createUser(http, "test.user.conflict.userb");
        final UUID courtId = TestDataHelper.createCourt(http, "Test Court Lock Conflict");

        TestDataHelper.createCourtLock(http, courtId, Page.GENERAL, userAId);

        final Response userBLockResponse = http.doPost(
            "/locks/" + SubjectType.COURT + "/" + courtId + "/v1/" + Page.GENERAL,
            mapper.writeValueAsString(userBId)
        );

        assertThat(userBLockResponse.statusCode())
            .as("Expected 409 CONFLICT when user B tries to lock page already locked by user A")
            .isEqualTo(CONFLICT.value());

        assertThat(userBLockResponse.contentType())
            .as("Expected JSON content type for conflict response")
            .contains("json");

        assertThat(userBLockResponse.jsonPath().getString("error"))
            .as("Error field should indicate conflict")
            .isEqualTo("Conflict");

        assertThat(userBLockResponse.jsonPath().getInt("status"))
            .as("Status field should be 409")
            .isEqualTo(409);
    }

    @Test
    @DisplayName("POST /locks/{subjectType}/{subjectId}/v1/{page} allows same user to refresh lock")
    void shouldAllowSameUserToRefreshLock() throws Exception {
        final UUID userId = TestDataHelper.createUser(http, "test.user.refresh");
        final UUID courtId = TestDataHelper.createCourt(http, "Test Lock Refresh");

        final Lock initialLock = TestDataHelper.createCourtLock(http, courtId, Page.GENERAL, userId);

        final ZonedDateTime initialTimestamp = initialLock.getLockAcquired();

        Thread.sleep(1000);

        final Lock refreshedLock = TestDataHelper.createCourtLock(http, courtId, Page.GENERAL, userId);

        assertThat(refreshedLock.getId())
            .as("Lock ID should be regenerated after refresh")
            .isNotEqualTo(initialLock.getId());

        assertThat(refreshedLock.getLockAcquired())
            .as("Lock acquired timestamp should be updated after refresh")
            .isAfter(initialTimestamp);

        assertThat(refreshedLock.getSubjectId())
            .as("Lock court ID should remain %s", courtId)
            .isEqualTo(courtId);

        assertThat(refreshedLock.getUserId())
            .as("Lock user ID should remain %s", userId)
            .isEqualTo(userId);

        assertThat(refreshedLock.getPage())
            .as("Lock page should remain %s", Page.GENERAL)
            .isEqualTo(Page.GENERAL);
    }

    @Test
    @DisplayName("End-to-end: Create lock, get status, refresh lock, delete, verify deletion")
    void shouldCompleteEndToEndLockWorkflow() throws Exception {
        final UUID userId = TestDataHelper.createUser(http, "test.user.endtoend");
        final UUID courtId = TestDataHelper.createCourt(http, "Test Lock End To End");

        final Lock createdLock = TestDataHelper
            .createCourtLock(http, courtId, Page.LOCAL_AUTHORITIES, userId);

        assertThat(createdLock.getSubjectId())
            .as("Created lock court ID should be %s", courtId)
            .isEqualTo(courtId);

        final Response getLockStatusResponse = http.doGet(
            "/locks/" + SubjectType.COURT + "/" + courtId + "/v1/" + Page.LOCAL_AUTHORITIES
        );

        assertThat(getLockStatusResponse.statusCode())
            .as("Expected 200 OK when getting lock status")
            .isEqualTo(OK.value());

        final Lock fetchedLock = mapper.readValue(
            getLockStatusResponse.getBody().asString(),
            Lock.class
        );

        assertThat(fetchedLock.getId())
            .as("Fetched lock ID should match created lock ID")
            .isEqualTo(createdLock.getId());

        Thread.sleep(1000);

        final Lock refreshedLock = TestDataHelper
            .createCourtLock(http, courtId, Page.LOCAL_AUTHORITIES, userId);

        assertThat(refreshedLock.getLockAcquired())
            .as("Refreshed lock timestamp should be after original timestamp")
            .isAfter(createdLock.getLockAcquired());

        final Response deleteLockResponse = http.doDelete(
            "/locks/" + SubjectType.COURT + "/" + courtId + "/v1/" + Page.LOCAL_AUTHORITIES
        );

        assertThat(deleteLockResponse.statusCode())
            .as("Expected 204 NO CONTENT when deleting lock")
            .isEqualTo(NO_CONTENT.value());

        final Response verifyDeletedResponse = http.doGet(
            "/locks/" + SubjectType.COURT + "/" + courtId + "/v1/" + Page.LOCAL_AUTHORITIES
        );

        assertThat(verifyDeletedResponse.statusCode())
            .as("Expected 204 OK when checking deleted lock status")
            .isEqualTo(NO_CONTENT.value());

        assertThat(verifyDeletedResponse.getBody().asString())
            .as("Expected null response after lock deletion")
            .isEqualTo("");
    }

    @AfterAll
    static void cleanUpTestData() {
        http.doDelete("/testing-support/courts/name-prefix/Test Court Lock");
    }
}
