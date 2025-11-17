package uk.gov.hmcts.reform.fact.data.api.migration.service;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import lombok.Getter;

@Getter
class MigrationContext {
    private final Map<Integer, UUID> regionIds = new HashMap<>();
    private final Map<Integer, UUID> areaOfLawIds = new HashMap<>();
    private final Map<Integer, UUID> serviceAreaIds = new HashMap<>();
    private final Map<Integer, UUID> localAuthorityTypeIds = new HashMap<>();
    private UUID serviceCentreRegionId;
    int courtAreasOfLawMigrated;
    int courtServiceAreasMigrated;
    int courtLocalAuthoritiesMigrated;
    int courtSinglePointsOfEntryMigrated;
    int courtProfessionalInformationMigrated;
    int courtCodesMigrated;
    int courtDxCodesMigrated;
    int courtFaxMigrated;

    void setServiceCentreRegionId(UUID id) {
        this.serviceCentreRegionId = id;
    }
}
