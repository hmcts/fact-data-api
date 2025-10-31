package uk.gov.hmcts.reform.fact.data.api.migration.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record CourtLocalAuthorityDto(
    Integer id,
    @JsonProperty("area_of_law_id") Integer areaOfLawId,
    @JsonProperty("local_authority_ids") List<Integer> localAuthorityIds
) {
}
