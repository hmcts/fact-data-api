package uk.gov.hmcts.reform.fact.data.api.services;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.fact.data.api.entities.User;
import uk.gov.hmcts.reform.fact.data.api.errorhandling.exceptions.NotFoundException;
import uk.gov.hmcts.reform.fact.data.api.entities.Court;
import uk.gov.hmcts.reform.fact.data.api.entities.CourtLock;
import uk.gov.hmcts.reform.fact.data.api.entities.types.Page;
import uk.gov.hmcts.reform.fact.data.api.repositories.CourtLockRepository;

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
class CourtLockServiceTest {

    @Mock
    private CourtLockRepository courtLockRepository;

    @Mock
    private CourtService courtService;

    @Mock
    private UserService userService;

    @InjectMocks
    private CourtLockService courtLockService;

    private UUID userId;
    private User user;
    private UUID courtId;
    private Court court;
    private CourtLock lock;

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

        lock = new CourtLock();
        lock.setCourtId(courtId);
        lock.setPage(Page.COURT);
        lock.setLockAcquired(ZonedDateTime.now());
    }

    @Test
    void clearUserLocksShouldDeleteAllLocksForUser() {
        when(userService.getUserById(userId)).thenReturn(user);
        courtLockService.clearUserLocks(userId);

        verify(courtLockRepository).deleteAllByUserId(userId);
    }

    @Test
    void clearUserLocksShouldThrowNotFoundExceptionWhenUserDoesNotExist() {
        when(userService.getUserById(userId)).thenThrow(new NotFoundException("User not found"));

        assertThrows(NotFoundException.class, () -> courtLockService.clearUserLocks(userId));
    }

    @Test
    void deleteLocksByUserIdShouldHandleNoLocksForUser() {
        UUID newUserId = UUID.randomUUID();
        User newUser = new User();
        newUser.setId(newUserId);
        when(userService.getUserById(newUserId)).thenReturn(newUser);
        courtLockService.clearUserLocks(newUserId);
        verify(courtLockRepository).deleteAllByUserId(newUserId);
    }

    @Test
    void getLocksByCourtIdShouldGetLocksByCourtId() {
        when(courtService.getCourtById(courtId)).thenReturn(court);
        when(courtLockRepository.findAllByCourtId(courtId)).thenReturn(List.of(lock));
        List<CourtLock> result = courtLockService.getLocksByCourtId(courtId);
        assertEquals(1, result.size());
        verify(courtService).getCourtById(courtId);
        verify(courtLockRepository).findAllByCourtId(courtId);
    }

    @Test
    void getPageLockShouldGetPageLock() {
        Page page = Page.COURT;
        when(courtService.getCourtById(courtId)).thenReturn(court);
        when(courtLockRepository.findByCourtIdAndPage(courtId, page)).thenReturn(Optional.of(lock));
        Optional<CourtLock> result = courtLockService.getPageLock(courtId, page);
        assertTrue(result.isPresent());
        verify(courtService).getCourtById(courtId);
        verify(courtLockRepository).findByCourtIdAndPage(courtId, page);
    }

    @Test
    void createLockShouldCreateLock() {
        when(courtService.getCourtById(courtId)).thenReturn(court);
        when(userService.getUserById(userId)).thenReturn(user);
        when(courtLockRepository.save(any(CourtLock.class))).thenReturn(lock);
        CourtLock result = courtLockService.createOrUpdateLock(courtId, Page.COURT, userId);
        assertNotNull(result);
        verify(courtLockRepository).save(any(CourtLock.class));
    }

    @Test
    void createOrUpdateLockShouldUpdateExistingLock() {
        Page page = Page.COURT;
        when(courtService.getCourtById(courtId)).thenReturn(court);
        when(userService.getUserById(userId)).thenReturn(user);
        when(courtLockRepository.findByCourtIdAndPage(courtId, page)).thenReturn(Optional.of(lock));
        when(courtLockRepository.save(any(CourtLock.class))).thenReturn(lock);
        CourtLock result = courtLockService.createOrUpdateLock(courtId, page, userId);
        assertNotNull(result);
        verify(courtService).getCourtById(courtId);
        verify(courtLockRepository).findByCourtIdAndPage(courtId, page);
        verify(courtLockRepository).save(any(CourtLock.class));
    }

    @Test
    void deleteLockShouldDeleteLock() {
        Page page = Page.COURT;
        when(courtService.getCourtById(courtId)).thenReturn(court);
        courtLockService.deleteLock(courtId, page);
        verify(courtService).getCourtById(courtId);
        verify(courtLockRepository).deleteByCourtIdAndPage(courtId, page);
    }
}

