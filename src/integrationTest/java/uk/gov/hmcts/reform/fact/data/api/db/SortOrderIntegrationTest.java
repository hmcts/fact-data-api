package uk.gov.hmcts.reform.fact.data.api.db;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import uk.gov.hmcts.reform.fact.data.api.entities.Service;
import uk.gov.hmcts.reform.fact.data.api.entities.ServiceArea;
import uk.gov.hmcts.reform.fact.data.api.repositories.ServiceAreaRepository;
import uk.gov.hmcts.reform.fact.data.api.repositories.ServiceRepository;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
class SortOrderIntegrationTest {

    @Autowired
    private ServiceRepository serviceRepository;

    @Autowired
    private ServiceAreaRepository serviceAreaRepository;

    @Test
    @DisplayName("Verify Service sort order is correct")
    void verifyServiceSortOrder() {
        final List<Service> services = serviceRepository.findAllByOrderBySortOrderAsc();

        assertThat(services).hasSizeGreaterThanOrEqualTo(7);

        assertThat(services.get(0).getName()).isEqualTo("Money");
        assertThat(services.get(1).getName()).isEqualTo("Probate, divorce or ending civil partnerships");
        assertThat(services.get(2).getName()).isEqualTo("Childcare and parenting");
        assertThat(services.get(3).getName()).isEqualTo("Harm and abuse");
        assertThat(services.get(4).getName()).isEqualTo("Immigration and asylum");
        assertThat(services.get(5).getName()).isEqualTo("Crime");
        assertThat(services.get(6).getName()).isEqualTo("High Court district registries");
    }

    @Test
    @DisplayName("Verify ServiceArea sort order is correct for Money")
    void verifyServiceAreaSortOrderMoney() {
        final List<ServiceArea> areas = serviceAreaRepository.findAllByServiceName("Money");

        // The sequence defined in V1.20 for areas in 'Money' service
        // 'Money claims' (1), 'Probate' (2), 'Housing' (3), 'Bankruptcy' (4),
        // 'Benefits' (5), 'Claims against employers' (6), 'Tax' (7)

        assertThat(areas).extracting(ServiceArea::getName)
            .containsSubsequence(
                "Money claims",
                "Probate",
                "Housing",
                "Bankruptcy",
                "Benefits",
                "Claims against employers",
                "Tax");
    }

    @Test
    @DisplayName("Verify ServiceArea sort order is correct for Probate, divorce or ending civil partnerships")
    void testVerifyServiceAreaSortOrderFamily() {
        final List<ServiceArea> areas =
            serviceAreaRepository.findAllByServiceName("Probate, divorce or ending civil partnerships");

        // 'Probate' (2), 'Divorce' (9), 'Civil partnership' (10), 'Financial remedy' (11)
        assertThat(areas).extracting(ServiceArea::getName)
            .containsSubsequence(
                "Probate",
                "Divorce",
                "Civil partnership",
                "Financial remedy");
    }
}
