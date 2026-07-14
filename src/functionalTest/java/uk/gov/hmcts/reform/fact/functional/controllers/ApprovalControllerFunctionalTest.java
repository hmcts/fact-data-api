package uk.gov.hmcts.reform.fact.functional.controllers;

import io.qameta.allure.Feature;
import io.restassured.response.Response;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import uk.gov.hmcts.reform.fact.data.api.entities.Approval;
import uk.gov.hmcts.reform.fact.data.api.entities.types.SubjectType;
import uk.gov.hmcts.reform.fact.functional.helpers.AssertionHelper;
import uk.gov.hmcts.reform.fact.functional.helpers.TestDataHelper;
import uk.gov.hmcts.reform.fact.functional.http.HttpClient;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.CREATED;
import static org.springframework.http.HttpStatus.NO_CONTENT;
import static org.springframework.http.HttpStatus.NOT_FOUND;
import static org.springframework.http.HttpStatus.OK;

@Feature("Approval Controller")
@DisplayName("Approval Controller")
public final class ApprovalControllerFunctionalTest {

    private static final HttpClient http = new HttpClient();
    private static final List<UUID> approvalIdsToDelete = new CopyOnWriteArrayList<>();

    @Test
    @DisplayName("POST /approvals/v1 creates approval and GET /approvals/v1 returns it")
    void shouldCreateAndReturnApprovalSuccessfully() {
        final UUID courtId = TestDataHelper.createCourt(http, "Test Court Approval");
        final UUID userId = TestDataHelper.createUser(http, "test.approval.create");
        final Approval approval = createApproval(courtId, SubjectType.COURT, userId);

        final Response createResponse = http.doPost("/approvals/v1", approval);
        AssertionHelper.assertStatus(createResponse, CREATED);

        final UUID approvalId = UUID.fromString(createResponse.jsonPath().getString("id"));
        approvalIdsToDelete.add(approvalId);

        assertThat(createResponse.jsonPath().getString("subjectId"))
            .as("Created approval should reference the requested court")
            .isEqualTo(courtId.toString());
        assertThat(createResponse.jsonPath().getString("subjectType"))
            .as("Created approval should reference a court subject")
            .isEqualTo(SubjectType.COURT.name());
        assertThat(createResponse.jsonPath().getString("userId"))
            .as("Created approval should reference the requested user")
            .isEqualTo(userId.toString());
        assertThat(createResponse.jsonPath().getString("lastUpdatedAt"))
            .as("Created approval should include a last updated timestamp")
            .isNotBlank();

        final Response getResponse = http.doGet("/approvals/v1");
        AssertionHelper.assertStatus(getResponse, OK);

        final String approvalPath = "find { it.subjectId == '" + courtId + "' && it.subjectType == 'COURT' }";
        assertThat(getResponse.jsonPath().getString(approvalPath + ".name"))
            .as("GET approvals should include the created court")
            .isNotBlank();
        assertThat(getResponse.jsonPath().getBoolean(approvalPath + ".approved"))
            .as("GET approvals should mark the created court as approved")
            .isTrue();
        assertThat(getResponse.jsonPath().getString(approvalPath + ".approvalId"))
            .as("GET approvals should include the approval ID for deleting")
            .isEqualTo(approvalId.toString());
        assertThat(getResponse.jsonPath().getString(approvalPath + ".user.id"))
            .as("GET approvals should include the approver user")
            .isEqualTo(userId.toString());
        assertThat(getResponse.jsonPath().getString(approvalPath + ".user.email"))
            .as("GET approvals should include the approver email")
            .startsWith("test.approval.create.");
    }

    @Test
    @DisplayName("POST /approvals/v1 creates service centre approval")
    void shouldCreateServiceCentreApprovalSuccessfully() {
        final UUID serviceCentreId = TestDataHelper.createServiceCentre(http, "Test Service Centre Approval");
        final UUID userId = TestDataHelper.createUser(http, "test.approval.service.centre");
        final Approval approval = createApproval(serviceCentreId, SubjectType.SERVICE_CENTRE, userId);

        final Response createResponse = http.doPost("/approvals/v1", approval);
        AssertionHelper.assertStatus(createResponse, CREATED);

        final UUID approvalId = UUID.fromString(createResponse.jsonPath().getString("id"));
        approvalIdsToDelete.add(approvalId);

        assertThat(createResponse.jsonPath().getString("subjectId"))
            .as("Created approval should reference the requested service centre")
            .isEqualTo(serviceCentreId.toString());
        assertThat(createResponse.jsonPath().getString("subjectType"))
            .as("Created approval should reference a service centre subject")
            .isEqualTo(SubjectType.SERVICE_CENTRE.name());
    }

    @Test
    @DisplayName("DELETE /approvals/{approvalId}/v1 deletes approval")
    void shouldDeleteApprovalSuccessfully() {
        final UUID courtId = TestDataHelper.createCourt(http, "Test Court Approval Delete");
        final UUID userId = TestDataHelper.createUser(http, "test.approval.delete");
        final Response createResponse = http.doPost("/approvals/v1", createApproval(courtId, SubjectType.COURT,
                                                                                    userId));
        AssertionHelper.assertStatus(createResponse, CREATED);

        final UUID approvalId = UUID.fromString(createResponse.jsonPath().getString("id"));
        approvalIdsToDelete.add(approvalId);

        final Response deleteResponse = http.doDelete("/approvals/" + approvalId + "/v1");
        AssertionHelper.assertStatus(deleteResponse, NO_CONTENT);
        approvalIdsToDelete.remove(approvalId);

        final Response secondDeleteResponse = http.doDelete("/approvals/" + approvalId + "/v1");
        AssertionHelper.assertStatus(secondDeleteResponse, NOT_FOUND);
    }

    @Test
    @DisplayName("POST /approvals/v1 returns 400 for invalid approval")
    void shouldReturnBadRequestForInvalidApproval() {
        final Approval approval = Approval.builder()
            .subjectId(UUID.randomUUID())
            .subjectType(SubjectType.COURT)
            .build();

        final Response response = http.doPost("/approvals/v1", approval);
        AssertionHelper.assertStatus(response, BAD_REQUEST);
    }

    @Test
    @DisplayName("POST /approvals/v1 returns 404 when subject does not exist")
    void shouldReturnNotFoundWhenSubjectDoesNotExist() {
        final UUID userId = TestDataHelper.createUser(http, "test.approval.missing.subject");
        final Approval approval = createApproval(UUID.randomUUID(), SubjectType.COURT, userId);

        final Response response = http.doPost("/approvals/v1", approval);
        AssertionHelper.assertStatus(response, NOT_FOUND);

        assertThat(response.jsonPath().getString("message"))
            .as("Error message should mention court not found")
            .contains("Court not found");
    }

    @Test
    @DisplayName("DELETE /approvals/{approvalId}/v1 returns 404 for unknown approval")
    void shouldReturnNotFoundWhenDeletingUnknownApproval() {
        final Response response = http.doDelete("/approvals/" + UUID.randomUUID() + "/v1");
        AssertionHelper.assertStatus(response, NOT_FOUND);
    }

    private static Approval createApproval(final UUID subjectId, final SubjectType subjectType,
                                           final UUID userId) {
        return Approval.builder()
            .subjectId(subjectId)
            .subjectType(subjectType)
            .userId(userId)
            .build();
    }

    @AfterAll
    static void cleanUp() {
        approvalIdsToDelete.forEach(approvalId -> http.doDelete("/approvals/" + approvalId + "/v1"));
        http.doDelete("/testing-support/courts/name-prefix/Test Court Approval");
        http.doDelete("/testing-support/service-centres/name-prefix/Test Service Centre Approval");
    }
}
