package uk.gov.hmcts.reform.fact.data.api.migration.model;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

class CourtDtoTest {

    @Test
    void shouldDeserialiseWarningNoticesFromLegacyMigrationContract() {
        ObjectMapper mapper = JsonMapper.builder().build();

        CourtDto court = mapper.readValue(
            """
                {
                  "id": 1,
                  "slug": "example-court",
                  "warning_notice": "<strong>Urgent &amp; important</strong>",
                  "warning_notice_cy": "Rhybudd brys"
                }
                """,
            CourtDto.class
        );

        assertThat(court.getWarningNotice()).isEqualTo("<strong>Urgent &amp; important</strong>");
        assertThat(court.getWarningNoticeCy()).isEqualTo("Rhybudd brys");
    }
}
