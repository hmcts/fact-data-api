package uk.gov.hmcts.reform.fact.data.api.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import uk.gov.hmcts.reform.fact.data.api.entities.ServiceCentre;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ServiceCentreRepository extends JpaRepository<ServiceCentre, UUID> {

    Optional<ServiceCentre> findByName(String name);

    boolean existsBySlug(String slug);

    List<ServiceCentre> findByNameStartingWithIgnoreCase(String namePrefix);
}
