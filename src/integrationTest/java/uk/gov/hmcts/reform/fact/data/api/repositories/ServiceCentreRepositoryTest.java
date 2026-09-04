package uk.gov.hmcts.reform.fact.data.api.repositories;

import io.qameta.allure.Feature;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import uk.gov.hmcts.reform.fact.data.api.audit.AuditUserContext;
import uk.gov.hmcts.reform.fact.data.api.entities.Region;
import uk.gov.hmcts.reform.fact.data.api.entities.ServiceCentre;
import uk.gov.hmcts.reform.fact.data.api.entities.types.CatchmentType;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@Feature("Service Centre Repository")
@DisplayName("Service Centre Repository")
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class ServiceCentreRepositoryTest {

    @Autowired
    private ServiceCentreRepository serviceCentreRepository;

    @Autowired
    private RegionRepository regionRepository;

    @Autowired
    private AuditUserContext auditUserContext;

    private UUID regionId;

    @BeforeEach
    void setUp() {
        auditUserContext.clear();
        auditUserContext.suppressAudit();

        regionId = regionRepository.save(Region.builder()
                                             .name("Service Centre Repository Region")
                                             .country("England")
                                             .build()).getId();
    }

    @AfterEach
    void tearDown() {
        auditUserContext.clear();
    }

    @Test
    void openServiceAreaQueriesExcludeClosedServiceCentres() {
        UUID serviceAreaId = UUID.randomUUID();

        ServiceCentre openNational = saveServiceCentre(
            "Open National Service Centre",
            true,
            CatchmentType.NATIONAL,
            serviceAreaId
        );
        ServiceCentre closedRegional = saveServiceCentre(
            "Closed Regional Service Centre",
            false,
            CatchmentType.REGIONAL,
            serviceAreaId
        );

        serviceCentreRepository.flush();

        assertThat(serviceCentreRepository.findByServiceAreaIdAndOpenTrue(serviceAreaId))
            .extracting(ServiceCentre::getId)
            .containsExactly(openNational.getId())
            .doesNotContain(closedRegional.getId());

        assertThat(serviceCentreRepository.existsByServiceAreaIdAndCatchmentTypeInAndOpenTrue(
            serviceAreaId,
            List.of(CatchmentType.NATIONAL)
        )).isTrue();

        assertThat(serviceCentreRepository.existsByServiceAreaIdAndCatchmentTypeInAndOpenTrue(
            serviceAreaId,
            List.of(CatchmentType.REGIONAL)
        )).isFalse();
    }

    private ServiceCentre saveServiceCentre(
        String name,
        boolean open,
        CatchmentType catchmentType,
        UUID serviceAreaId) {
        return serviceCentreRepository.save(
            ServiceCentre.builder()
                .name(name)
                .slug(UUID.randomUUID().toString())
                .open(open)
                .regionId(regionId)
                .catchmentType(catchmentType)
                .serviceAreaIds(List.of(serviceAreaId))
                .build()
        );
    }
}
