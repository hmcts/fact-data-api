package uk.gov.hmcts.reform.fact.data.api.migration.model;

import java.util.List;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MigrationFinding {
    private MigrationFindingSeverity severity;
    private MigrationFindingType type;
    private MigrationSection section;
    private String reasonCode;
    private Long legacyCourtId;
    private String courtSlug;
    private Integer sourceRecordId;
    @Builder.Default
    private List<Integer> sourceReferenceIds = List.of();
    @Builder.Default
    private List<String> fields = List.of();
    private int affectedRecords;
    private int affectedReferences;
}
