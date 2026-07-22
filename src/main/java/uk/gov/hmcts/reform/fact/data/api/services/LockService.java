package uk.gov.hmcts.reform.fact.data.api.services;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import org.springframework.web.server.ResponseStatusException;
import uk.gov.hmcts.reform.fact.data.api.entities.types.SubjectType;
import uk.gov.hmcts.reform.fact.data.api.entities.types.Page;
import uk.gov.hmcts.reform.fact.data.api.repositories.LockRepository;
import uk.gov.hmcts.reform.fact.data.api.entities.Lock;
import uk.gov.hmcts.reform.fact.data.api.entities.User;

import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Service class responsible for managing court lock operations.
 * Provides functionality to handle court locking mechanisms and user-specific locks.
 */
@Slf4j
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
        UUID id = verifySubject(subjectType, subjectId);

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
        UUID id = verifySubject(subjectType, subjectId);

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
        UUID id = verifySubject(subjectType, subjectId);

        log.info("Attempting to acquire lock for subjectType: {}, subjectId: {}, page: {}, userId: {}",
            subjectType, subjectId, page, userId);

        User user = userService.getUserById(userId);
        ZonedDateTime lockAcquired = ZonedDateTime.now(ZoneOffset.UTC);
        ZonedDateTime expiryThreshold = lockAcquired.minusMinutes(lockTimeoutMinutes);

        UUID lockId = lockRepository.tryAcquireLock(
            UUID.randomUUID(),
            subjectType.name(),
            id,
            page.name(),
            user.getId(),
            lockAcquired,
            expiryThreshold
        ).orElseThrow(() -> new ResponseStatusException(
            HttpStatus.CONFLICT, "Page locked by another user"));

        log.info("Lock acquired for subjectType: {}, subjectId: {}, page: {}, userId: {}, lockId: {}",
            subjectType, subjectId, page, userId, lockId);

        return Lock.builder()
            .id(lockId)
            .subjectId(id)
            .subjectType(subjectType)
            .userId(user.getId())
            .user(user)
            .page(page)
            .lockAcquired(lockAcquired)
            .build();
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
        UUID id = verifySubject(subjectType, subjectId);

        lockRepository.deleteBySubjectTypeAndSubjectIdAndPage(subjectType.name(), id, page.name());
    }

    /**
     * Deletes all expired locks.
     */
    @Transactional
    public void deleteExpiredLocks() {
        lockRepository.deleteByLockAcquiredBefore(ZonedDateTime.now(ZoneOffset.UTC).minusMinutes(lockTimeoutMinutes));
    }

    /**
     * Verifies the subject type and id, returning the corresponding subject id.
     *
     * @param subjectType the {@link SubjectType}
     * @param subjectId the id
     * @return the subject id as a UUID
     *
     * @throws uk.gov.hmcts.reform.fact.data.api.errorhandling.exceptions.NotFoundException if the entity does not exist
     */
    private UUID verifySubject(final SubjectType subjectType, final UUID subjectId) {
        return subjectType == SubjectType.SERVICE_CENTRE
            ? serviceCentreService.getServiceCentreById(subjectId).getId()
            : courtService.getCourtById(subjectId).getId();
    }
}
