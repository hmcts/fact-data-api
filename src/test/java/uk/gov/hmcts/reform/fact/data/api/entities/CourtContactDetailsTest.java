package uk.gov.hmcts.reform.fact.data.api.entities;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class CourtContactDetailsTest {

    @Test
    void getCourtContactDescriptionForViewReturnsTransientDetails() {
        ContactDescriptionType details = ContactDescriptionType.builder()
            .id(UUID.randomUUID())
            .name("Email")
            .nameCy("E-bost")
            .build();

        CourtContactDetails courtContactDetails = new CourtContactDetails();
        courtContactDetails.setCourtContactDescriptionDetails(details);

        assertThat(courtContactDetails.getCourtContactDescriptionForView()).isEqualTo(details);
    }
}

