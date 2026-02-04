package uk.gov.hmcts.reform.fact.data.api.repositories;

import uk.gov.hmcts.reform.fact.data.api.entities.CourtDetails;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CourtDetailsRepository extends JpaRepository<CourtDetails, UUID> {
    /**
     * Finds a court details record by slug.
     *
     * @param slug the court slug
     * @return matching court details if present
     */
    Optional<CourtDetails> findBySlug(String slug);
}
