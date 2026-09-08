package uk.gov.hmcts.reform.fact.data.api.entities;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class CourtAreasOfLawTest {

    @Test
    void getAreasOfLawForViewReturnsExpandedDetailsWhenPresent() {
        AreaOfLawType areaOfLaw = AreaOfLawType.builder()
            .id(UUID.randomUUID())
            .name("Family")
            .nameCy("Teulu")
            .build();

        CourtAreasOfLaw courtAreasOfLaw = new CourtAreasOfLaw();
        courtAreasOfLaw.setAreasOfLawDetails(List.of(areaOfLaw));

        assertThat(courtAreasOfLaw.getAreasOfLawForView()).containsExactly(areaOfLaw);
    }

    @Test
    void getAreasOfLawForViewFallsBackToAreaIds() {
        UUID areaOfLawId = UUID.randomUUID();
        CourtAreasOfLaw courtAreasOfLaw = new CourtAreasOfLaw();
        courtAreasOfLaw.setAreasOfLaw(List.of(areaOfLawId));

        assertThat(courtAreasOfLaw.getAreasOfLawForView()).containsExactly(areaOfLawId);
    }
}

