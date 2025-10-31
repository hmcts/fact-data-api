package uk.gov.hmcts.reform.fact.data.api.migration.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public record CourtDxCodeDto(
    String id,
    @JsonProperty("dx_code") String dxCode,
    String explanation
) {
}
