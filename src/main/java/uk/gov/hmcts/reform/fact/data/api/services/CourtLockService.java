package uk.gov.hmcts.reform.fact.data.api.services;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.hmcts.reform.fact.data.api.entities.types.Page;
import uk.gov.hmcts.reform.fact.data.api.repositories.CourtLockRepository;
import uk.gov.hmcts.reform.fact.data.api.entities.CourtLock;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Service class responsible for managing court lock operations.
 * Provides functionality to handle court locking mechanisms and user-specific locks.
 */
@Service
public class CourtLockService {

    private final CourtLockRepository courtLockRepository;
    private final UserService userService;
    private final CourtService courtService;

    /**
     * Constructs a new CourtLockService with required dependencies.
     *
     * @param courtLockRepository Repository for court lock operations
     * @param courtService Service for court operations
     * @param userService Service for user operations
     */
    public CourtLockService(CourtLockRepository courtLockRepository,
                            CourtService courtService,
                            UserService userService) {
        this.courtLockRepository = courtLockRepository;
        this.courtService = courtService;
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

    /**
     * Gets all active locks for a specific court.
     *
     * @param courtId The court's unique identifier
     * @return List of active court locks with court ID matching the provided ID
     */
    public List<CourtLock> getLocksByCourtId(UUID courtId) {
        return courtLockRepository.findAllByCourtId(courtService.getCourtById(courtId).getId());
    }

    /**
     * Checks if a specific page is locked for a court.
     *
     * @param courtId The court's unique identifier
     * @param page    The page to check
     * @return Optional containing the lock if it exists
     */
    public Optional<CourtLock> getPageLock(UUID courtId, Page page) {
        return courtLockRepository.findByCourtIdAndPage(courtService.getCourtById(courtId).getId(), page);
    }

    /**
     * Creates a new court lock.
     *
     * @param courtId The court's unique identifier
     * @param page    The page to lock
     * @param userId  The user's unique identifier
     * @return The created court lock
     */
    public CourtLock createLock(UUID courtId, Page page, UUID userId) {
        CourtLock lock = new CourtLock();
        lock.setCourtId(courtService.getCourtById(courtId).getId());
        lock.setPage(page);
        lock.setUserId(userService.getUserById(userId).getId());
        lock.setLockAcquired(ZonedDateTime.now());
        return courtLockRepository.save(lock);
    }

    /**
     * Updates an existing court lock.
     *
     * @param courtId The court's unique identifier
     * @param page    The page to update
     * @param userId  The user's unique identifier
     * @return The updated court lock
     */
    public CourtLock updateLock(UUID courtId, Page page, UUID userId) {
        Optional<CourtLock> existingLock =
            courtLockRepository.findByCourtIdAndPage(courtService.getCourtById(courtId).getId(), page);

        if (existingLock.isPresent()) {
            CourtLock lock = existingLock.get();
            lock.setUserId(userId);
            lock.setLockAcquired(ZonedDateTime.now());
            return courtLockRepository.save(lock);
        }

        return createLock(courtId, page, userId);
    }

    /**
     * Deletes a specific court lock.
     *
     * @param courtId The court's unique identifier
     * @param page    The page to unlock
     */
    public void deleteLock(UUID courtId, Page page) {
        courtLockRepository.deleteByCourtIdAndPage(courtService.getCourtById(courtId).getId(), page);
    }
}
