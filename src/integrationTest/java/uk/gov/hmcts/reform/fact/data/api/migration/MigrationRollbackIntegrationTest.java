package uk.gov.hmcts.reform.fact.data.api.migration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
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
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.http.HttpStatus;
import org.springframework.test.annotation.DirtiesContext;
import uk.gov.hmcts.reform.fact.data.api.entities.Court;
import uk.gov.hmcts.reform.fact.data.api.entities.Region;
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

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@Import(MigrationRollbackIntegrationTest.StubConfiguration.class)
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

    @Test
    void shouldRollbackAllChangesWhenPersistenceFails() {
        assertThatThrownBy(() -> restTemplate.postForEntity(migrationUrl(), null, String.class))
            .hasMessageContaining(String.valueOf(HttpStatus.INTERNAL_SERVER_ERROR.value()));

        assertThat(regionRepository.count()).isZero();
        assertThat(serviceAreaRepository.count()).isZero();
        assertThat(courtRepository.count()).isZero();
    }

    private URI migrationUrl() {
        return URI.create("http://localhost:" + port + "/migration/import");
    }

    static class StubConfiguration {

        @Bean
        @Primary
        LegacyFactClient failingLegacyFactClient() {
            LegacyExportResponse response = new LegacyExportResponse(
                List.of(new CourtDto(
                    "legacy-court",
                    "Court",
                    "court",
                    true,
                    1,
                    List.of(new CourtServiceAreaDto(1, "LOCAL", List.of(1))),
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
                List.of(new ServiceDto(1, "Service", "Gwasanaeth", "desc", "disgrifiad")),
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
    }
}
