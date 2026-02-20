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
public class CourtServiceAreaDto {
    private Integer id;
    @JsonProperty("catchment_type")
    private String catchmentType;
    @JsonProperty("service_area_ids")
    private List<Integer> serviceAreaIds;
}
