package uk.gov.hmcts.reform.fact.data.api.controllers;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import uk.gov.hmcts.reform.fact.data.api.entities.CourtLock;
import uk.gov.hmcts.reform.fact.data.api.entities.types.Page;
import uk.gov.hmcts.reform.fact.data.api.services.CourtLockService;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CourtLockControllerTest {

    private static final UUID COURT_ID = UUID.randomUUID();
    private static final UUID USER_ID = UUID.randomUUID();
    private static final String INVALID_UUID = "abcde";
    private static final Page TEST_PAGE = Page.COURT;

    private static final String RESPONSE_STATUS_MESSAGE = "Response status does not match";
    private static final String RESPONSE_BODY_MESSAGE = "Response body does not match";

    @Mock
    private CourtLockService courtLockService;

    @InjectMocks
    private CourtLockController courtLockController;

    @Test
    void getCourtLocksReturns200() {
        List<CourtLock> locks = List.of(new CourtLock());
        when(courtLockService.getLocksByCourtId(COURT_ID)).thenReturn(locks);

        ResponseEntity<List<CourtLock>> response = courtLockController.getCourtLocks(COURT_ID.toString());

        assertThat(response.getStatusCode()).as(RESPONSE_STATUS_MESSAGE).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).as(RESPONSE_BODY_MESSAGE).isEqualTo(locks);
    }

    @Test
    void getCourtLocksThrowsIllegalArgumentExceptionForInvalidUUID() {
        assertThrows(
            IllegalArgumentException.class, () ->
                courtLockController.getCourtLocks(INVALID_UUID)
        );
    }

    @Test
    void getCourtLockStatusReturns200() {
        Optional<CourtLock> lock = Optional.of(new CourtLock());
        when(courtLockService.getPageLock(COURT_ID, TEST_PAGE)).thenReturn(lock);

        ResponseEntity<Optional<CourtLock>> response = courtLockController.getCourtLockStatus(
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
                courtLockController.getCourtLockStatus(INVALID_UUID, TEST_PAGE)
        );
    }

    @Test
    void createCourtLockReturns201() {
        CourtLock lock = new CourtLock();
        when(courtLockService.createLock(COURT_ID, TEST_PAGE, USER_ID)).thenReturn(lock);

        ResponseEntity<CourtLock> response = courtLockController.createCourtLock(
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
                courtLockController.createCourtLock(INVALID_UUID, TEST_PAGE, USER_ID)
        );
    }

    @Test
    void updateCourtLockReturns200() {
        CourtLock lock = new CourtLock();
        when(courtLockService.updateLock(COURT_ID, TEST_PAGE, USER_ID)).thenReturn(lock);

        ResponseEntity<CourtLock> response = courtLockController.updateCourtLock(
            COURT_ID.toString(),
            TEST_PAGE,
            USER_ID
        );

        assertThat(response.getStatusCode()).as(RESPONSE_STATUS_MESSAGE).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).as(RESPONSE_BODY_MESSAGE).isEqualTo(lock);
    }

    @Test
    void updateCourtLockThrowsIllegalArgumentExceptionForInvalidUUID() {
        assertThrows(
            IllegalArgumentException.class, () ->
                courtLockController.updateCourtLock(INVALID_UUID, TEST_PAGE, USER_ID)
        );
    }

    @Test
    void deleteCourtLockReturns204() {
        ResponseEntity<Void> response = courtLockController.deleteCourtLock(COURT_ID.toString(), TEST_PAGE);

        assertThat(response.getStatusCode()).as(RESPONSE_STATUS_MESSAGE).isEqualTo(HttpStatus.NO_CONTENT);
    }

    @Test
    void deleteCourtLockThrowsIllegalArgumentExceptionForInvalidUUID() {
        assertThrows(
            IllegalArgumentException.class, () ->
                courtLockController.deleteCourtLock(INVALID_UUID, TEST_PAGE)
        );
    }
}
