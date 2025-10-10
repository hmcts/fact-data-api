package uk.gov.hmcts.reform.fact.data.api.repositories;

import uk.gov.hmcts.reform.fact.data.api.entities.CourtFacility;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface CourtFacilityRepository extends JpaRepository<CourtFacility, UUID> {
}
