package uk.gov.hmcts.reform.fact.data.api.repositories;

import uk.gov.hmcts.reform.fact.data.api.entities.CourtCode;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface CourtCodeRepository extends JpaRepository<CourtCode, UUID> {
}
