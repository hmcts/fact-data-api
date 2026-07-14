package uk.gov.hmcts.reform.fact.data.api.repositories;

import uk.gov.hmcts.reform.fact.data.api.entities.Approval;
import uk.gov.hmcts.reform.fact.data.api.entities.types.SubjectType;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ApprovalRepository extends JpaRepository<Approval, UUID> {
    Optional<Approval> findBySubjectIdAndSubjectType(UUID subjectId, SubjectType subjectType);

    @EntityGraph(attributePaths = {"user"})
    List<Approval> findAll();
}
