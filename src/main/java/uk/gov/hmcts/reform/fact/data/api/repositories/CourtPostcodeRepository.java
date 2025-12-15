package uk.gov.hmcts.reform.fact.data.api.repositories;

import uk.gov.hmcts.reform.fact.data.api.entities.CourtPostcode;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CourtPostcodeRepository extends JpaRepository<CourtPostcode, UUID> {
    List<CourtPostcode> getCourtPostcodeByCourtId(UUID courtId);

    List<CourtPostcode> findAllByCourtId(UUID courtId);

    List<CourtPostcode> findAllByCourtIdAndPostcodeIn(UUID courtId, List<String> postcodes);
}
