package uk.gov.hmcts.reform.fact.data.api.migration.model;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import uk.gov.hmcts.reform.fact.data.api.migration.entities.MigrationStatus;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MigrationReport {
    private UUID id;
    private String migrationName;
    private MigrationStatus status;
    private Instant startedAt;
    private Instant completedAt;
    private MigrationSource source;
    private MigrationResult result;
    @Builder.Default
    private List<MigrationFinding> findings = List.of();
}
