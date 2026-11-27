package uk.gov.hmcts.reform.fact.data.api.os;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class OsDataTest {

    private final ObjectMapper objectMapper = JsonMapper.builder().build();

    @Test
    void shouldOmitUnsetMetadataWithoutChangingExistingFields() {
        OsData data = OsData.builder().results(List.of()).build();

        assertThat(objectMapper.readTree(objectMapper.writeValueAsString(data)))
            .isEqualTo(objectMapper.readTree("{\"header\":null,\"results\":[]}"));
    }

    @ParameterizedTest
    @CsvSource({"false, false", "true, false", "false, true", "true, true"})
    void shouldSerializeIndependentPostcodeFlags(boolean largeUser, boolean terminated) {
        OsData data = OsData.builder()
            .postcodeMetadata(OsPostcodeMetadata.builder().largeUser(largeUser).terminated(terminated).build())
            .build();

        assertThat(objectMapper.readTree(objectMapper.writeValueAsString(data)).get("postcodeMetadata"))
            .isEqualTo(objectMapper.readTree(
                "{\"largeUser\":%s,\"terminated\":%s}".formatted(largeUser, terminated)
            ));
    }
}
