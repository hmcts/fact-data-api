package uk.gov.hmcts.reform.fact.data.api.migration.repository;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import uk.gov.hmcts.reform.fact.data.api.migration.entities.MigrationAudit;

@Repository
public interface MigrationAuditRepository extends JpaRepository<MigrationAudit, UUID> {
    Optional<MigrationAudit> findByMigrationName(String migrationName);
}
