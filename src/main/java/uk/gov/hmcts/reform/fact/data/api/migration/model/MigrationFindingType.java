package uk.gov.hmcts.reform.fact.data.api.migration.model;

public enum MigrationFindingType {
    SKIPPED,
    UNMAPPED,
    REJECTED,
    APPROVED_DISCARD,
    DEFERRED,
    TRANSFORMED
}
