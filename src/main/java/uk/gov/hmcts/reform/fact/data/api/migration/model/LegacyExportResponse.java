package uk.gov.hmcts.reform.fact.data.api.migration.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record LegacyExportResponse(
    List<CourtDto> courts,
    @JsonProperty("local_authority_types") List<LocalAuthorityTypeDto> localAuthorityTypes,
    @JsonProperty("service_areas") List<ServiceAreaDto> serviceAreas,
    List<ServiceDto> services,
    @JsonProperty("contact_description_types") List<ContactDescriptionTypeDto> contactDescriptionTypes,
    @JsonProperty("opening_hour_types") List<OpeningHourTypeDto> openingHourTypes,
    @JsonProperty("court_types") List<CourtTypeDto> courtTypes,
    List<RegionDto> regions,
    @JsonProperty("area_of_law_types") List<AreaOfLawTypeDto> areaOfLawTypes
) {
}
