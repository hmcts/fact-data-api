package uk.gov.hmcts.reform.fact.data.api.migration;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.hmcts.reform.fact.data.api.migration.entities.MigrationAudit;
import uk.gov.hmcts.reform.fact.data.api.migration.entities.MigrationStatus;
import uk.gov.hmcts.reform.fact.data.api.migration.model.MigrationFinding;
import uk.gov.hmcts.reform.fact.data.api.migration.model.MigrationFindingCounts;
import uk.gov.hmcts.reform.fact.data.api.migration.model.MigrationFindingSeverity;
import uk.gov.hmcts.reform.fact.data.api.migration.model.MigrationFindingType;
import uk.gov.hmcts.reform.fact.data.api.migration.model.MigrationReport;
import uk.gov.hmcts.reform.fact.data.api.migration.model.MigrationResult;
import uk.gov.hmcts.reform.fact.data.api.migration.model.MigrationSection;
import uk.gov.hmcts.reform.fact.data.api.migration.model.MigrationSectionCount;
import uk.gov.hmcts.reform.fact.data.api.migration.model.MigrationSource;
import uk.gov.hmcts.reform.fact.data.api.migration.repository.MigrationAuditRepository;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class MigrationReportPersistenceTest {

    @Autowired
    private MigrationAuditRepository migrationAuditRepository;

    @Test
    void persistsAndReadsStructuredJsonReport() {
        final Instant now = Instant.now();
        MigrationSectionCount courts = new MigrationSectionCount();
        courts.setSourceRecords(429);
        courts.setPersistedRecords(429);

        MigrationResult result = new MigrationResult();
        result.setReconciliationPassed(true);
        result.setFindingCounts(new MigrationFindingCounts(0, 1, 0));
        result.getSectionCounts().put(MigrationSection.COURTS, courts);

        MigrationFinding finding = MigrationFinding.builder()
            .severity(MigrationFindingSeverity.REVIEW)
            .type(MigrationFindingType.APPROVED_DISCARD)
            .section(MigrationSection.COURT_LOCAL_AUTHORITIES)
            .reasonCode("FACT_2612_UNSUPPORTED_LOCAL_AUTHORITY_AREA")
            .legacyCourtId(123L)
            .courtSlug("example-court")
            .sourceRecordId(456)
            .sourceReferenceIds(List.of(1, 2))
            .affectedRecords(1)
            .affectedReferences(2)
            .build();

        MigrationAudit audit = migrationAuditRepository.saveAndFlush(MigrationAudit.builder()
            .migrationName("report-json-" + UUID.randomUUID())
            .status(MigrationStatus.SUCCESS)
            .startedAt(now)
            .completedAt(now)
            .updatedAt(now)
            .build());
        MigrationReport report = MigrationReport.builder()
            .id(audit.getId())
            .migrationName(audit.getMigrationName())
            .status(MigrationStatus.SUCCESS)
            .startedAt(now)
            .completedAt(now)
            .source(new MigrationSource("http://source.internal", now, "a".repeat(64)))
            .result(result)
            .findings(List.of(finding))
            .build();
        audit.setReport(report);
        migrationAuditRepository.saveAndFlush(audit);

        MigrationReport persisted = migrationAuditRepository.findById(audit.getId())
            .orElseThrow()
            .getReport();

        assertThat(persisted).isEqualTo(report);
        assertThat(persisted.getResult().getSectionCounts().get(MigrationSection.COURTS)
            .getPersistedRecords()).isEqualTo(429);
        assertThat(persisted.getFindings()).containsExactly(finding);
    }
}
