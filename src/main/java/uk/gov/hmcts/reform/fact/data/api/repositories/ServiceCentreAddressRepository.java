package uk.gov.hmcts.reform.fact.data.api.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import uk.gov.hmcts.reform.fact.data.api.entities.ServiceCentreAddress;

import java.util.UUID;

@Repository
public interface ServiceCentreAddressRepository extends JpaRepository<ServiceCentreAddress, UUID> {}
