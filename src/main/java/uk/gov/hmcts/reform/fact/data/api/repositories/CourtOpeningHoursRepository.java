package uk.gov.hmcts.reform.fact.data.api.repositories;

import uk.gov.hmcts.reform.fact.data.api.entities.CourtOpeningHours;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CourtOpeningHoursRepository extends JpaRepository<CourtOpeningHours, UUID> {

    List<CourtOpeningHours> findByCourtId(UUID courtId);

    List<CourtOpeningHours> findByCourtIdAndOpeningHourTypeId(UUID courtId, UUID openingHourTypeId);

    void deleteByCourtIdAndOpeningHourTypeId(UUID courtId, UUID openingHourId);
}
