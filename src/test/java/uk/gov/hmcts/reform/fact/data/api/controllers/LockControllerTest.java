package uk.gov.hmcts.reform.fact.data.api.controllers;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import uk.gov.hmcts.reform.fact.data.api.entities.Lock;
import uk.gov.hmcts.reform.fact.data.api.entities.types.SubjectType;
import uk.gov.hmcts.reform.fact.data.api.entities.types.Page;
import uk.gov.hmcts.reform.fact.data.api.services.LockService;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LockControllerTest {

    private static final UUID COURT_ID = UUID.randomUUID();
    private static final UUID USER_ID = UUID.randomUUID();
    private static final String INVALID_UUID = "abcde";
    private static final Page TEST_PAGE = Page.GENERAL;

    private static final String RESPONSE_STATUS_MESSAGE = "Response status does not match";
    private static final String RESPONSE_BODY_MESSAGE = "Response body does not match";

    @Mock
    private LockService lockService;

    @InjectMocks
    private LockController lockController;

    @Test
    void getCourtLocksReturns200() {
        List<Lock> locks = List.of(new Lock());
        when(lockService.getAllSubjectLocks(SubjectType.COURT, COURT_ID)).thenReturn(locks);

        ResponseEntity<List<Lock>> response =
            lockController.getSubjectLocks(SubjectType.COURT, COURT_ID.toString());

        assertThat(response.getStatusCode()).as(RESPONSE_STATUS_MESSAGE).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).as(RESPONSE_BODY_MESSAGE).isEqualTo(locks);
    }

    @Test
    void getCourtLocksThrowsIllegalArgumentExceptionForInvalidUUID() {
        assertThrows(
            IllegalArgumentException.class, () ->
                lockController.getSubjectLocks(SubjectType.COURT, INVALID_UUID)
        );
    }

    @Test
    void getCourtLockStatusReturns200() {
        Lock lock = new Lock();
        when(lockService.getPageLock(SubjectType.COURT, COURT_ID, TEST_PAGE)).thenReturn(Optional.of(lock));

        ResponseEntity<Lock> response = lockController.getSubjectLockStatus(
            SubjectType.COURT,
            COURT_ID.toString(),
            TEST_PAGE
        );

        assertThat(response.getStatusCode()).as(RESPONSE_STATUS_MESSAGE).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).as(RESPONSE_BODY_MESSAGE).isEqualTo(lock);
    }

    @Test
    void getCourtLockStatusThrowsIllegalArgumentExceptionForInvalidUUID() {
        assertThrows(
            IllegalArgumentException.class, () ->
                lockController.getSubjectLockStatus(SubjectType.COURT, INVALID_UUID, TEST_PAGE)
        );
    }

    @Test
    void createOrUpdateCourtLockReturns201() {
        Lock lock = new Lock();
        when(lockService.createOrUpdateLock(SubjectType.COURT, COURT_ID, TEST_PAGE, USER_ID)).thenReturn(lock);

        ResponseEntity<Lock> response = lockController.createOrUpdateSubjectLock(
            SubjectType.COURT,
            COURT_ID.toString(),
            TEST_PAGE,
            USER_ID
        );

        assertThat(response.getStatusCode()).as(RESPONSE_STATUS_MESSAGE).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).as(RESPONSE_BODY_MESSAGE).isEqualTo(lock);
    }

    @Test
    void createCourtLockThrowsIllegalArgumentExceptionForInvalidUUID() {
        assertThrows(
            IllegalArgumentException.class, () ->
                lockController.createOrUpdateSubjectLock(SubjectType.COURT, INVALID_UUID, TEST_PAGE, USER_ID)
        );
    }

    @Test
    void deleteCourtLockReturns204() {
        ResponseEntity<Void> response =
            lockController.deleteSubjectLock(SubjectType.COURT, COURT_ID.toString(), TEST_PAGE);

        assertThat(response.getStatusCode()).as(RESPONSE_STATUS_MESSAGE).isEqualTo(HttpStatus.NO_CONTENT);
    }

    @Test
    void deleteCourtLockThrowsIllegalArgumentExceptionForInvalidUUID() {
        assertThrows(
            IllegalArgumentException.class, () ->
                lockController.deleteSubjectLock(SubjectType.COURT, INVALID_UUID, TEST_PAGE)
        );
    }
}
