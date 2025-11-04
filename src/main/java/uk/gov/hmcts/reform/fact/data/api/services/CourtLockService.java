package uk.gov.hmcts.reform.fact.data.api.services;

import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.fact.data.api.repositories.CourtLockRepository;

import java.util.UUID;

/**
 * Service class responsible for managing court lock operations.
 * Provides functionality to handle court locking mechanisms and user-specific locks.
 */
@Service
public class CourtLockService {

    private final CourtLockRepository courtLockRepository;

    /**
     * Constructs a new CourtLockService with required dependencies.
     *
     * @param courtLockRepository Repository for court lock operations
     */
    public CourtLockService(CourtLockRepository courtLockRepository) {
        this.courtLockRepository = courtLockRepository;
    }

    /**
     * Deletes all court locks associated with a specific user ID.
     *
     * @param userId The unique identifier of the user whose locks should be deleted
     */
    public void deleteLocksByUserId(UUID userId) {
        courtLockRepository.deleteAllByUserId(userId);
    }
}
