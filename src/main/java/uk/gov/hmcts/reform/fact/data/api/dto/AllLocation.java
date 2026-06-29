package uk.gov.hmcts.reform.fact.data.api.dto;

import uk.gov.hmcts.reform.fact.data.api.entities.Court;
import uk.gov.hmcts.reform.fact.data.api.entities.ServiceCentre;

import java.time.ZonedDateTime;
import java.util.UUID;

public record AllLocation(
    UUID id,
    String name,
    String slug,
    Boolean open,
    String warningNotice,
    ZonedDateTime createdAt,
    ZonedDateTime lastUpdatedAt,
    String locationType,
    Boolean serviceCentre,
    UUID regionId,
    Boolean openOnCath,
    String mrdId
) {

    public static AllLocation fromCourt(Court court) {
        return new AllLocation(
            court.getId(),
            court.getName(),
            court.getSlug(),
            court.getOpen(),
            court.getWarningNotice(),
            court.getCreatedAt(),
            court.getLastUpdatedAt(),
            "COURT",
            Boolean.FALSE,
            court.getRegionId(),
            court.getOpenOnCath(),
            court.getMrdId()
        );
    }

    public static AllLocation fromServiceCentre(ServiceCentre serviceCentre) {
        return new AllLocation(
            serviceCentre.getId(),
            serviceCentre.getName(),
            serviceCentre.getSlug(),
            serviceCentre.getOpen(),
            serviceCentre.getWarningNotice(),
            serviceCentre.getCreatedAt(),
            serviceCentre.getLastUpdatedAt(),
            "SERVICE_CENTRE",
            Boolean.TRUE,
            null,
            null,
            null
        );
    }
}
