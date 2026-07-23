package uk.gov.hmcts.reform.fact.data.api.repositories;

import uk.gov.hmcts.reform.fact.data.api.entities.Lock;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import uk.gov.hmcts.reform.fact.data.api.entities.types.SubjectType;
import uk.gov.hmcts.reform.fact.data.api.entities.types.Page;

@Repository
public interface LockRepository extends JpaRepository<Lock, UUID> {

    @EntityGraph(attributePaths = {"user"})
    List<Lock> findAllBySubjectTypeAndSubjectId(SubjectType subjectType, UUID subjectId);

    @EntityGraph(attributePaths = {"user"})
    Optional<Lock> findBySubjectTypeAndSubjectIdAndPage(SubjectType subjectType, UUID subjectId, Page page);

    @Modifying
    @Query(value = """
        DELETE FROM
            "lock"
        WHERE
            subject_type = :subjectType
        AND
            subject_id = :subjectId
        AND
            page = :page
        """, nativeQuery = true)
    void deleteBySubjectTypeAndSubjectIdAndPage(
        @Param("subjectType") String subjectType,
        @Param("subjectId") UUID subjectId,
        @Param("page") String page
    );

    @Modifying
    @Query(value = """
        DELETE FROM
            "lock"
        WHERE
            lock_acquired < :lockAcquired
        """, nativeQuery = true)
    void deleteByLockAcquiredBefore(
        @Param("lockAcquired") ZonedDateTime lockAcquired
    );

    @Modifying
    @Query(value = """
        DELETE FROM
            "lock"
        WHERE
            user_id = :userId
        """, nativeQuery = true)
    void deleteAllByUserId(
        @Param("userId") UUID userId
    );

    @Query(value = """
        INSERT INTO "lock" AS l (id, subject_id, subject_type, user_id, page, lock_acquired)
        VALUES (:newLockId, :subjectId, :subjectType, :userId, :page, :lockAcquired)
        ON CONFLICT (subject_type, subject_id, page)
        DO UPDATE SET
            user_id = :userId,
            lock_acquired = :lockAcquired,
            id = :newLockId
        WHERE l.user_id = :userId OR l.lock_acquired < :expiryThreshold
        RETURNING id
        """, nativeQuery = true)
    Optional<UUID> tryAcquireLock(
        @Param("newLockId") UUID newLockId,
        @Param("subjectType") String subjectType,
        @Param("subjectId") UUID subjectId,
        @Param("page") String page,
        @Param("userId") UUID userId,
        @Param("lockAcquired") ZonedDateTime lockAcquired,
        @Param("expiryThreshold") ZonedDateTime expiryThreshold
    );
}
