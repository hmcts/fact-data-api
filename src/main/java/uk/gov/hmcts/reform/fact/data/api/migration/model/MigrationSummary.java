package uk.gov.hmcts.reform.fact.data.api.migration.model;

import java.util.List;

public record MigrationSummary(
    MigrationResult result,
    List<String> skippedCourtPostcodes
) {
}
