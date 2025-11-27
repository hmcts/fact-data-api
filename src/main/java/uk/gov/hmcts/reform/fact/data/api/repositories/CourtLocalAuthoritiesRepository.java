package uk.gov.hmcts.reform.fact.data.api.repositories;

import uk.gov.hmcts.reform.fact.data.api.entities.CourtLocalAuthorities;

import java.util.UUID;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CourtLocalAuthoritiesRepository extends JpaRepository<CourtLocalAuthorities, UUID> {
    Optional<CourtLocalAuthorities> findByCourtIdAndAreaOfLawId(UUID courtId, UUID areaOfLawId);

    void deleteByCourtId(UUID courtId);
}
