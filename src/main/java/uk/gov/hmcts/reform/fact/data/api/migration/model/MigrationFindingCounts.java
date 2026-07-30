package uk.gov.hmcts.reform.fact.data.api.migration.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MigrationFindingCounts {
    private int errors;
    private int review;
    private int information;
}
