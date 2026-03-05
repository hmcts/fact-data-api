package uk.gov.hmcts.reform.fact.data.api.repositories;

import uk.gov.hmcts.reform.fact.data.api.entities.CourtOpeningHours;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CourtOpeningHoursRepository extends JpaRepository<CourtOpeningHours, UUID> {

    Optional<List<CourtOpeningHours>> findByCourtId(UUID courtId);

    Optional<CourtOpeningHours> findByCourtIdAndId(UUID courtId, UUID openingHourId);

    void deleteByCourtIdAndId(UUID courtId, UUID openingHourId);
}
