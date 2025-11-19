package uk.gov.hmcts.reform.fact.data.api.migration.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@JsonIgnoreProperties(ignoreUnknown = true)
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CourtLocalAuthorityDto {
    private Integer id;
    @JsonProperty("area_of_law_id")
    private Integer areaOfLawId;
    @JsonProperty("local_authority_ids")
    private List<Integer> localAuthorityIds;
}
