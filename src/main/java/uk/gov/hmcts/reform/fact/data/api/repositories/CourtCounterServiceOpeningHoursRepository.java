package uk.gov.hmcts.reform.fact.data.api.repositories;

import uk.gov.hmcts.reform.fact.data.api.entities.CourtCounterServiceOpeningHours;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CourtCounterServiceOpeningHoursRepository
    extends JpaRepository<CourtCounterServiceOpeningHours, UUID> {

    List<CourtCounterServiceOpeningHours> findByCourtId(UUID courtId);

    void deleteByCourtId(UUID courtId);
}
