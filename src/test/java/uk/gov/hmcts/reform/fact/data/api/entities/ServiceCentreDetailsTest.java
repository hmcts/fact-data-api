package uk.gov.hmcts.reform.fact.data.api.entities;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class ServiceCentreDetailsTest {

    @Test
    void getServiceAreasForViewReturnsExpandedDetailsWhenPresent() {
        ServiceArea serviceArea = ServiceArea.builder()
            .id(UUID.randomUUID())
            .name("Family")
            .nameCy("Teulu")
            .areaOfLawId(UUID.randomUUID())
            .build();

        ServiceCentreDetails details = ServiceCentreDetails.builder().build();
        details.setServiceAreaDetails(List.of(serviceArea));

        assertThat(details.getServiceAreasForView()).containsExactly(serviceArea);
    }

    @Test
    void getServiceAreasForViewFallsBackToServiceAreaIds() {
        UUID areaId = UUID.randomUUID();
        ServiceCentreDetails details = ServiceCentreDetails.builder().serviceAreaIds(List.of(areaId)).build();

        assertThat(details.getServiceAreasForView()).containsExactly(areaId);
    }

    @Test
    void getServiceAreasForViewReturnsNullWhenNoDetailsOrIdsExist() {
        ServiceCentreDetails details = ServiceCentreDetails.builder().build();

        assertThat(details.getServiceAreasForView()).isNull();
    }
}

