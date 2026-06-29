package uk.gov.hmcts.reform.fact.data.api.dto;

import org.junit.jupiter.api.Test;
import uk.gov.hmcts.reform.fact.data.api.entities.Court;
import uk.gov.hmcts.reform.fact.data.api.entities.CourtDetails;
import uk.gov.hmcts.reform.fact.data.api.entities.ServiceCentre;
import uk.gov.hmcts.reform.fact.data.api.entities.ServiceCentreDetails;

import java.time.ZonedDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class AllLocationTest {

    private static final UUID LOCATION_ID = UUID.fromString("123e4567-e89b-12d3-a456-426614174000");
    private static final UUID REGION_ID = UUID.fromString("223e4567-e89b-12d3-a456-426614174000");
    private static final ZonedDateTime CREATED_AT = ZonedDateTime.parse("2026-01-01T10:00:00Z");
    private static final ZonedDateTime LAST_UPDATED_AT = ZonedDateTime.parse("2026-01-02T10:00:00Z");

    @Test
    void fromCourtMapsCourtSummaryFields() {
        Court court = Court.builder()
            .id(LOCATION_ID)
            .name("Test Court")
            .slug("test-court")
            .open(true)
            .warningNotice("Court warning")
            .createdAt(CREATED_AT)
            .lastUpdatedAt(LAST_UPDATED_AT)
            .regionId(REGION_ID)
            .openOnCath(false)
            .mrdId("123456")
            .build();

        AllLocation result = AllLocation.fromCourt(court);

        assertThat(result.getId()).isEqualTo(LOCATION_ID);
        assertThat(result.getName()).isEqualTo("Test Court");
        assertThat(result.getSlug()).isEqualTo("test-court");
        assertThat(result.getOpen()).isTrue();
        assertThat(result.getWarningNotice()).isEqualTo("Court warning");
        assertThat(result.getCreatedAt()).isEqualTo(CREATED_AT);
        assertThat(result.getLastUpdatedAt()).isEqualTo(LAST_UPDATED_AT);
        assertThat(result.getLocationType()).isEqualTo("COURT");
        assertThat(result.getServiceCentre()).isFalse();
        assertThat(result.getRegionId()).isEqualTo(REGION_ID);
        assertThat(result.getOpenOnCath()).isFalse();
        assertThat(result.getMrdId()).isEqualTo("123456");
    }

    @Test
    void fromServiceCentreMapsServiceCentreSummaryFieldsAndLeavesCourtFieldsEmpty() {
        ServiceCentre serviceCentre = ServiceCentre.builder()
            .id(LOCATION_ID)
            .name("Test Service Centre")
            .slug("test-service-centre")
            .open(false)
            .warningNotice("Service centre warning")
            .createdAt(CREATED_AT)
            .lastUpdatedAt(LAST_UPDATED_AT)
            .build();

        AllLocation result = AllLocation.fromServiceCentre(serviceCentre);

        assertThat(result.getId()).isEqualTo(LOCATION_ID);
        assertThat(result.getName()).isEqualTo("Test Service Centre");
        assertThat(result.getSlug()).isEqualTo("test-service-centre");
        assertThat(result.getOpen()).isFalse();
        assertThat(result.getWarningNotice()).isEqualTo("Service centre warning");
        assertThat(result.getCreatedAt()).isEqualTo(CREATED_AT);
        assertThat(result.getLastUpdatedAt()).isEqualTo(LAST_UPDATED_AT);
        assertThat(result.getLocationType()).isEqualTo("SERVICE_CENTRE");
        assertThat(result.getServiceCentre()).isTrue();
        assertThat(result.getRegionId()).isNull();
        assertThat(result.getOpenOnCath()).isNull();
        assertThat(result.getMrdId()).isNull();
    }

    @Test
    void fromCourtDetailsWrapsCourtDetails() {
        CourtDetails courtDetails = CourtDetails.builder()
            .id(LOCATION_ID)
            .name("Test Court")
            .build();

        AllLocationDetails result = AllLocationDetails.fromCourt(courtDetails);

        assertThat(result.getLocationType()).isEqualTo("COURT");
        assertThat(result.getServiceCentre()).isFalse();
        assertThat(result.getCourt()).isSameAs(courtDetails);
        assertThat(result.getServiceCentreDetails()).isNull();
    }

    @Test
    void fromServiceCentreDetailsWrapsServiceCentreDetails() {
        ServiceCentreDetails serviceCentreDetails = ServiceCentreDetails.builder()
            .id(LOCATION_ID)
            .name("Test Service Centre")
            .build();

        AllLocationDetails result = AllLocationDetails.fromServiceCentre(serviceCentreDetails);

        assertThat(result.getLocationType()).isEqualTo("SERVICE_CENTRE");
        assertThat(result.getServiceCentre()).isTrue();
        assertThat(result.getCourt()).isNull();
        assertThat(result.getServiceCentreDetails()).isSameAs(serviceCentreDetails);
    }
}
