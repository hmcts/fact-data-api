package uk.gov.hmcts.reform.fact.data.api.migration.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record CourtAreasOfLawDto(
    String id,
    @JsonProperty("areas_of_law") List<Integer> areaOfLawIds
) {
}
