package uk.gov.hmcts.reform.fact.data.api.repositories;

import uk.gov.hmcts.reform.fact.data.api.entities.ServiceArea;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface ServiceAreaRepository extends JpaRepository<ServiceArea, UUID> {
}
