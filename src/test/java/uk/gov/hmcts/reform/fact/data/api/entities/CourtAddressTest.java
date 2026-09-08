package uk.gov.hmcts.reform.fact.data.api.entities;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class CourtAddressTest {

    @Test
    void viewAccessorsReturnExpandedDetailsWhenAvailable() {
        AreaOfLawType area = AreaOfLawType.builder().id(UUID.randomUUID()).name("Family").nameCy("Teulu").build();
        CourtType courtType = CourtType.builder().id(UUID.randomUUID()).name("County Court").build();

        CourtAddress address = new CourtAddress();
        address.setAreasOfLawDetails(List.of(area));
        address.setCourtTypeDetails(List.of(courtType));

        assertThat(address.getAreasOfLawForView()).containsExactly(area);
        assertThat(address.getCourtTypesForView()).containsExactly(courtType);
    }

    @Test
    void viewAccessorsFallBackToIdsWhenExpandedDetailsAreMissing() {
        UUID areaId = UUID.randomUUID();
        UUID courtTypeId = UUID.randomUUID();

        CourtAddress address = new CourtAddress();
        address.setAreasOfLaw(List.of(areaId));
        address.setCourtTypes(List.of(courtTypeId));

        assertThat(address.getAreasOfLawForView()).containsExactly(areaId);
        assertThat(address.getCourtTypesForView()).containsExactly(courtTypeId);
    }
}

