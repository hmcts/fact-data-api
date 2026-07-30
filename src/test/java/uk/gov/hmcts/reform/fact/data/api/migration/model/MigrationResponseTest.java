package uk.gov.hmcts.reform.fact.data.api.migration.model;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.UUID;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;

class MigrationResponseTest {

    @Test
    void shouldIncludeMigrationCountsInResponseJson() {
        MigrationResult result = new MigrationResult();
        result.setCourtsMigrated(1);
        result.setServiceCentresMigrated(1);
        result.setWarningNoticesMigrated(2);
        result.setCourtSlugsPreserved(5);
        result.setServicesMigrated(7);
        result.setServiceAreaLinksMigrated(22);
        result.setReportId(UUID.randomUUID());
        result.setReconciliationPassed(true);
        result.setReviewRequired(true);
        result.setFindingCounts(new MigrationFindingCounts(0, 2, 5));
        MigrationSectionCount courts = new MigrationSectionCount();
        courts.setSourceRecords(429);
        courts.setPersistedRecords(429);
        result.getSectionCounts().put(MigrationSection.COURTS, courts);

        String json = JsonMapper.builder().build().writeValueAsString(
            new MigrationResponse("Migration completed successfully", result)
        );

        assertThat(json).contains("\"warningNoticesMigrated\":2");
        assertThat(json).contains("\"courtSlugsPreserved\":5");
        assertThat(json).contains("\"servicesMigrated\":7");
        assertThat(json).contains("\"serviceAreaLinksMigrated\":22");
        assertThat(json).contains("\"reconciliationPassed\":true");
        assertThat(json)
            .contains("\"COURTS\":{")
            .contains("\"sourceRecords\":429")
            .contains("\"persistedRecords\":429");
    }
}
