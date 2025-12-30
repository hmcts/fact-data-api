package uk.gov.hmcts.reform.fact.data.api.repositories;

import jakarta.validation.constraints.NotBlank;
import uk.gov.hmcts.reform.fact.data.api.entities.ServiceArea;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ServiceAreaRepository extends JpaRepository<ServiceArea, UUID> {
    Optional<ServiceArea> findByNameIgnoreCase(
        @NotBlank(message = "The name must be specified") String name
    );
}
