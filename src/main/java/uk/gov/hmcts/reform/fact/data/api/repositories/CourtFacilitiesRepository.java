package uk.gov.hmcts.reform.fact.data.api.repositories;

import uk.gov.hmcts.reform.fact.data.api.entities.CourtFacilities;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CourtFacilitiesRepository extends JpaRepository<CourtFacilities, UUID> {
    Optional<CourtFacilities> findByCourtId(UUID courtId);
}
