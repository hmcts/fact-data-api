package uk.gov.hmcts.reform.fact.data.api.migration.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public record ServiceAreaDto(
    Integer id,
    String name,
    @JsonProperty("name_cy") String nameCy,
    String description,
    @JsonProperty("description_cy") String descriptionCy,
    @JsonProperty("online_url") String onlineUrl,
    @JsonProperty("online_text") String onlineText,
    @JsonProperty("online_text_cy") String onlineTextCy,
    String type,
    String text,
    @JsonProperty("text_cy") String textCy,
    @JsonProperty("catchment_method") String catchmentMethod,
    @JsonProperty("area_of_law_id") Integer areaOfLawId
) {
}
