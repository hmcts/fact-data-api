package uk.gov.hmcts.reform.fact.data.api.repositories;

import uk.gov.hmcts.reform.fact.data.api.entities.CourtAccessibilityOption;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface CourtAccessibilityOptionRepository extends JpaRepository<CourtAccessibilityOption, UUID> {
}
