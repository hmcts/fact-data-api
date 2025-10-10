package uk.gov.hmcts.reform.fact.data.api.repositories;

import uk.gov.hmcts.reform.fact.data.api.entities.CourtCounterServiceOpeningHours;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface CourtCounterServiceOpeningHoursRepository
    extends JpaRepository<CourtCounterServiceOpeningHours, UUID> {
}
