package uk.gov.hmcts.reform.fact.data.api.migration.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@JsonIgnoreProperties(ignoreUnknown = true)
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CourtCodesDto {
    private String id;
    @JsonProperty("magistrate_court_code")
    private Integer magistrateCourtCode;
    @JsonProperty("family_court_code")
    private Integer familyCourtCode;
    @JsonProperty("tribunal_code")
    private Integer tribunalCode;
    @JsonProperty("county_court_code")
    private Integer countyCourtCode;
    @JsonProperty("crown_court_code")
    private Integer crownCourtCode;
    private String gbs;
}
