package uk.gov.hmcts.reform.fact.data.api.repositories;

import uk.gov.hmcts.reform.fact.data.api.entities.CourtServiceArea;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface CourtServiceAreaRepository extends JpaRepository<CourtServiceArea, UUID> {
}
