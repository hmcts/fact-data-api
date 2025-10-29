package uk.gov.hmcts.reform.fact.data.api.repositories;

import uk.gov.hmcts.reform.fact.data.api.entities.CourtPhoto;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CourtPhotoRepository extends JpaRepository<CourtPhoto, UUID> {
    Optional<CourtPhoto> findCourtPhotoByCourtId(UUID courtId);
}
