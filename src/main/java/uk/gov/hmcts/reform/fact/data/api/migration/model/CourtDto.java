package uk.gov.hmcts.reform.fact.data.api.migration.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record CourtDto(
    String id,
    String name,
    String slug,
    Boolean open,
    @JsonProperty("region_id") Integer regionId,
    @JsonProperty("court_service_areas") List<CourtServiceAreaDto> courtServiceAreas,
    @JsonProperty("court_local_authorities") List<CourtLocalAuthorityDto> courtLocalAuthorities,
    @JsonProperty("court_professional_information") CourtProfessionalInformationDto courtProfessionalInformation,
    @JsonProperty("court_codes") CourtCodesDto courtCodes,
    @JsonProperty("court_areas_of_law") CourtAreasOfLawDto courtAreasOfLaw,
    @JsonProperty("court_single_points_of_entry") CourtSinglePointOfEntryDto courtSinglePointsOfEntry,
    @JsonProperty("court_dx_codes") List<CourtDxCodeDto> courtDxCodes,
    @JsonProperty("court_fax") List<CourtFaxDto> courtFax,
    @JsonProperty("court_photo") CourtPhotoDto courtPhoto,
    @JsonProperty("is_service_centre") Boolean isServiceCentre
) {
}
