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
public class CourtDto {
    private Long id;
    private String name;
    private String slug;
    private Boolean open;
    @JsonProperty("region_id")
    private Integer regionId;
    @JsonProperty("court_service_areas")
    private List<CourtServiceAreaDto> courtServiceAreas;
    @JsonProperty("court_local_authorities")
    private List<CourtLocalAuthorityDto> courtLocalAuthorities;
    @JsonProperty("court_professional_information")
    private CourtProfessionalInformationDto courtProfessionalInformation;
    @JsonProperty("court_codes")
    private CourtCodesDto courtCodes;
    @JsonProperty("court_areas_of_law")
    private CourtAreasOfLawDto courtAreasOfLaw;
    @JsonProperty("court_single_points_of_entry")
    private CourtSinglePointOfEntryDto courtSinglePointsOfEntry;
    @JsonProperty("court_dx_codes")
    private List<CourtDxCodeDto> courtDxCodes;
    @JsonProperty("court_fax")
    private List<CourtFaxDto> courtFax;
    @JsonProperty("court_photo")
    private CourtPhotoDto courtPhoto;
    @JsonProperty("is_service_centre")
    private Boolean isServiceCentre;
}
