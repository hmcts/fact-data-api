package uk.gov.hmcts.reform.fact.data.api.migration.repository;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import uk.gov.hmcts.reform.fact.data.api.migration.entities.LegacyCourtMapping;

@Repository
public interface LegacyCourtMappingRepository extends JpaRepository<LegacyCourtMapping, UUID> {
}
