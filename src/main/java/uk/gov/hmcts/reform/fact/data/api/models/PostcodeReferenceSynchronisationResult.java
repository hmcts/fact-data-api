package uk.gov.hmcts.reform.fact.data.api.models;

import java.time.Instant;

public record PostcodeReferenceSynchronisationResult(
    boolean blobUpdated,
    boolean imported,
    String sourceVersion,
    String release,
    long recordCount,
    String blobPath,
    Instant importedAt
) {
}
