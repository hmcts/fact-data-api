package uk.gov.hmcts.reform.fact.data.api.migration.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Wrapper returned by the migration endpoint providing a human-readable message alongside
 * the detailed counts of the imported entities.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class MigrationResponse {
    private String message;
    private MigrationResult result;
}
