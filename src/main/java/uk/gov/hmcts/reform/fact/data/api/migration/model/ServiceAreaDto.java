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
public class ServiceAreaDto {
    private Integer id;
    private String name;
    @JsonProperty("name_cy")
    private String nameCy;
    private String description;
    @JsonProperty("description_cy")
    private String descriptionCy;
    @JsonProperty("online_url")
    private String onlineUrl;
    @JsonProperty("online_text")
    private String onlineText;
    @JsonProperty("online_text_cy")
    private String onlineTextCy;
    private String type;
    private String text;
    @JsonProperty("text_cy")
    private String textCy;
    @JsonProperty("catchment_method")
    private String catchmentMethod;
    @JsonProperty("area_of_law_id")
    private Integer areaOfLawId;
}
