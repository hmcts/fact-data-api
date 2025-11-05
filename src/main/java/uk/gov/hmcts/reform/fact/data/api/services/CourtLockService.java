package uk.gov.hmcts.reform.fact.data.api.services;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.hmcts.reform.fact.data.api.repositories.CourtLockRepository;

import java.util.UUID;

/**
 * Service class responsible for managing court lock operations.
 * Provides functionality to handle court locking mechanisms and user-specific locks.
 */
@Service
public class CourtLockService {

    private final CourtLockRepository courtLockRepository;
    private final UserService userService;

    /**
     * Constructs a new CourtLockService with required dependencies.
     *
     * @param courtLockRepository Repository for court lock operations
     * @param userService Service for user operations
     */
    public CourtLockService(CourtLockRepository courtLockRepository, UserService userService) {
        this.courtLockRepository = courtLockRepository;
        this.userService = userService;
    }

    /**
     * Delete all court locks for a given user id.
     *
     * @param userId The user id to clear locks for.
     */
    @Transactional
    public void clearUserLocks(UUID userId) {
        courtLockRepository.deleteAllByUserId(userService.getUserById(userId).getId());
    }
}
