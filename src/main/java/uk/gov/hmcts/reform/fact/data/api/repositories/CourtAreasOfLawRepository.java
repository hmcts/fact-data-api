package uk.gov.hmcts.reform.fact.data.api.repositories;

import uk.gov.hmcts.reform.fact.data.api.entities.CourtAreasOfLaw;

import java.util.Optional;
import java.util.UUID;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CourtAreasOfLawRepository extends JpaRepository<CourtAreasOfLaw, UUID> {
    Optional<CourtAreasOfLaw> findByCourtId(UUID courtId);
}
