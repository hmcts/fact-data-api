package uk.gov.hmcts.reform.fact.data.api.migration.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MigrationResponse {
    private String message;
    private MigrationResult result;
}
