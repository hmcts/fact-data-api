package uk.gov.hmcts.reform.fact.data.api.repositories;

import uk.gov.hmcts.reform.fact.data.api.entities.Audit;

import java.time.ZonedDateTime;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface AuditRepository extends JpaRepository<Audit, UUID> {

    // ----------------------------------------------------
    // Standard paged queries

    Page<Audit> findByCreatedAtAfter(
        ZonedDateTime createdAt,
        Pageable pageable);

    Page<Audit> findByCreatedAtBetween(
        ZonedDateTime createdAtAfter,
        ZonedDateTime createdAtBefore,
        Pageable pageable);

    Page<Audit> findByCourtIdAndCreatedAtAfter(
        UUID courtId,
        ZonedDateTime createdAt,
        Pageable pageable);

    Page<Audit> findByCourtIdAndCreatedAtBetween(
        UUID courtId,
        ZonedDateTime createdAtAfter,
        ZonedDateTime createdAtBefore,
        Pageable pageable);

    // ----------------------------------------------------
    // Complex paged queries

    String SELECT_WITH_USER_JOIN = """
        select
            a
        from
            Audit a
        join
            User u on a.userId = u.id
        """;

    String COUNT_WITH_USER_JOIN = """
        select
            count(*)
        from
            Audit a
        join
            User u on a.userId = u.id
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

    String WHERE_COURT_ID_EQUALS = " and a.courtId = :courtId";


    @Query(value = SELECT_WITH_USER_JOIN + WHERE_EMAIL_LIKE_AND_CREATED_AT_AFTER,
        countQuery = COUNT_WITH_USER_JOIN + WHERE_EMAIL_LIKE_AND_CREATED_AT_AFTER)
    Page<Audit> findByCreatedAtAfterAndEmailAddressLike(
        ZonedDateTime createdAtAfter,
        String email,
        Pageable pageable);

    @Query(value = SELECT_WITH_USER_JOIN + WHERE_EMAIL_LIKE_AND_CREATED_AT_BETWEEN,
        countQuery = COUNT_WITH_USER_JOIN + WHERE_EMAIL_LIKE_AND_CREATED_AT_BETWEEN)
    Page<Audit> findByCreatedAtBetweenAndEmailAddressLike(
        ZonedDateTime createdAtAfter,
        ZonedDateTime createdAtBefore,
        String email,
        Pageable pageable);

    @Query(value = SELECT_WITH_USER_JOIN + WHERE_EMAIL_LIKE_AND_CREATED_AT_AFTER + WHERE_COURT_ID_EQUALS,
        countQuery = COUNT_WITH_USER_JOIN + WHERE_EMAIL_LIKE_AND_CREATED_AT_AFTER + WHERE_COURT_ID_EQUALS)
    Page<Audit> findByCourtIdAndCreatedAtAfterAndEmailAddressLike(
        UUID courtId,
        ZonedDateTime createdAtAfter,
        String email,
        Pageable pageable);

    @Query(value = SELECT_WITH_USER_JOIN + WHERE_EMAIL_LIKE_AND_CREATED_AT_BETWEEN + WHERE_COURT_ID_EQUALS,
        countQuery = COUNT_WITH_USER_JOIN + WHERE_EMAIL_LIKE_AND_CREATED_AT_BETWEEN + WHERE_COURT_ID_EQUALS)
    Page<Audit> findByCourtIdAndCreatedAtBetweenAndEmailAddressLike(
        UUID courtId,
        ZonedDateTime createdAtAfter,
        ZonedDateTime createdAtBefore,
        String email,
        Pageable pageable);

    // ----------------------------------------------------
    // Housekeeping queries

    void deleteAllByCreatedAtBefore(ZonedDateTime createdAtBefore);

}
