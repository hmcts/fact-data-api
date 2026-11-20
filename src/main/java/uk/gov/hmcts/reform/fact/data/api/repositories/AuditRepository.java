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

    @EntityGraph(attributePaths = {"user"})
    Optional<Audit> findWithUserById(UUID id);

    @EntityGraph(attributePaths = {"user"})
    @Query("""
        select a from Audit a
        join a.user u
        where a.createdAt >= :fromDate
            and a.createdAt <= coalesce(:toDate, a.createdAt)
            and a.subjectId = coalesce(:subjectId, a.subjectId)
            and a.subjectType = coalesce(:subjectType, a.subjectType)
            and (cast(:pattern as String) is null
                or lower(u.email) ilike cast(:pattern as String) escape '\\'
                or a.actionEntity ilike cast(:pattern as String) escape '\\'
                or cast(a.actionType as String) ilike cast(:pattern as String) escape '\\'
                or cast(function('regexp_replace', cast(a.actionDataDiff as String),
                                 '.(field|newValue|oldValue).: ', '', 'g') as String)
                   ilike cast(:pattern as String) escape '\\')
            and (cast(:email as String) is null
                or lower(u.email) like concat('%', lower(cast(:email as String)), '%'))
    """)
    Page<Audit> findFiltered(
        ZonedDateTime fromDate,
        ZonedDateTime toDate,
        UUID subjectId,
        SubjectType subjectType,
        String pattern,
        String email,
        Pageable pageable
    );

    void deleteAllByCreatedAtBefore(ZonedDateTime createdAtBefore);

    /**
     * removes all audits where the subject id is in the given list.
     *
     * @param auditSubjectIds the list of audit subject ids to delete
     */
    void deleteBySubjectIdIn(List<UUID> auditSubjectIds);
}
