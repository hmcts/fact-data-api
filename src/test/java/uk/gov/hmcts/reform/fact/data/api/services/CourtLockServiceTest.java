package uk.gov.hmcts.reform.fact.data.api.services;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.fact.data.api.entities.User;
import uk.gov.hmcts.reform.fact.data.api.errorhandling.exceptions.NotFoundException;
import uk.gov.hmcts.reform.fact.data.api.repositories.CourtLockRepository;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CourtLockServiceTest {

    @Mock
    private CourtLockRepository courtLockRepository;

    @Mock
    private UserService userService;

    @InjectMocks
    private CourtLockService courtLockService;

    private UUID userId;
    private User user;

    @BeforeEach
    void setup() {
        userId = UUID.randomUUID();
        user = new User();
        user.setId(userId);
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
}

