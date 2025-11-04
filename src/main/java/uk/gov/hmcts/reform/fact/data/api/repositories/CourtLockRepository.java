package uk.gov.hmcts.reform.fact.data.api.repositories;

import uk.gov.hmcts.reform.fact.data.api.entities.CourtLock;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import uk.gov.hmcts.reform.fact.data.api.entities.types.Page;

@Repository
public interface CourtLockRepository extends JpaRepository<CourtLock, UUID> {
    void deleteAllByUserId(UUID userId);

    List<CourtLock> findAllByCourtId(UUID userId);

    Optional<CourtLock> findByCourtIdAndPage(UUID courtId, Page page);

    void deleteByCourtIdAndPage(UUID courtId, Page page);


}
