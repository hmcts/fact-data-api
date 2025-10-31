package uk.gov.hmcts.reform.fact.data.api.migration.model;

public record MigrationResult(
    int courtsMigrated,
    int regionsMigrated,
    int areaOfLawTypesMigrated,
    int serviceAreasMigrated,
    int servicesMigrated,
    int localAuthorityTypesMigrated,
    int contactDescriptionTypesMigrated,
    int openingHourTypesMigrated,
    int courtTypesMigrated,
    int courtLocalAuthoritiesMigrated
) {
}
