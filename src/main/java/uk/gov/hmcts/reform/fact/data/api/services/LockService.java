package uk.gov.hmcts.reform.fact.data.api.services;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import uk.gov.hmcts.reform.fact.data.api.entities.types.SubjectType;
import uk.gov.hmcts.reform.fact.data.api.entities.types.Page;
import uk.gov.hmcts.reform.fact.data.api.repositories.LockRepository;
import uk.gov.hmcts.reform.fact.data.api.entities.Lock;
import uk.gov.hmcts.reform.fact.data.api.entities.User;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Service class responsible for managing court lock operations.
 * Provides functionality to handle court locking mechanisms and user-specific locks.
 */
@Service
@RequiredArgsConstructor
public class LockService {

    @Value("${courtLock.timeout-minutes}")
    private long lockTimeoutMinutes;

    private final LockRepository lockRepository;
    private final UserService userService;
    private final CourtService courtService;
    private final ServiceCentreService serviceCentreService;

    /**
     * Delete all court locks for a given user id.
     *
     * @param userId The user id to clear locks for.
     */
    @Transactional
    public void clearUserLocks(UUID userId) {
        lockRepository.deleteAllByUserId(userService.getUserById(userId).getId());
    }

    /**
     * get all active locks for the given subject.
     *
     * @param subjectType the subject type to get locks for
     * @param subjectId   the id of the subject
     * @return the {@link List} of {@link Lock}s for the given subject
     */
    public List<Lock> getAllSubjectLocks(SubjectType subjectType, UUID subjectId) {
        UUID id = subjectType == SubjectType.SERVICE_CENTRE
            ? serviceCentreService.getServiceCentreById(subjectId).getId()
            : courtService.getCourtById(subjectId).getId();

        return lockRepository.findAllBySubjectTypeAndSubjectId(subjectType,id);
    }

    /**
     * Checks if a specific page is locked for a subject.
     *
     * @param subjectType the subject type to get locks for
     * @param subjectId   the id of the subject
     * @param page        The page to check
     * @return Optional containing the lock if it exists
     */
    public Optional<Lock> getPageLock(SubjectType subjectType, UUID subjectId, Page page) {
        UUID id = subjectType == SubjectType.SERVICE_CENTRE
            ? serviceCentreService.getServiceCentreById(subjectId).getId()
            : courtService.getCourtById(subjectId).getId();

        return lockRepository.findBySubjectTypeAndSubjectIdAndPage(subjectType, id, page);
    }

    /**
     * Updates an existing subject lock.
     *
     * @param subjectType the subject type to get locks for
     * @param subjectId   the id of the subject
     * @param page        The page to update
     * @param userId      The user's unique identifier
     * @return The updated court lock
     */
    @Transactional
    public Lock createOrUpdateLock(SubjectType subjectType, UUID subjectId, Page page, UUID userId) {
        UUID id = subjectType == SubjectType.SERVICE_CENTRE
            ? serviceCentreService.getServiceCentreById(subjectId).getId()
            : courtService.getCourtById(subjectId).getId();

        User user = userService.getUserById(userId);

        Lock lock = lockRepository.findBySubjectTypeAndSubjectIdAndPage(subjectType, id, page)
            .orElse(new Lock());

        lock.setSubjectId(id);
        lock.setSubjectType(subjectType);
        lock.setUserId(user.getId());
        lock.setUser(user);
        lock.setPage(page);
        lock.setLockAcquired(ZonedDateTime.now());

        Lock savedLock = lockRepository.save(lock);
        // remove any existing locks for other pages owned by this user
        lockRepository.deleteAllByUserIdAndIdIsNot(userId, savedLock.getId());
        return savedLock;
    }

    /**
     * Deletes a specific subject lock.
     *
     * @param subjectType the subject type to get locks for
     * @param subjectId   the id of the subject
     * @param page    The page to unlock
     */
    @Transactional
    public void deleteLock(SubjectType subjectType, UUID subjectId, Page page) {
        UUID id = subjectType == SubjectType.SERVICE_CENTRE
            ? serviceCentreService.getServiceCentreById(subjectId).getId()
            : courtService.getCourtById(subjectId).getId();

        lockRepository.deleteBySubjectTypeAndSubjectIdAndPage(subjectType, id, page);
    }

    /**
     * Deletes all expired locks.
     */
    @Transactional
    public void deleteExpiredLocks() {
        lockRepository.deleteByLockAcquiredBefore(ZonedDateTime.now().minusMinutes(lockTimeoutMinutes));
    }
}
