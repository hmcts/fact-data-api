package uk.gov.hmcts.reform.fact.data.api.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import uk.gov.hmcts.reform.fact.data.api.entities.ServiceCentreContactDetails;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ServiceCentreContactDetailsRepository extends JpaRepository<ServiceCentreContactDetails, UUID> {

    List<ServiceCentreContactDetails> findByServiceCentreId(UUID serviceCentreId);

    Optional<ServiceCentreContactDetails> findByIdAndServiceCentreId(UUID contactId, UUID serviceCentreId);

    void deleteByIdAndServiceCentreId(UUID contactId, UUID serviceCentreId);

    boolean existsByIdAndServiceCentreId(UUID contactId, UUID serviceCentreId);
}
