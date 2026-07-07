package uk.gov.hmcts.reform.fact.data.api.repositories;

import uk.gov.hmcts.reform.fact.data.api.entities.Lock;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import uk.gov.hmcts.reform.fact.data.api.entities.types.SubjectType;
import uk.gov.hmcts.reform.fact.data.api.entities.types.Page;

@Repository
public interface LockRepository extends JpaRepository<Lock, UUID> {
    void deleteAllByUserId(UUID userId);

    @EntityGraph(attributePaths = {"user"})
    List<Lock> findAllBySubjectTypeAndSubjectId(SubjectType subjectType, UUID subjectId);

    @EntityGraph(attributePaths = {"user"})
    Optional<Lock> findBySubjectTypeAndSubjectIdAndPage(SubjectType subjectType, UUID subjectId, Page page);

    void deleteBySubjectTypeAndSubjectIdAndPage(SubjectType subjectType, UUID subjectId, Page page);

    void deleteByLockAcquiredBefore(ZonedDateTime lockAcquired);

    void deleteAllByUserIdAndIdIsNot(UUID userId, UUID id);
}
