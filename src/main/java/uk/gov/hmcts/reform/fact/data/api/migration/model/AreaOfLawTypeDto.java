package uk.gov.hmcts.reform.fact.data.api.migration.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public record AreaOfLawTypeDto(
    Integer id,
    String name,
    @JsonProperty("name_cy") String nameCy
) {
}
