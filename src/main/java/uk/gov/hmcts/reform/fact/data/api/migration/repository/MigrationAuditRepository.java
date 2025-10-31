package uk.gov.hmcts.reform.fact.data.api.migration.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import uk.gov.hmcts.reform.fact.data.api.migration.entities.MigrationAudit;

import java.util.UUID;

@Repository
public interface MigrationAuditRepository extends JpaRepository<MigrationAudit, UUID> {
}
