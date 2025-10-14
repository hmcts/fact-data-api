package uk.gov.hmcts.reform.fact.data.api.repositories;

import uk.gov.hmcts.reform.fact.data.api.entities.CourtPostcode;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CourtPostcodeRepository extends JpaRepository<CourtPostcode, UUID> {
}
