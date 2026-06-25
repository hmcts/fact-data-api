package uk.gov.hmcts.reform.fact.data.api.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import uk.gov.hmcts.reform.fact.data.api.entities.ServiceCentreDetails;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface ServiceCentreDetailsRepository extends JpaRepository<ServiceCentreDetails, UUID> {
    Optional<ServiceCentreDetails> findBySlug(String slug);
}
