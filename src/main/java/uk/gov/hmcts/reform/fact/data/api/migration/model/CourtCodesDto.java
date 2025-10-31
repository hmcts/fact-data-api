package uk.gov.hmcts.reform.fact.data.api.migration.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public record CourtCodesDto(
    String id,
    @JsonProperty("magistrate_court_code") Integer magistrateCourtCode,
    @JsonProperty("family_court_code") Integer familyCourtCode,
    @JsonProperty("tribunal_code") Integer tribunalCode,
    @JsonProperty("county_court_code") Integer countyCourtCode,
    @JsonProperty("crown_court_code") Integer crownCourtCode,
    String gbs
) {
}
