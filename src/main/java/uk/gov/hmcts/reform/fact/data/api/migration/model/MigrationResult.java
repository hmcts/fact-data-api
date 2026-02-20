package uk.gov.hmcts.reform.fact.data.api.migration.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MigrationResult {
    private int courtsMigrated;
    private int courtAreasOfLawMigrated;
    private int courtServiceAreasMigrated;
    private int courtLocalAuthoritiesMigrated;
    private int courtSinglePointsOfEntryMigrated;
    private int courtProfessionalInformationMigrated;
    private int courtCodesMigrated;
    private int courtDxCodesMigrated;
    private int courtFaxMigrated;
}
