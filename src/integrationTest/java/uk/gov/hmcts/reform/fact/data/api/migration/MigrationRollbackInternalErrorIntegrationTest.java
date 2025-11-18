package uk.gov.hmcts.reform.fact.data.api.migration;

import static org.assertj.core.api.Assertions.assertThat;
import java.net.URI;
import java.util.Collections;
import java.util.List;
import org.mockito.Mockito;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import uk.gov.hmcts.reform.fact.data.api.services.RegionService;
import uk.gov.hmcts.reform.fact.data.api.entities.Court;
import uk.gov.hmcts.reform.fact.data.api.migration.client.LegacyFactClient;
import uk.gov.hmcts.reform.fact.data.api.migration.model.CourtDto;
import uk.gov.hmcts.reform.fact.data.api.migration.model.LegacyExportResponse;
import uk.gov.hmcts.reform.fact.data.api.migration.model.LocalAuthorityTypeDto;
import uk.gov.hmcts.reform.fact.data.api.migration.model.RegionDto;
import uk.gov.hmcts.reform.fact.data.api.repositories.CourtRepository;
import uk.gov.hmcts.reform.fact.data.api.repositories.RegionRepository;
import uk.gov.hmcts.reform.fact.data.api.services.CourtService;

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
@Import({
    MigrationRollbackInternalErrorIntegrationTest.StubConfiguration.class,
    MigrationRollbackInternalErrorIntegrationTest.FailingCourtServiceConfiguration.class
})
@ActiveProfiles("test")
class MigrationRollbackInternalErrorIntegrationTest {

    @LocalServerPort
    private int port;

    private final TestRestTemplate restTemplate;
    private final RegionRepository regionRepository;
    private final CourtRepository courtRepository;

    @Autowired
    MigrationRollbackInternalErrorIntegrationTest(
        TestRestTemplate restTemplate,
        RegionRepository regionRepository,
        CourtRepository courtRepository
    ) {
        this.restTemplate = restTemplate;
        this.regionRepository = regionRepository;
        this.courtRepository = courtRepository;
    }

    @Test
    void shouldRollbackAllChangesWhenUnexpectedErrorOccurs() {
        TableCounts before = captureTableCounts();
        ResponseEntity<String> response = restTemplate.postForEntity(migrationUrl(), null, String.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);

        assertThat(regionRepository.count()).isEqualTo(before.regions());
        assertThat(courtRepository.count()).isEqualTo(before.courts());
    }

    private URI migrationUrl() {
        return URI.create("http://localhost:" + port + "/migration/import");
    }

    private TableCounts captureTableCounts() {
        return new TableCounts(
            regionRepository.count(),
            courtRepository.count()
        );
    }

    private record TableCounts(long regions, long courts) {}

    static class StubConfiguration {

        @Bean
        @Primary
        LegacyFactClient successfulLegacyFactClient() {
            LegacyExportResponse response = new LegacyExportResponse(
                List.of(new CourtDto(
                    1L,
                    "Example Court",
                    "example-court",
                    true,
                    1,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    false
                )),
                Collections.emptyList(),
                Collections.emptyList(),
                Collections.emptyList(),
                Collections.emptyList(),
                Collections.emptyList(),
                Collections.emptyList(),
                List.of(new RegionDto(1, "Eastern", "England")),
                Collections.emptyList()
            );

            LegacyFactClient client = Mockito.mock(LegacyFactClient.class);
            Mockito.when(client.fetchExport()).thenReturn(response);
            return client;
        }
    }

    static class FailingCourtServiceConfiguration {

        @Bean
        @Primary
        CourtService failingCourtService(CourtRepository courtRepository, RegionService regionService) {
            return new CourtService(courtRepository, regionService) {
                @Override
                public Court createCourt(Court court) {
                    throw new RuntimeException("boom");
                }
            };
        }
    }
}
