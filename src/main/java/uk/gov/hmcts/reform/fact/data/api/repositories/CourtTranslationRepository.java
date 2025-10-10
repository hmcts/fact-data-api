package uk.gov.hmcts.reform.fact.data.api.repositories;

import uk.gov.hmcts.reform.fact.data.api.entities.CourtTranslation;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface CourtTranslationRepository extends JpaRepository<CourtTranslation, UUID> {
}
