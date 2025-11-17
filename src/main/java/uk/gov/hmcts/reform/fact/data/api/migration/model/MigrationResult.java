package uk.gov.hmcts.reform.fact.data.api.migration.model;

public record MigrationResult(
    int courtsMigrated,
    int courtAreasOfLawMigrated,
    int courtServiceAreasMigrated,
    int courtLocalAuthoritiesMigrated,
    int courtSinglePointsOfEntryMigrated,
    int courtProfessionalInformationMigrated,
    int courtCodesMigrated,
    int courtDxCodesMigrated,
    int courtFaxMigrated
) {
}
