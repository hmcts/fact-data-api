package uk.gov.hmcts.reform.fact.data.api.migration.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import uk.gov.hmcts.reform.fact.data.api.migration.entities.LegacyService;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface LegacyServiceRepository extends JpaRepository<LegacyService, UUID> {
    Optional<LegacyService> findByName(String name);
}
