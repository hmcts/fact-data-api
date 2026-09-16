package uk.gov.hmcts.reform.fact.data.api.entities;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class CourtCounterServiceOpeningHoursTest {

    @Test
    void getCourtTypesForViewReturnsExpandedCourtTypesWhenPresent() {
        CourtType courtType = CourtType.builder().id(UUID.randomUUID()).name("County Court").build();
        CourtCounterServiceOpeningHours openingHours = new CourtCounterServiceOpeningHours();
        openingHours.setCourtTypeDetails(List.of(courtType));

        assertThat(openingHours.getCourtTypesForView()).containsExactly(courtType);
    }

    @Test
    void getCourtTypesForViewFallsBackToCourtTypeIds() {
        UUID courtTypeId = UUID.randomUUID();
        CourtCounterServiceOpeningHours openingHours = new CourtCounterServiceOpeningHours();
        openingHours.setCourtTypes(List.of(courtTypeId));

        assertThat(openingHours.getCourtTypesForView()).containsExactly(courtTypeId);
    }

    @Test
    void getCourtTypesForViewReturnsNullWhenNeitherDetailsNorIdsAreSet() {
        CourtCounterServiceOpeningHours openingHours = new CourtCounterServiceOpeningHours();

        assertThat(openingHours.getCourtTypesForView()).isNull();
    }
}

