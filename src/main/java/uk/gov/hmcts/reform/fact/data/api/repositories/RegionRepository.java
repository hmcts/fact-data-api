package uk.gov.hmcts.reform.fact.data.api.repositories;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import uk.gov.hmcts.reform.fact.data.api.entities.Region;

@Repository
public interface RegionRepository extends JpaRepository<Region, UUID> {

    Optional<Region> findByNameAndCountry(String name, String country);
}
