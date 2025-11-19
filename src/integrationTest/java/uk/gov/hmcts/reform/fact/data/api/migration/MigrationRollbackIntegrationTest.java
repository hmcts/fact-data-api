package uk.gov.hmcts.reform.fact.data.api.migration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.net.URI;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.http.HttpStatus;
import org.springframework.test.annotation.DirtiesContext;
import uk.gov.hmcts.reform.fact.data.api.migration.client.LegacyFactClient;
import uk.gov.hmcts.reform.fact.data.api.migration.model.AreaOfLawTypeDto;
import uk.gov.hmcts.reform.fact.data.api.migration.model.ContactDescriptionTypeDto;
import uk.gov.hmcts.reform.fact.data.api.migration.model.CourtDto;
import uk.gov.hmcts.reform.fact.data.api.migration.model.CourtServiceAreaDto;
import uk.gov.hmcts.reform.fact.data.api.migration.model.CourtTypeDto;
import uk.gov.hmcts.reform.fact.data.api.migration.model.LegacyExportResponse;
import uk.gov.hmcts.reform.fact.data.api.migration.model.LocalAuthorityTypeDto;
import uk.gov.hmcts.reform.fact.data.api.migration.model.OpeningHourTypeDto;
import uk.gov.hmcts.reform.fact.data.api.migration.model.RegionDto;
import uk.gov.hmcts.reform.fact.data.api.migration.model.ServiceAreaDto;
import uk.gov.hmcts.reform.fact.data.api.migration.model.ServiceDto;
import uk.gov.hmcts.reform.fact.data.api.repositories.CourtRepository;
import uk.gov.hmcts.reform.fact.data.api.repositories.RegionRepository;
import uk.gov.hmcts.reform.fact.data.api.repositories.ServiceAreaRepository;

/**
 * Exercises the rollback path by stubbing the legacy export client to return data that causes a
 * persistence failure (a region with a blank name). The Spring transaction should roll back every
 * insert so the database remains empty.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestPropertySource(properties = {
    "spring.cloud.azure.profile.tenant-id=test-tenant",
    "spring.cloud.azure.credential.managed-identity-enabled=false",
    "spring.cloud.azure.credential.client-id=test-client",
    "spring.cloud.azure.storage.account-name=test-account",
    "spring.cloud.azure.storage.blob.connection-string=",
    "spring.cloud.azure.storage.blob.container-name=test-container"
})
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@Import(MigrationRollbackIntegrationTest.StubConfiguration.class)
@ActiveProfiles("test")
class MigrationRollbackIntegrationTest {

    @LocalServerPort
    private int port;

    private final TestRestTemplate restTemplate;
    private final RegionRepository regionRepository;
    private final ServiceAreaRepository serviceAreaRepository;
    private final CourtRepository courtRepository;

    @Autowired
    MigrationRollbackIntegrationTest(
        TestRestTemplate restTemplate,
        RegionRepository regionRepository,
        ServiceAreaRepository serviceAreaRepository,
        CourtRepository courtRepository
    ) {
        this.restTemplate = restTemplate;
        this.regionRepository = regionRepository;
        this.serviceAreaRepository = serviceAreaRepository;
        this.courtRepository = courtRepository;
    }

    /**
     * The region in the stub response has a null name, which violates the Region entity constraint
     * and causes the persistence transaction to throw. We expect the API to return 500 and the DB
     * to contain no migrated data.
     */
    @Test
    void shouldRollbackAllChangesWhenPersistenceFails() {
        TableCounts before = captureTableCounts();
        ResponseEntity<String> response = restTemplate.postForEntity(migrationUrl(), null, String.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);

        assertThat(regionRepository.count()).isEqualTo(before.regions());
        assertThat(serviceAreaRepository.count()).isEqualTo(before.serviceAreas());
        assertThat(courtRepository.count()).isEqualTo(before.courts());
    }

    private URI migrationUrl() {
        return URI.create("http://localhost:" + port + "/migration/import");
    }

    private TableCounts captureTableCounts() {
        return new TableCounts(
            regionRepository.count(),
            serviceAreaRepository.count(),
            courtRepository.count()
        );
    }

    private record TableCounts(long regions, long serviceAreas, long courts) {}

    /**
     * Provides a mocked {@link LegacyFactClient} so we can control the payload without editing the
     * production client code. Marked @Primary so it overrides the real bean inside this test slice.
     */
    static class StubConfiguration {

        @Bean
        @Primary
        LegacyFactClient failingLegacyFactClient() {
            LegacyExportResponse response = new LegacyExportResponse(
                List.of(buildLegacyCourt()),
                List.of(new LocalAuthorityTypeDto(1, "LA")),
                List.of(new ServiceAreaDto(
                    1,
                    "Service Area",
                    "Ardal",
                    "desc",
                    "desc cy",
                    "http://example.com",
                    "online",
                    "ar-lein",
                    "CIVIL",
                    "text",
                    "testun",
                    "POSTCODE",
                    1
                )),
                List.of(new ServiceDto(1, "Service", "Gwasanaeth", "desc", "disgrifiad", List.of(1))),
                List.of(new ContactDescriptionTypeDto(1, "CDT", "CDT CY")),
                List.of(new OpeningHourTypeDto(1, "OH", "OH CY")),
                List.of(new CourtTypeDto(1, "Type")),
                List.of(new RegionDto(1, null, "England")),
                List.of(new AreaOfLawTypeDto(1, "Area", "Area CY"))
            );

            LegacyFactClient client = mock(LegacyFactClient.class);
            when(client.fetchExport()).thenReturn(response);
            return client;
        }

        private CourtDto buildLegacyCourt() {
            CourtDto dto = new CourtDto();
            dto.setId(123L);
            dto.setName("Court");
            dto.setSlug("court");
            dto.setOpen(true);
            dto.setRegionId(1);
            dto.setCourtServiceAreas(List.of(new CourtServiceAreaDto(1, "LOCAL", List.of(1))));
            dto.setIsServiceCentre(false);
            return dto;
        }
    }
}
