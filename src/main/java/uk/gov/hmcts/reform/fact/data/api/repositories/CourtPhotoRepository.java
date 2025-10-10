package uk.gov.hmcts.reform.fact.data.api.repositories;

import uk.gov.hmcts.reform.fact.data.api.entities.CourtPhoto;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface CourtPhotoRepository extends JpaRepository<CourtPhoto, UUID> {
}
