package uk.gov.hmcts.reform.fact.data.api.entities;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class CourtOpeningHoursTest {

    @Test
    void getOpeningHourTypeForViewReturnsTransientDetails() {
        OpeningHourType details = OpeningHourType.builder()
            .id(UUID.randomUUID())
            .name("Counter")
            .nameCy("Cownter")
            .build();

        CourtOpeningHours openingHours = new CourtOpeningHours();
        openingHours.setOpeningHourTypeDetails(details);

        assertThat(openingHours.getOpeningHourTypeForView()).isEqualTo(details);
    }
}

