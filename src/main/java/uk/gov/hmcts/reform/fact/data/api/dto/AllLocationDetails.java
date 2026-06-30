package uk.gov.hmcts.reform.fact.data.api.dto;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import uk.gov.hmcts.reform.fact.data.api.entities.CourtDetails;
import uk.gov.hmcts.reform.fact.data.api.entities.ServiceCentreDetails;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor(access = AccessLevel.PUBLIC)
public class AllLocationDetails {

    private String locationType;
    private Boolean serviceCentre;
    private CourtDetails court;
    private ServiceCentreDetails serviceCentreDetails;

    public static AllLocationDetails fromCourt(CourtDetails courtDetails) {
        return AllLocationDetails.builder()
            .locationType("COURT")
            .serviceCentre(Boolean.FALSE)
            .court(courtDetails)
            .serviceCentreDetails(null)
            .build();
    }

    public static AllLocationDetails fromServiceCentre(ServiceCentreDetails serviceCentreDetails) {
        return AllLocationDetails.builder()
            .locationType("SERVICE_CENTRE")
            .serviceCentre(Boolean.TRUE)
            .court(null)
            .serviceCentreDetails(serviceCentreDetails)
            .build();
    }
}
