package uk.gov.hmcts.reform.fact.data.api.services;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.fact.data.api.entities.Court;
import uk.gov.hmcts.reform.fact.data.api.entities.CourtLock;
import uk.gov.hmcts.reform.fact.data.api.repositories.CourtLockRepository;

import java.util.UUID;

import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class CourtLockServiceTest {

    @Mock
    private CourtLockRepository courtLockRepository;

    @InjectMocks
    private CourtLockService courtLockService;

    private UUID courtId;
    private Court court;
    private CourtLock lock;
    private UUID userId;

    @BeforeEach
    void setup() {
        courtId = UUID.randomUUID();
        userId = UUID.randomUUID();
        court = new Court();
        court.setId(courtId);
        lock = new CourtLock();
        lock.setCourtId(courtId);
        lock.setUserId(userId);
    }

    @Test
    void shouldDeleteAllLocksForUser() {
        courtLockService.deleteLocksByUserId(userId);
        verify(courtLockRepository).deleteAllByUserId(userId);
    }

    @Test
    void shouldHandleNoLocksForUser() {
        UUID newUserId = UUID.randomUUID();
        courtLockService.deleteLocksByUserId(newUserId);
        verify(courtLockRepository).deleteAllByUserId(newUserId);
    }
}

