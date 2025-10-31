package uk.gov.hmcts.reform.fact.data.api.migration.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record CourtServiceAreaDto(
    Integer id,
    @JsonProperty("catchment_type") String catchmentType,
    @JsonProperty("service_area_ids") List<Integer> serviceAreaIds
) {
}
