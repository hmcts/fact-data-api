package uk.gov.hmcts.reform.fact.data.api.repositories;

import uk.gov.hmcts.reform.fact.data.api.entities.CourtContactDetail;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface CourtContactDetailRepository extends JpaRepository<CourtContactDetail, UUID> {
}
