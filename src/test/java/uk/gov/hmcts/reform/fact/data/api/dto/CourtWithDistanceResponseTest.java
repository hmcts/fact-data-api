package uk.gov.hmcts.reform.fact.data.api.dto;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class CourtWithDistanceResponseTest {

    @Test
    void exposesValuesViaCourtWithDistanceInterfaceGetters() {
        UUID courtId = UUID.randomUUID();
        BigDecimal distance = new BigDecimal("12.34");

        CourtWithDistance courtWithDistance = CourtWithDistanceResponse.builder()
            .courtId(courtId)
            .courtName("Central Family Court")
            .courtSlug("central-family-court")
            .distance(distance)
            .build();

        assertThat(courtWithDistance.getCourtId()).isEqualTo(courtId);
        assertThat(courtWithDistance.getCourtName()).isEqualTo("Central Family Court");
        assertThat(courtWithDistance.getCourtSlug()).isEqualTo("central-family-court");
        assertThat(courtWithDistance.getDistance()).isEqualTo(distance);
    }

    @Test
    void exposesValuesViaNoArgsConstructorAndSetters() {
        UUID courtId = UUID.randomUUID();
        BigDecimal distance = new BigDecimal("1.50");

        CourtWithDistanceResponse response = new CourtWithDistanceResponse();
        response.setCourtId(courtId);
        response.setCourtName("Manchester Civil Justice Centre");
        response.setCourtSlug("manchester-civil-justice-centre");
        response.setDistance(distance);

        assertThat(response.getCourtId()).isEqualTo(courtId);
        assertThat(response.getCourtName()).isEqualTo("Manchester Civil Justice Centre");
        assertThat(response.getCourtSlug()).isEqualTo("manchester-civil-justice-centre");
        assertThat(response.getDistance()).isEqualTo(distance);
    }
}

