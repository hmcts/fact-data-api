package uk.gov.hmcts.reform.fact.data.api.repositories;

import uk.gov.hmcts.reform.fact.data.api.entities.Audit;
import uk.gov.hmcts.reform.fact.data.api.entities.types.SubjectType;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface AuditRepository extends JpaRepository<Audit, UUID> {

    // ----------------------------------------------------
    // Standard paged queries

    @EntityGraph(attributePaths = {"user"})
    Page<Audit> findByCreatedAtAfter(
        ZonedDateTime createdAt,
        Pageable pageable);

    @EntityGraph(attributePaths = {"user"})
    Page<Audit> findByCreatedAtBetween(
        ZonedDateTime createdAtAfter,
        ZonedDateTime createdAtBefore,
        Pageable pageable);

    @EntityGraph(attributePaths = {"user"})
    Page<Audit> findBySubjectTypeAndCreatedAtAfter(
        SubjectType subjectType,
        ZonedDateTime createdAtAfter,
        Pageable pageable);

    @EntityGraph(attributePaths = {"user"})
    Page<Audit> findBySubjectIdAndSubjectTypeAndCreatedAtAfter(
        UUID subjectId,
        SubjectType subjectType,
        ZonedDateTime createdAt,
        Pageable pageable);

    @EntityGraph(attributePaths = {"user"})
    Page<Audit> findBySubjectTypeAndCreatedAtBetween(
        SubjectType subjectType,
        ZonedDateTime createdAtAfter,
        ZonedDateTime createdAtBefore,
        Pageable pageable);

    @EntityGraph(attributePaths = {"user"})
    Page<Audit> findBySubjectIdAndSubjectTypeAndCreatedAtBetween(
        UUID subjectId,
        SubjectType subjectType,
        ZonedDateTime createdAtAfter,
        ZonedDateTime createdAtBefore,
        Pageable pageable);


    // ----------------------------------------------------
    // Complex paged queries

    String SELECT_WITH_USER_JOIN = """
        select a
        from Audit a
        join User u on a.userId = u.id
        """;

    String WHERE_EMAIL_LIKE_AND_CREATED_AT_AFTER = """
        where
            a.createdAt > :createdAtAfter
            and u.email like %:email%
        """;

    String WHERE_EMAIL_LIKE_AND_CREATED_AT_BETWEEN = """
        where
            a.createdAt between :createdAtAfter and :createdAtBefore
            and u.email like %:email%
        """;

    String AND_SUBJECT_EQUALS = " and a.subjectId = :subjectId and a.subjectType = :subjectType";
    String AND_SUBJECT_TYPE_EQUALS = " and a.subjectType = :subjectType";


    @EntityGraph(attributePaths = {"user"})
    @Query(value = SELECT_WITH_USER_JOIN + WHERE_EMAIL_LIKE_AND_CREATED_AT_AFTER)
    Page<Audit> findByCreatedAtAfterAndEmailAddressLike(
        ZonedDateTime createdAtAfter,
        String email,
        Pageable pageable);

    @EntityGraph(attributePaths = {"user"})
    @Query(value = SELECT_WITH_USER_JOIN + WHERE_EMAIL_LIKE_AND_CREATED_AT_BETWEEN)
    Page<Audit> findByCreatedAtBetweenAndEmailAddressLike(
        ZonedDateTime createdAtAfter,
        ZonedDateTime createdAtBefore,
        String email,
        Pageable pageable);

    @EntityGraph(attributePaths = {"user"})
    @Query(value = SELECT_WITH_USER_JOIN + WHERE_EMAIL_LIKE_AND_CREATED_AT_AFTER + AND_SUBJECT_EQUALS)
    Page<Audit> findBySubjectIdAndSubjectTypeAndCreatedAtAfterAndEmailAddressLike(
        UUID subjectId,
        SubjectType subjectType,
        ZonedDateTime createdAtAfter,
        String email,
        Pageable pageable);

    @EntityGraph(attributePaths = {"user"})
    @Query(value = SELECT_WITH_USER_JOIN + WHERE_EMAIL_LIKE_AND_CREATED_AT_BETWEEN + AND_SUBJECT_EQUALS)
    Page<Audit> findBySubjectIdAndSubjectTypeAndCreatedAtBetweenAndEmailAddressLike(
        UUID subjectId,
        SubjectType subjectType,
        ZonedDateTime createdAtAfter,
        ZonedDateTime createdAtBefore,
        String email,
        Pageable pageable);

    @EntityGraph(attributePaths = {"user"})
    @Query(value = SELECT_WITH_USER_JOIN + WHERE_EMAIL_LIKE_AND_CREATED_AT_AFTER + AND_SUBJECT_TYPE_EQUALS)
    Page<Audit> findBySubjectTypeAndCreatedAtAfterAndEmailAddressLike(
        SubjectType subjectType,
        ZonedDateTime createdAtAfter,
        String email,
        Pageable pageable);

    @EntityGraph(attributePaths = {"user"})
    @Query(value = SELECT_WITH_USER_JOIN + WHERE_EMAIL_LIKE_AND_CREATED_AT_BETWEEN + AND_SUBJECT_TYPE_EQUALS)
    Page<Audit> findBySubjectTypeAndCreatedAtBetweenAndEmailAddressLike(
        SubjectType subjectType,
        ZonedDateTime createdAtAfter,
        ZonedDateTime createdAtBefore,
        String email,
        Pageable pageable);

    @EntityGraph(attributePaths = {"user"})
    Optional<Audit> findWithUserById(UUID id);

    // ----------------------------------------------------
    // Housekeeping queries

    void deleteAllByCreatedAtBefore(ZonedDateTime createdAtBefore);

    /**
     * removes all audits where the subject id is in the given list.
     *
     * @param auditSubjectIds the list of audit subject ids to delete
     */
    void deleteBySubjectIdIn(List<UUID> auditSubjectIds);
}
