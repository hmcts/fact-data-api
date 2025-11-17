package uk.gov.hmcts.reform.fact.data.api.migration.model;

import java.util.List;
import java.util.UUID;

public record PhotoMigrationResponse(
    String message,
    List<Failure> failedFiles
) {
    public record Failure(String name, UUID id, String error) {}
}
