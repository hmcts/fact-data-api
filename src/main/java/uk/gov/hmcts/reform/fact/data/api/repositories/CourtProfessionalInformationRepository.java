package uk.gov.hmcts.reform.fact.data.api.repositories;

import uk.gov.hmcts.reform.fact.data.api.entities.CourtProfessionalInformation;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface CourtProfessionalInformationRepository extends JpaRepository<CourtProfessionalInformation, UUID> {

    Optional<CourtProfessionalInformation> findByCourtId(UUID courtId);

}
