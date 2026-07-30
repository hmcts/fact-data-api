package uk.gov.hmcts.reform.fact.data.api.migration.model;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class MigrationSectionCount {
    private int sourceRecords;
    private int sourceReferences;
    private int persistedRecords;
    private int persistedReferences;
    private int skippedRecords;
    private int discardedRecords;
    private int discardedReferences;
    private int transformedRecords;
    private int unmappedReferences;

    public MigrationSectionCount(MigrationSectionCount source) {
        this.sourceRecords = source.sourceRecords;
        this.sourceReferences = source.sourceReferences;
        this.persistedRecords = source.persistedRecords;
        this.persistedReferences = source.persistedReferences;
        this.skippedRecords = source.skippedRecords;
        this.discardedRecords = source.discardedRecords;
        this.discardedReferences = source.discardedReferences;
        this.transformedRecords = source.transformedRecords;
        this.unmappedReferences = source.unmappedReferences;
    }
}
