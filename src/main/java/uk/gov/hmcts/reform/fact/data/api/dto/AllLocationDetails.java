package uk.gov.hmcts.reform.fact.data.api.dto;

import uk.gov.hmcts.reform.fact.data.api.entities.CourtDetails;
import uk.gov.hmcts.reform.fact.data.api.entities.ServiceCentreDetails;

public record AllLocationDetails(
    String locationType,
    Boolean serviceCentre,
    CourtDetails court,
    ServiceCentreDetails serviceCentreDetails
) {

    public static AllLocationDetails fromCourt(CourtDetails courtDetails) {
        return new AllLocationDetails("COURT", Boolean.FALSE, courtDetails, null);
    }

    public static AllLocationDetails fromServiceCentre(ServiceCentreDetails serviceCentreDetails) {
        return new AllLocationDetails("SERVICE_CENTRE", Boolean.TRUE, null, serviceCentreDetails);
    }
}
