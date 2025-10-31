package uk.gov.hmcts.reform.fact.data.api.migration.model;

/**
 * Wrapper returned by the migration endpoint providing a human-readable message alongside
 * the detailed counts of the imported entities.
 *
 * @param message contextual message for the caller.
 * @param result  detailed counts of entities migrated during the run.
 * @param skippedCourtPostcodes postcodes that were skipped because they did not pass validation.
 */
public record MigrationResponse(
    String message,
    MigrationResult result,
    java.util.List<String> skippedCourtPostcodes
) {
}
