package uk.gov.hmcts.reform.fact.data.api.migration.model;

import java.util.EnumMap;
import java.util.Map;
import java.util.UUID;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class MigrationResult {
    private int courtsMigrated;
    private int serviceCentresMigrated;
    private int courtAreasOfLawMigrated;
    private int courtLocalAuthoritiesMigrated;
    private int courtSinglePointsOfEntryMigrated;
    private int courtProfessionalInformationMigrated;
    private int courtCodesMigrated;
    private int courtDxCodesMigrated;
    private int courtFaxMigrated;
    private int serviceCentreAreasOfLawMigrated;
    private int warningNoticesMigrated;
    private int courtSlugsPreserved;
    private int servicesMigrated;
    private int serviceAreaLinksMigrated;
    private UUID reportId;
    private boolean reconciliationPassed;
    private boolean reviewRequired;
    private MigrationFindingCounts findingCounts = new MigrationFindingCounts();
    private Map<MigrationSection, MigrationSectionCount> sectionCounts =
        new EnumMap<>(MigrationSection.class);
}
