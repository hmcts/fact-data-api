package uk.gov.hmcts.reform.fact.data.api.services;

import uk.gov.hmcts.reform.fact.data.api.dto.ApprovalStatus;
import uk.gov.hmcts.reform.fact.data.api.entities.Approval;
import uk.gov.hmcts.reform.fact.data.api.entities.Court;
import uk.gov.hmcts.reform.fact.data.api.entities.ServiceCentre;
import uk.gov.hmcts.reform.fact.data.api.entities.User;
import uk.gov.hmcts.reform.fact.data.api.entities.types.AuditSubjectType;
import uk.gov.hmcts.reform.fact.data.api.entities.types.NameAndId;
import uk.gov.hmcts.reform.fact.data.api.errorhandling.exceptions.NotFoundException;
import uk.gov.hmcts.reform.fact.data.api.repositories.ApprovalRepository;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ApprovalServiceTest {

    private static final UUID APPROVAL_ID = UUID.randomUUID();
    private static final UUID SUBJECT_ID = UUID.randomUUID();
    private static final UUID SERVICE_CENTRE_ID = UUID.randomUUID();
    private static final UUID USER_ID = UUID.randomUUID();
    private static final String USER_EMAIL = "approver@justice.gov.uk";
    private static final ZonedDateTime LAST_UPDATED_AT = ZonedDateTime.now();

    @Mock
    private ApprovalRepository approvalRepository;

    @Mock
    private CourtService courtService;

    @Mock
    private ServiceCentreService serviceCentreService;

    @Mock
    private UserService userService;

    @InjectMocks
    private ApprovalService approvalService;

    @Test
    void getAllApprovalStatusesReturnsCourtsAndServiceCentresMarkedWithApprovalState() {
        User user = User.builder()
            .id(USER_ID)
            .email(USER_EMAIL)
            .build();
        Approval approval = Approval.builder()
            .id(APPROVAL_ID)
            .subjectId(SUBJECT_ID)
            .subjectType(AuditSubjectType.COURT)
            .userId(USER_ID)
            .user(user)
            .lastUpdatedAt(LAST_UPDATED_AT)
            .build();
        when(approvalRepository.findAll()).thenReturn(List.of(approval));
        when(courtService.getAllCourtNameAndIds()).thenReturn(List.of(new NameAndId("Test Court", SUBJECT_ID)));
        when(serviceCentreService.getAllServiceCentreNameAndIds())
            .thenReturn(List.of(new NameAndId("Test Service Centre", SERVICE_CENTRE_ID)));

        List<ApprovalStatus> result = approvalService.getAllApprovalStatuses();

        assertThat(result).containsExactly(
            new ApprovalStatus(
                SUBJECT_ID,
                AuditSubjectType.COURT,
                "Test Court",
                true,
                APPROVAL_ID,
                USER_ID,
                user,
                LAST_UPDATED_AT
            ),
            new ApprovalStatus(
                SERVICE_CENTRE_ID,
                AuditSubjectType.SERVICE_CENTRE,
                "Test Service Centre",
                false,
                null,
                null,
                null,
                null
            )
        );
    }

    @Test
    void createApprovalValidatesCourtAndUserBeforeSaving() {
        Approval approval = createApproval(AuditSubjectType.COURT);
        approval.setId(APPROVAL_ID);
        when(userService.getUserById(USER_ID)).thenReturn(new User());
        when(courtService.getCourtById(SUBJECT_ID)).thenReturn(new Court());
        when(approvalRepository.findBySubjectIdAndSubjectType(SUBJECT_ID, AuditSubjectType.COURT))
            .thenReturn(Optional.empty());
        when(approvalRepository.save(any(Approval.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Approval result = approvalService.createApproval(approval);

        assertThat(result.getId()).isNull();
        verify(userService).getUserById(USER_ID);
        verify(courtService).getCourtById(SUBJECT_ID);
        verify(approvalRepository).save(approval);
    }

    @Test
    void createApprovalValidatesServiceCentreBeforeSaving() {
        when(userService.getUserById(USER_ID)).thenReturn(new User());
        when(serviceCentreService.getServiceCentreById(SUBJECT_ID)).thenReturn(new ServiceCentre());
        when(approvalRepository.findBySubjectIdAndSubjectType(SUBJECT_ID, AuditSubjectType.SERVICE_CENTRE))
            .thenReturn(Optional.empty());
        when(approvalRepository.save(any(Approval.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Approval approval = createApproval(AuditSubjectType.SERVICE_CENTRE);
        approvalService.createApproval(approval);

        verify(serviceCentreService).getServiceCentreById(SUBJECT_ID);
    }

    @Test
    void createApprovalReusesExistingApprovalIdForApprovedSubject() {
        Approval existingApproval = createApproval(AuditSubjectType.COURT);
        existingApproval.setId(APPROVAL_ID);
        Approval approval = createApproval(AuditSubjectType.COURT);
        when(userService.getUserById(USER_ID)).thenReturn(new User());
        when(courtService.getCourtById(SUBJECT_ID)).thenReturn(new Court());
        when(approvalRepository.findBySubjectIdAndSubjectType(SUBJECT_ID, AuditSubjectType.COURT))
            .thenReturn(Optional.of(existingApproval));
        when(approvalRepository.save(any(Approval.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Approval result = approvalService.createApproval(approval);

        assertThat(result.getId()).isEqualTo(APPROVAL_ID);
    }

    @Test
    void deleteApprovalDeletesExistingApproval() {
        when(approvalRepository.existsById(APPROVAL_ID)).thenReturn(true);

        approvalService.deleteApproval(APPROVAL_ID);

        verify(approvalRepository).deleteById(APPROVAL_ID);
    }

    @Test
    void deleteApprovalThrowsNotFoundExceptionWhenApprovalDoesNotExist() {
        when(approvalRepository.existsById(APPROVAL_ID)).thenReturn(false);

        NotFoundException exception = assertThrows(
            NotFoundException.class,
            () -> approvalService.deleteApproval(APPROVAL_ID)
        );

        assertThat(exception.getMessage()).isEqualTo("Approval not found, ID: " + APPROVAL_ID);
    }

    private Approval createApproval(AuditSubjectType subjectType) {
        return Approval.builder()
            .subjectId(SUBJECT_ID)
            .subjectType(subjectType)
            .userId(USER_ID)
            .build();
    }
}
