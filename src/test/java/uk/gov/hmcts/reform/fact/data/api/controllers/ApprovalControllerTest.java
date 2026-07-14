package uk.gov.hmcts.reform.fact.data.api.controllers;

import uk.gov.hmcts.reform.fact.data.api.dto.ApprovalStatus;
import uk.gov.hmcts.reform.fact.data.api.entities.Approval;
import uk.gov.hmcts.reform.fact.data.api.entities.User;
import uk.gov.hmcts.reform.fact.data.api.entities.types.SubjectType;
import uk.gov.hmcts.reform.fact.data.api.errorhandling.exceptions.NotFoundException;
import uk.gov.hmcts.reform.fact.data.api.services.ApprovalService;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ApprovalControllerTest {

    private static final UUID APPROVAL_ID = UUID.randomUUID();
    private static final UUID SUBJECT_ID = UUID.randomUUID();
    private static final UUID USER_ID = UUID.randomUUID();
    private static final String INVALID_UUID = "invalid-uuid";

    private static final String RESPONSE_STATUS_MESSAGE = "Response status does not match";
    private static final String RESPONSE_BODY_MESSAGE = "Response body does not match";

    @Mock
    private ApprovalService approvalService;

    @InjectMocks
    private ApprovalController approvalController;

    @Test
    void getAllApprovalsReturns200() {
        List<ApprovalStatus> approvals = List.of(createApprovalStatus());
        when(approvalService.getAllApprovalStatuses()).thenReturn(approvals);

        ResponseEntity<List<ApprovalStatus>> response = approvalController.getAllApprovals();

        assertThat(response.getStatusCode()).as(RESPONSE_STATUS_MESSAGE).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).as(RESPONSE_BODY_MESSAGE).isEqualTo(approvals);
    }

    @Test
    void createApprovalReturns201() {
        Approval approval = createApproval();
        when(approvalService.createApproval(approval)).thenReturn(approval);

        ResponseEntity<Approval> response = approvalController.createApproval(approval);

        assertThat(response.getStatusCode()).as(RESPONSE_STATUS_MESSAGE).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).as(RESPONSE_BODY_MESSAGE).isEqualTo(approval);
    }

    @Test
    void deleteApprovalReturns204() {
        ResponseEntity<Void> response = approvalController.deleteApproval(APPROVAL_ID.toString());

        assertThat(response.getStatusCode()).as(RESPONSE_STATUS_MESSAGE).isEqualTo(HttpStatus.NO_CONTENT);
        verify(approvalService).deleteApproval(APPROVAL_ID);
    }

    @Test
    void deleteApprovalPropagatesNotFoundException() {
        doThrow(new NotFoundException("Approval not found")).when(approvalService).deleteApproval(APPROVAL_ID);

        assertThrows(
            NotFoundException.class,
            () -> approvalController.deleteApproval(APPROVAL_ID.toString())
        );
    }

    @Test
    void deleteApprovalThrowsIllegalArgumentExceptionForInvalidUuid() {
        assertThrows(
            IllegalArgumentException.class,
            () -> approvalController.deleteApproval(INVALID_UUID)
        );
    }

    private Approval createApproval() {
        return Approval.builder()
            .id(APPROVAL_ID)
            .subjectId(SUBJECT_ID)
            .subjectType(SubjectType.COURT)
            .userId(USER_ID)
            .build();
    }

    private ApprovalStatus createApprovalStatus() {
        return new ApprovalStatus(
            SUBJECT_ID,
            SubjectType.COURT,
            "Test Court",
            true,
            APPROVAL_ID,
            USER_ID,
            User.builder().id(USER_ID).email("approver@justice.gov.uk").build(),
            null
        );
    }
}
