package uk.gov.hmcts.reform.fact.data.api.repositories;

import uk.gov.hmcts.reform.fact.data.api.entities.CourtProfessionalInformation;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface CourtProfessionalInformationRepository extends JpaRepository<CourtProfessionalInformation, UUID> {
}
