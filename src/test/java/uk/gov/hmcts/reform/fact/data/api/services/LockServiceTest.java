package uk.gov.hmcts.reform.fact.data.api.services;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.fact.data.api.entities.User;
import uk.gov.hmcts.reform.fact.data.api.entities.types.AuditSubjectType;
import uk.gov.hmcts.reform.fact.data.api.errorhandling.exceptions.NotFoundException;
import uk.gov.hmcts.reform.fact.data.api.entities.Court;
import uk.gov.hmcts.reform.fact.data.api.entities.Lock;
import uk.gov.hmcts.reform.fact.data.api.entities.types.Page;
import uk.gov.hmcts.reform.fact.data.api.repositories.LockRepository;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LockServiceTest {

    @Mock
    private LockRepository lockRepository;

    @Mock
    private CourtService courtService;

    @Mock
    private UserService userService;

    @InjectMocks
    private LockService lockService;

    private UUID userId;
    private User user;
    private UUID courtId;
    private Court court;
    private Lock lock;

    @BeforeEach
    void setup() {
        courtId = UUID.randomUUID();
        court = new Court();
        court.setId(courtId);
        court.setName("Test Court");

        userId = UUID.randomUUID();
        user = new User();
        user.setId(userId);
        user.setEmail("email@justice.gov.uk");

        lock = new Lock();
        lock.setSubjectType(AuditSubjectType.COURT);
        lock.setSubjectId(courtId);
        lock.setPage(Page.GENERAL);
        lock.setLockAcquired(ZonedDateTime.now());
    }

    @Test
    void clearUserLocksShouldDeleteAllLocksForUser() {
        when(userService.getUserById(userId)).thenReturn(user);
        lockService.clearUserLocks(userId);

        verify(lockRepository).deleteAllByUserId(userId);
    }

    @Test
    void clearUserLocksShouldThrowNotFoundExceptionWhenUserDoesNotExist() {
        when(userService.getUserById(userId)).thenThrow(new NotFoundException("User not found"));

        assertThrows(NotFoundException.class, () -> lockService.clearUserLocks(userId));
    }

    @Test
    void deleteLocksByUserIdShouldHandleNoLocksForUser() {
        UUID newUserId = UUID.randomUUID();
        User newUser = new User();
        newUser.setId(newUserId);
        when(userService.getUserById(newUserId)).thenReturn(newUser);
        lockService.clearUserLocks(newUserId);
        verify(lockRepository).deleteAllByUserId(newUserId);
    }

    @Test
    void getLocksByCourtIdShouldGetLocksByCourtId() {
        when(courtService.getCourtById(courtId)).thenReturn(court);
        when(lockRepository.findAllBySubjectTypeAndSubjectId(AuditSubjectType.COURT, courtId))
            .thenReturn(List.of(lock));
        List<Lock> result = lockService.getAllSubjectLocks(AuditSubjectType.COURT, courtId);
        assertEquals(1, result.size());
        verify(courtService).getCourtById(courtId);
        verify(lockRepository).findAllBySubjectTypeAndSubjectId(AuditSubjectType.COURT, courtId);
    }

    @Test
    void getPageLockShouldGetPageLock() {
        Page page = Page.GENERAL;
        when(courtService.getCourtById(courtId)).thenReturn(court);
        when(lockRepository.findBySubjectTypeAndSubjectIdAndPage(AuditSubjectType.COURT, courtId, page))
            .thenReturn(Optional.of(lock));
        Optional<Lock> result = lockService.getPageLock(AuditSubjectType.COURT, courtId, page);
        assertTrue(result.isPresent());
        verify(courtService).getCourtById(courtId);
        verify(lockRepository).findBySubjectTypeAndSubjectIdAndPage(AuditSubjectType.COURT, courtId, page);
    }

    @Test
    void createLockShouldCreateLock() {
        when(courtService.getCourtById(courtId)).thenReturn(court);
        when(userService.getUserById(userId)).thenReturn(user);
        when(lockRepository.save(any(Lock.class))).thenReturn(lock);
        Lock result = lockService.createOrUpdateLock(AuditSubjectType.COURT, courtId, Page.GENERAL, userId);
        assertNotNull(result);
        verify(lockRepository).save(any(Lock.class));
    }

    @Test
    void createOrUpdateLockShouldUpdateExistingLock() {
        Page page = Page.GENERAL;
        when(courtService.getCourtById(courtId)).thenReturn(court);
        when(userService.getUserById(userId)).thenReturn(user);
        when(lockRepository.findBySubjectTypeAndSubjectIdAndPage(AuditSubjectType.COURT, courtId, page))
            .thenReturn(Optional.of(lock));
        when(lockRepository.save(any(Lock.class))).thenReturn(lock);
        Lock result = lockService.createOrUpdateLock(AuditSubjectType.COURT, courtId, page, userId);
        assertNotNull(result);
        verify(courtService).getCourtById(courtId);
        verify(lockRepository).findBySubjectTypeAndSubjectIdAndPage(AuditSubjectType.COURT, courtId, page);
        verify(lockRepository).save(any(Lock.class));
    }

    @Test
    void deleteLockShouldDeleteLock() {
        Page page = Page.GENERAL;
        when(courtService.getCourtById(courtId)).thenReturn(court);
        lockService.deleteLock(AuditSubjectType.COURT, courtId, page);
        verify(courtService).getCourtById(courtId);
        verify(lockRepository).deleteBySubjectTypeAndSubjectIdAndPage(AuditSubjectType.COURT, courtId, page);
    }
}

