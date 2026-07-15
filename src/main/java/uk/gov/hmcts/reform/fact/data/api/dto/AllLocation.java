package uk.gov.hmcts.reform.fact.data.api.dto;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import uk.gov.hmcts.reform.fact.data.api.entities.Court;
import uk.gov.hmcts.reform.fact.data.api.entities.ServiceCentre;

import java.time.ZonedDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor(access = AccessLevel.PUBLIC)
public class AllLocation {

    private UUID id;
    private String name;
    private String slug;
    private Boolean open;
    private String warningNotice;
    private String warningNoticeCy;
    private ZonedDateTime createdAt;
    private ZonedDateTime lastUpdatedAt;
    private String locationType;
    private Boolean serviceCentre;
    private UUID regionId;
    private Boolean openOnCath;
    private String mrdId;

    public static AllLocation fromCourt(Court court) {
        return AllLocation.builder()
            .id(court.getId())
            .name(court.getName())
            .slug(court.getSlug())
            .open(court.getOpen())
            .warningNotice(court.getWarningNotice())
            .createdAt(court.getCreatedAt())
            .lastUpdatedAt(court.getLastUpdatedAt())
            .locationType("COURT")
            .serviceCentre(Boolean.FALSE)
            .regionId(court.getRegionId())
            .openOnCath(court.getOpenOnCath())
            .mrdId(court.getMrdId())
            .build();
    }

    public static AllLocation fromServiceCentre(ServiceCentre serviceCentre) {
        return AllLocation.builder()
            .id(serviceCentre.getId())
            .name(serviceCentre.getName())
            .slug(serviceCentre.getSlug())
            .open(serviceCentre.getOpen())
            .warningNotice(serviceCentre.getWarningNotice())
            .warningNoticeCy(serviceCentre.getWarningNoticeCy())
            .createdAt(serviceCentre.getCreatedAt())
            .lastUpdatedAt(serviceCentre.getLastUpdatedAt())
            .locationType("SERVICE_CENTRE")
            .serviceCentre(Boolean.TRUE)
            // Service centres do not currently carry court/CaTH fields in the summary response.
            .regionId(serviceCentre.getRegionId())
            .openOnCath(null)
            .mrdId(null)
            .build();
    }
}
