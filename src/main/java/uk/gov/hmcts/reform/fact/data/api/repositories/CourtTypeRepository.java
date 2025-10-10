package uk.gov.hmcts.reform.fact.data.api.repositories;

import uk.gov.hmcts.reform.fact.data.api.entities.CourtType;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface CourtTypeRepository extends JpaRepository<CourtType, UUID> {
}
