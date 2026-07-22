package uk.gov.hmcts.reform.fact.data.api.services;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import uk.gov.hmcts.reform.fact.data.api.entities.ServiceCentre;
import uk.gov.hmcts.reform.fact.data.api.entities.User;
import uk.gov.hmcts.reform.fact.data.api.entities.types.SubjectType;
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
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LockServiceTest {

    @Mock
    private LockRepository lockRepository;

    @Mock
    private CourtService courtService;

    @Mock
    private ServiceCentreService serviceCentreService;

    @Mock
    private UserService userService;

    @InjectMocks
    private LockService lockService;

    private UUID userId;
    private User user;
    private UUID courtId;
    private Court court;
    private Lock lock;
    private UUID serviceCentreId;
    private ServiceCentre serviceCentre;

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
        lock.setId(UUID.randomUUID());
        lock.setSubjectType(SubjectType.COURT);
        lock.setSubjectId(courtId);
        lock.setPage(Page.GENERAL);
        lock.setLockAcquired(ZonedDateTime.now());

        serviceCentreId = UUID.randomUUID();
        serviceCentre = new ServiceCentre();
        serviceCentre.setId(serviceCentreId);
        serviceCentre.setName("Test Service Centre");

        // needed for deleteExpiredLocks test
        ReflectionTestUtils.setField(lockService, "lockTimeoutMinutes", 30L);
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
        when(lockRepository.findAllBySubjectTypeAndSubjectId(SubjectType.COURT, courtId))
            .thenReturn(List.of(lock));
        List<Lock> result = lockService.getAllSubjectLocks(SubjectType.COURT, courtId);
        assertEquals(1, result.size());
        verify(courtService).getCourtById(courtId);
        verify(lockRepository).findAllBySubjectTypeAndSubjectId(SubjectType.COURT, courtId);
    }

    @Test
    void getPageLockShouldGetPageLock() {
        Page page = Page.GENERAL;
        when(courtService.getCourtById(courtId)).thenReturn(court);
        when(lockRepository.findBySubjectTypeAndSubjectIdAndPage(SubjectType.COURT, courtId, page))
            .thenReturn(Optional.of(lock));
        Optional<Lock> result = lockService.getPageLock(SubjectType.COURT, courtId, page);
        assertTrue(result.isPresent());
        verify(courtService).getCourtById(courtId);
        verify(lockRepository).findBySubjectTypeAndSubjectIdAndPage(SubjectType.COURT, courtId, page);
    }

    @Test
    void createLockShouldCreateLock() {
        UUID lockId = UUID.randomUUID();
        when(courtService.getCourtById(courtId)).thenReturn(court);
        when(userService.getUserById(userId)).thenReturn(user);
        when(lockRepository.tryAcquireLock(
            any(),
            eq(SubjectType.COURT.name()),
            eq(courtId),
            eq(Page.GENERAL.name()),
            eq(userId),
            any(ZonedDateTime.class),
            any(ZonedDateTime.class)
        )).thenReturn(Optional.of(lockId));

        Lock result = lockService.createOrUpdateLock(SubjectType.COURT, courtId, Page.GENERAL, userId);

        assertNotNull(result);
        assertEquals(lockId, result.getId());
        verify(lockRepository).tryAcquireLock(
            any(),
            eq(SubjectType.COURT.name()),
            eq(courtId),
            eq(Page.GENERAL.name()),
            eq(userId),
            any(ZonedDateTime.class),
            any(ZonedDateTime.class)
        );
    }

    @Test
    void createOrUpdateLockShouldUpdateExistingLock() {
        Page page = Page.GENERAL;
        UUID lockId = UUID.randomUUID();
        when(courtService.getCourtById(courtId)).thenReturn(court);
        when(userService.getUserById(userId)).thenReturn(user);
        when(lockRepository.tryAcquireLock(
             any(),
             eq(SubjectType.COURT.name()),
             eq(courtId),
             eq(page.name()),
             eq(userId),
             any(ZonedDateTime.class),
             any(ZonedDateTime.class)
        )).thenReturn(Optional.of(lockId));

        Lock result = lockService.createOrUpdateLock(SubjectType.COURT, courtId, page, userId);

        assertNotNull(result);
        assertEquals(lockId, result.getId());
        verify(courtService).getCourtById(courtId);
        verify(lockRepository).tryAcquireLock(
             any(),
             eq(SubjectType.COURT.name()),
             eq(courtId),
             eq(page.name()),
             eq(userId),
             any(ZonedDateTime.class),
             any(ZonedDateTime.class)
        );
    }

    @Test
    void deleteLockShouldDeleteLock() {
        Page page = Page.GENERAL;
        when(courtService.getCourtById(courtId)).thenReturn(court);
        lockService.deleteLock(SubjectType.COURT, courtId, page);
        verify(courtService).getCourtById(courtId);
        verify(lockRepository).deleteBySubjectTypeAndSubjectIdAndPage(SubjectType.COURT.name(), courtId, page.name());
    }

    @Test
    void getLocksByServiceCentreIdShouldGetLocksByServiceCentreId() {
        when(serviceCentreService.getServiceCentreById(serviceCentreId)).thenReturn(serviceCentre);
        when(lockRepository.findAllBySubjectTypeAndSubjectId(SubjectType.SERVICE_CENTRE, serviceCentreId))
            .thenReturn(List.of(lock));

        List<Lock> result = lockService.getAllSubjectLocks(SubjectType.SERVICE_CENTRE, serviceCentreId);

        assertEquals(1, result.size());
        verify(serviceCentreService).getServiceCentreById(serviceCentreId);
        verify(lockRepository).findAllBySubjectTypeAndSubjectId(SubjectType.SERVICE_CENTRE, serviceCentreId);
    }

    @Test
    void getPageLockShouldGetPageLockForServiceCentre() {
        Page page = Page.GENERAL;
        when(serviceCentreService.getServiceCentreById(serviceCentreId)).thenReturn(serviceCentre);
        when(lockRepository.findBySubjectTypeAndSubjectIdAndPage(SubjectType.SERVICE_CENTRE, serviceCentreId, page))
            .thenReturn(Optional.of(lock));

        Optional<Lock> result = lockService.getPageLock(SubjectType.SERVICE_CENTRE, serviceCentreId, page);

        assertTrue(result.isPresent());
        verify(serviceCentreService).getServiceCentreById(serviceCentreId);
        verify(lockRepository).findBySubjectTypeAndSubjectIdAndPage(SubjectType.SERVICE_CENTRE, serviceCentreId, page);
    }

    @Test
    void createLockShouldCreateLockForServiceCentre() {
        UUID lockId = UUID.randomUUID();
        when(serviceCentreService.getServiceCentreById(serviceCentreId)).thenReturn(serviceCentre);
        when(userService.getUserById(userId)).thenReturn(user);
        when(lockRepository.tryAcquireLock(
            any(),
            eq(SubjectType.SERVICE_CENTRE.name()),
            eq(serviceCentreId),
            eq(Page.GENERAL.name()),
            eq(userId),
            any(ZonedDateTime.class),
            any(ZonedDateTime.class)
        )).thenReturn(Optional.of(lockId));

        Lock result = lockService.createOrUpdateLock(SubjectType.SERVICE_CENTRE, serviceCentreId, Page.GENERAL, userId);

        assertNotNull(result);
        assertEquals(lockId, result.getId());
        verify(serviceCentreService).getServiceCentreById(serviceCentreId);
        verify(lockRepository).tryAcquireLock(
            any(),
            eq(SubjectType.SERVICE_CENTRE.name()),
            eq(serviceCentreId),
            eq(Page.GENERAL.name()),
            eq(userId),
            any(ZonedDateTime.class),
            any(ZonedDateTime.class)
        );
    }

    @Test
    void deleteLockShouldDeleteLockForServiceCentre() {
        Page page = Page.GENERAL;
        when(serviceCentreService.getServiceCentreById(serviceCentreId)).thenReturn(serviceCentre);

        lockService.deleteLock(SubjectType.SERVICE_CENTRE, serviceCentreId, page);

        verify(serviceCentreService).getServiceCentreById(serviceCentreId);
        verify(lockRepository).deleteBySubjectTypeAndSubjectIdAndPage(
            SubjectType.SERVICE_CENTRE.name(), serviceCentreId, page.name());
    }

    @Test
    void deleteExpiredLocksShouldDeleteLocksBeforeConfiguredTimeout() {
        lockService.deleteExpiredLocks();

        verify(lockRepository).deleteByLockAcquiredBefore(any(ZonedDateTime.class));
    }
}
