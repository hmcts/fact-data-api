package uk.gov.hmcts.reform.fact.data.api.repositories;

import uk.gov.hmcts.reform.fact.data.api.entities.CourtLock;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CourtLockRepository extends JpaRepository<CourtLock, UUID> {
}
