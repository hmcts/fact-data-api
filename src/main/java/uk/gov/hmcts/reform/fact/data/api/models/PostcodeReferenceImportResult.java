package uk.gov.hmcts.reform.fact.data.api.models;

import java.time.Instant;

public record PostcodeReferenceImportResult(
    boolean imported,
    String sourceVersion,
    String release,
    long recordCount,
    Instant importedAt
) {
}
