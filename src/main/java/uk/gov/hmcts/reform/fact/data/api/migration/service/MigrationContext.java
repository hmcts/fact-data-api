package uk.gov.hmcts.reform.fact.data.api.migration.service;

import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.Getter;
import org.apache.commons.lang3.StringUtils;
import uk.gov.hmcts.reform.fact.data.api.migration.model.CourtDto;
import uk.gov.hmcts.reform.fact.data.api.migration.model.CourtLocalAuthorityDto;
import uk.gov.hmcts.reform.fact.data.api.migration.model.CourtServiceAreaDto;
import uk.gov.hmcts.reform.fact.data.api.migration.model.LegacyExportResponse;
import uk.gov.hmcts.reform.fact.data.api.migration.model.MigrationFinding;
import uk.gov.hmcts.reform.fact.data.api.migration.model.MigrationFindingCounts;
import uk.gov.hmcts.reform.fact.data.api.migration.model.MigrationFindingSeverity;
import uk.gov.hmcts.reform.fact.data.api.migration.model.MigrationFindingType;
import uk.gov.hmcts.reform.fact.data.api.migration.model.MigrationSection;
import uk.gov.hmcts.reform.fact.data.api.migration.model.MigrationSectionCount;
import uk.gov.hmcts.reform.fact.data.api.migration.model.ServiceDto;

@Getter
class MigrationContext {
    private final Map<Integer, UUID> regionIds = new HashMap<>();
    private final Map<Integer, UUID> areaOfLawIds = new HashMap<>();
    private final Map<Integer, UUID> serviceAreaIds = new HashMap<>();
    private final Map<Integer, List<UUID>> localAuthorityTypeIds = new HashMap<>();
    private final Map<MigrationSection, MigrationSectionCount> sectionCounts =
        new EnumMap<>(MigrationSection.class);
    private final List<MigrationFinding> findings = new ArrayList<>();
    int courtAreasOfLawMigrated;
    int courtLocalAuthoritiesMigrated;
    int courtSinglePointsOfEntryMigrated;
    int courtProfessionalInformationMigrated;
    int courtCodesMigrated;
    int courtDxCodesMigrated;
    int courtFaxMigrated;
    int serviceCentreAreasOfLawMigrated;
    int warningNoticesMigrated;
    int courtSlugsPreserved;
    int servicesMigrated;
    int serviceAreaLinksMigrated;

    MigrationContext() {
        for (MigrationSection section : MigrationSection.values()) {
            sectionCounts.put(section, new MigrationSectionCount());
        }
    }

    void initialiseSourceCounts(LegacyExportResponse response) {
        source(MigrationSection.REGIONS, size(response.getRegions()), 0);
        source(MigrationSection.AREAS_OF_LAW, size(response.getAreaOfLawTypes()), 0);
        source(MigrationSection.SERVICE_AREAS, size(response.getServiceAreas()), 0);
        source(MigrationSection.LOCAL_AUTHORITY_TYPES, size(response.getLocalAuthorityTypes()), 0);
        source(MigrationSection.CONTACT_DESCRIPTION_TYPES, size(response.getContactDescriptionTypes()), 0);
        source(MigrationSection.OPENING_HOUR_TYPES, size(response.getOpeningHourTypes()), 0);
        source(MigrationSection.COURT_TYPES, size(response.getCourtTypes()), 0);
        source(MigrationSection.SERVICES, size(response.getServices()), 0);
        recordDeferredExportSections(response);
        if (safe(response.getCourts()).isEmpty()) {
            finding(
                MigrationFindingSeverity.ERROR,
                MigrationFindingType.REJECTED,
                MigrationSection.COURTS,
                "COURT_SOURCE_COLLECTION_MISSING",
                null,
                null,
                null,
                List.of(),
                List.of(),
                1,
                0
            );
        }

        for (ServiceDto service : safe(response.getServices())) {
            source(MigrationSection.SERVICE_AREA_LINKS, 0, size(service.getServiceAreaIds()));
        }
        for (CourtDto court : safe(response.getCourts())) {
            initialiseCourtSourceCounts(court);
        }
    }

    private void recordDeferredExportSections(LegacyExportResponse response) {
        int courtTypes = size(response.getCourtTypes());
        if (courtTypes > 0) {
            discarded(MigrationSection.COURT_TYPES, courtTypes, 0);
            finding(
                MigrationFindingSeverity.REVIEW,
                MigrationFindingType.DEFERRED,
                MigrationSection.COURT_TYPES,
                "COURT_TYPE_ASSOCIATIONS_UNSUPPORTED",
                null,
                null,
                null,
                List.of(),
                List.of(),
                courtTypes,
                0
            );
        }
    }

    private void initialiseCourtSourceCounts(CourtDto court) {
        boolean serviceCentre = Boolean.TRUE.equals(court.getIsServiceCentre());
        MigrationSection locationSection = serviceCentre
            ? MigrationSection.SERVICE_CENTRES
            : MigrationSection.COURTS;
        source(locationSection, 1, 0);
        source(MigrationSection.COURT_SLUGS, 1, 0);

        if (StringUtils.isNotBlank(court.getWarningNotice())
            || StringUtils.isNotBlank(court.getWarningNoticeCy())) {
            source(MigrationSection.WARNING_NOTICES, 1, 0);
        }
        if (court.getCourtPhoto() != null) {
            source(MigrationSection.PHOTOS, 1, 0);
            discarded(MigrationSection.PHOTOS, 1, 0);
            finding(
                MigrationFindingSeverity.REVIEW,
                MigrationFindingType.DEFERRED,
                MigrationSection.PHOTOS,
                "PHOTO_SEPARATE_MIGRATION_WORKSTREAM",
                court.getId(),
                court.getSlug(),
                null,
                List.of(),
                List.of(),
                1,
                0
            );
        }

        if (serviceCentre) {
            sourceCourtServiceAreas(court, MigrationSection.SERVICE_CENTRE_SERVICE_AREAS);
            sourceCourtAreaOfLaw(court, MigrationSection.SERVICE_CENTRE_AREAS_OF_LAW);
            int deferredRecords = size(court.getCourtLocalAuthorities())
                + present(court.getCourtSinglePointsOfEntry())
                + present(court.getCourtProfessionalInformation())
                + present(court.getCourtCodes())
                + size(court.getCourtDxCodes())
                + size(court.getCourtFax());
            source(MigrationSection.SERVICE_CENTRE_DEFERRED_DETAILS, deferredRecords, 0);
            return;
        }

        source(MigrationSection.LEGACY_COURT_MAPPINGS, court.getId() == null ? 0 : 1, 0);
        sourceCourtServiceAreas(court, MigrationSection.ORDINARY_COURT_SERVICE_AREAS);
        sourceCourtAreaOfLaw(court, MigrationSection.COURT_AREAS_OF_LAW);
        sourceEmbeddedReferences(
            MigrationSection.COURT_SINGLE_POINTS_OF_ENTRY,
            court.getCourtSinglePointsOfEntry(),
            court.getCourtSinglePointsOfEntry() == null
                ? List.of()
                : court.getCourtSinglePointsOfEntry().getAreaOfLawIds()
        );
        for (CourtLocalAuthorityDto localAuthority : safe(court.getCourtLocalAuthorities())) {
            source(
                MigrationSection.COURT_LOCAL_AUTHORITIES,
                1,
                size(localAuthority.getLocalAuthorityIds()) + present(localAuthority.getAreaOfLawId())
            );
        }
        source(
            MigrationSection.COURT_PROFESSIONAL_INFORMATION,
            present(court.getCourtProfessionalInformation()),
            0
        );
        source(MigrationSection.COURT_CODES, present(court.getCourtCodes()), 0);
        source(MigrationSection.COURT_DX_CODES, size(court.getCourtDxCodes()), 0);
        source(MigrationSection.COURT_FAX, size(court.getCourtFax()), 0);
    }

    private void sourceCourtAreaOfLaw(CourtDto court, MigrationSection section) {
        sourceEmbeddedReferences(
            section,
            court.getCourtAreasOfLaw(),
            court.getCourtAreasOfLaw() == null ? List.of() : court.getCourtAreasOfLaw().getAreaOfLawIds()
        );
    }

    private void sourceCourtServiceAreas(CourtDto court, MigrationSection section) {
        for (CourtServiceAreaDto serviceArea : safe(court.getCourtServiceAreas())) {
            source(section, 1, size(serviceArea.getServiceAreaIds()));
        }
    }

    private void sourceEmbeddedReferences(MigrationSection section, Object value, List<Integer> references) {
        source(section, present(value), size(references));
    }

    void source(MigrationSection section, int records, int references) {
        MigrationSectionCount count = sectionCounts.get(section);
        count.setSourceRecords(count.getSourceRecords() + records);
        count.setSourceReferences(count.getSourceReferences() + references);
    }

    void persisted(MigrationSection section, int records, int references) {
        MigrationSectionCount count = sectionCounts.get(section);
        count.setPersistedRecords(count.getPersistedRecords() + records);
        count.setPersistedReferences(count.getPersistedReferences() + references);
    }

    void skipped(MigrationSection section, int records) {
        MigrationSectionCount count = sectionCounts.get(section);
        count.setSkippedRecords(count.getSkippedRecords() + records);
    }

    void discarded(MigrationSection section, int records, int references) {
        MigrationSectionCount count = sectionCounts.get(section);
        count.setDiscardedRecords(count.getDiscardedRecords() + records);
        count.setDiscardedReferences(count.getDiscardedReferences() + references);
    }

    void transformed(MigrationSection section, int records) {
        MigrationSectionCount count = sectionCounts.get(section);
        count.setTransformedRecords(count.getTransformedRecords() + records);
    }

    void unmapped(MigrationSection section, int references) {
        MigrationSectionCount count = sectionCounts.get(section);
        count.setUnmappedReferences(count.getUnmappedReferences() + references);
    }

    void finding(
        MigrationFindingSeverity severity,
        MigrationFindingType type,
        MigrationSection section,
        String reasonCode,
        Long legacyCourtId,
        String courtSlug,
        Integer sourceRecordId,
        List<Integer> sourceReferenceIds,
        List<String> fields,
        int affectedRecords,
        int affectedReferences
    ) {
        findings.add(MigrationFinding.builder()
            .severity(severity)
            .type(type)
            .section(section)
            .reasonCode(reasonCode)
            .legacyCourtId(legacyCourtId)
            .courtSlug(courtSlug)
            .sourceRecordId(sourceRecordId)
            .sourceReferenceIds(sourceReferenceIds == null ? List.of() : List.copyOf(sourceReferenceIds))
            .fields(fields == null ? List.of() : List.copyOf(fields))
            .affectedRecords(affectedRecords)
            .affectedReferences(affectedReferences)
            .build());
    }

    MigrationFindingCounts findingCounts() {
        int errors = 0;
        int review = 0;
        int information = 0;
        for (MigrationFinding finding : findings) {
            switch (finding.getSeverity()) {
                case ERROR -> errors++;
                case REVIEW -> review++;
                case INFO -> information++;
                default -> throw new IllegalStateException("Unsupported finding severity");
            }
        }
        return new MigrationFindingCounts(errors, review, information);
    }

    Map<MigrationSection, MigrationSectionCount> copySectionCounts() {
        Map<MigrationSection, MigrationSectionCount> result = new EnumMap<>(MigrationSection.class);
        sectionCounts.forEach((section, count) -> result.put(section, new MigrationSectionCount(count)));
        return result;
    }

    private static int present(Object value) {
        return value == null ? 0 : 1;
    }

    private static int size(Collection<?> values) {
        return values == null ? 0 : values.size();
    }

    private static <T> List<T> safe(List<T> values) {
        return values == null ? List.of() : values;
    }
}
