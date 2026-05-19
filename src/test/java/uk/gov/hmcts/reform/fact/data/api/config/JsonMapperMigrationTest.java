package uk.gov.hmcts.reform.fact.data.api.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.util.Map;

import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.cfg.DateTimeFeature;
import tools.jackson.databind.json.JsonMapper;

class JsonMapperMigrationTest {

    @Test
    void shouldSerializeJavaTimeTypesWithoutExtraModules() {
        ObjectMapper mapper = JsonMapper.builder()
            .disable(DateTimeFeature.WRITE_DATES_AS_TIMESTAMPS)
            .build();

        String json = mapper.writeValueAsString(Map.of("date", LocalDate.of(2026, 5, 19)));

        assertThat(json).contains("\"date\":\"2026-05-19\"");
    }
}
