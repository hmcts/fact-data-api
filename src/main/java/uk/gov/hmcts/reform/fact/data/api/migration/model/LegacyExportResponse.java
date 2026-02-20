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
public class LegacyExportResponse {
    private List<CourtDto> courts;
    @JsonProperty("local_authority_types")
    private List<LocalAuthorityTypeDto> localAuthorityTypes;
    @JsonProperty("service_areas")
    private List<ServiceAreaDto> serviceAreas;
    private List<ServiceDto> services;
    @JsonProperty("contact_description_types")
    private List<ContactDescriptionTypeDto> contactDescriptionTypes;
    @JsonProperty("opening_hour_types")
    private List<OpeningHourTypeDto> openingHourTypes;
    @JsonProperty("court_types")
    private List<CourtTypeDto> courtTypes;
    private List<RegionDto> regions;
    @JsonProperty("area_of_law_types")
    private List<AreaOfLawTypeDto> areaOfLawTypes;
}
