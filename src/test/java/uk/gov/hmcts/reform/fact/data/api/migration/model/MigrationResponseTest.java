package uk.gov.hmcts.reform.fact.data.api.migration.model;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;

class MigrationResponseTest {

    @Test
    void shouldIncludeWarningNoticeCountInResponseJson() {
        MigrationResult result = new MigrationResult();
        result.setCourtsMigrated(1);
        result.setServiceCentresMigrated(1);
        result.setWarningNoticesMigrated(2);

        String json = JsonMapper.builder().build().writeValueAsString(
            new MigrationResponse("Migration completed successfully", result)
        );

        assertThat(json).contains("\"warningNoticesMigrated\":2");
    }
}
