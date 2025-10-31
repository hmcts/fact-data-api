package uk.gov.hmcts.reform.fact.data.api.migration;

import com.github.tomakehurst.wiremock.WireMockServer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import uk.gov.hmcts.reform.fact.data.api.entities.Court;
import uk.gov.hmcts.reform.fact.data.api.migration.entities.LegacyService;
import uk.gov.hmcts.reform.fact.data.api.entities.Region;
import uk.gov.hmcts.reform.fact.data.api.migration.model.MigrationResponse;
import uk.gov.hmcts.reform.fact.data.api.repositories.ContactDescriptionTypeRepository;
import uk.gov.hmcts.reform.fact.data.api.repositories.CourtAreasOfLawRepository;
import uk.gov.hmcts.reform.fact.data.api.repositories.CourtCodesRepository;
import uk.gov.hmcts.reform.fact.data.api.repositories.CourtDxCodeRepository;
import uk.gov.hmcts.reform.fact.data.api.repositories.CourtFaxRepository;
import uk.gov.hmcts.reform.fact.data.api.repositories.CourtPostcodeRepository;
import uk.gov.hmcts.reform.fact.data.api.repositories.CourtRepository;
import uk.gov.hmcts.reform.fact.data.api.repositories.CourtServiceAreasRepository;
import uk.gov.hmcts.reform.fact.data.api.repositories.CourtSinglePointsOfEntryRepository;
import uk.gov.hmcts.reform.fact.data.api.repositories.CourtTypeRepository;
import uk.gov.hmcts.reform.fact.data.api.repositories.CourtLocalAuthoritiesRepository;
import uk.gov.hmcts.reform.fact.data.api.repositories.LocalAuthorityTypeRepository;
import uk.gov.hmcts.reform.fact.data.api.repositories.OpeningHourTypeRepository;
import uk.gov.hmcts.reform.fact.data.api.migration.repository.LegacyServiceRepository;
import uk.gov.hmcts.reform.fact.data.api.repositories.RegionRepository;
import uk.gov.hmcts.reform.fact.data.api.repositories.ServiceAreaRepository;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.http.HttpMethod.POST;

/**
 * Full-stack integration tests that exercise the migration controller against a real Postgres
 * instance (via Testcontainers) whilst stubbing out the legacy FaCT API with WireMock. The tests
 * confirm we persist the expected data, roll back on error, and avoid calling the legacy service
 * when the migration was already executed.
 */
@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class MigrationIntegrationTest {

    private static final String EXPORT_ENDPOINT = "/private-migration/export";

    @Container
    private static final PostgreSQLContainer<?> POSTGRES =
        new PostgreSQLContainer<>("postgres:16.4-alpine");

    private static final WireMockServer WIRE_MOCK = new WireMockServer(wireMockConfig().dynamicPort());

    @DynamicPropertySource
    static void registerProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
        registry.add("spring.datasource.driver-class-name", POSTGRES::getDriverClassName);
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "none");
        registry.add("dbMigration.runOnStartup", () -> "true");
        registry.add("migration.source-base-url", () -> WIRE_MOCK.baseUrl());
    }

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private RegionRepository regionRepository;

    @Autowired
    private LegacyServiceRepository legacyServiceRepository;

    @Autowired
    private ServiceAreaRepository serviceAreaRepository;

    @Autowired
    private LocalAuthorityTypeRepository localAuthorityTypeRepository;

    @Autowired
    private ContactDescriptionTypeRepository contactDescriptionTypeRepository;

    @Autowired
    private OpeningHourTypeRepository openingHourTypeRepository;

    @Autowired
    private CourtTypeRepository courtTypeRepository;

    @Autowired
    private CourtRepository courtRepository;

    @Autowired
    private CourtServiceAreasRepository courtServiceAreasRepository;

    @Autowired
    private CourtAreasOfLawRepository courtAreasOfLawRepository;

    @Autowired
    private CourtSinglePointsOfEntryRepository courtSinglePointsOfEntryRepository;

    @Autowired
    private CourtLocalAuthoritiesRepository courtLocalAuthoritiesRepository;

    @Autowired
    private CourtCodesRepository courtCodesRepository;

    @Autowired
    private CourtPostcodeRepository courtPostcodeRepository;

    @Autowired
    private CourtDxCodeRepository courtDxCodeRepository;

    @Autowired
    private CourtFaxRepository courtFaxRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeAll
    void startWireMock() {
        WIRE_MOCK.start();
    }

    @AfterAll
    void stopWireMock() {
        WIRE_MOCK.stop();
    }

    @AfterEach
    void cleanupDatabase() {
        WIRE_MOCK.resetAll();
        jdbcTemplate.execute(
            "TRUNCATE TABLE court_dxcodes, court_fax, court_postcodes, court_service_areas, "
                + "court_areas_of_law, court_single_points_of_entry, court_codes, court, "
                + "service_area, \"service\", contact_description_types, opening_hour_types, "
                + "court_types, local_authority_types, area_of_law_types, region, migration_audit "
                + "RESTART IDENTITY CASCADE"
        );
    }

    @BeforeEach
    void insertReferenceDataBeforeTest() {
        insertReferenceData();
    }

    private void insertReferenceData() {
        jdbcTemplate.update(
            "INSERT INTO local_authority_types (id, name) VALUES (?, ?)",
            UUID.randomUUID(),
            "Local Authority"
        );
    }

    @Test
    void shouldImportLegacyData() throws IOException {
        stubLegacyExport("/migration/legacy-export-success.json", HttpStatus.OK);

        ResponseEntity<MigrationResponse> response = restTemplate.postForEntity(
            migrationUrl(), null, MigrationResponse.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        MigrationResponse responseBody = response.getBody();
        assertThat(responseBody).isNotNull();
        assertThat(responseBody.message()).isEqualTo("Migration completed successfully");
        assertThat(responseBody.result().courtsMigrated()).isEqualTo(1);
        assertThat(responseBody.result().regionsMigrated()).isEqualTo(1);
        assertThat(responseBody.skippedCourtPostcodes()).isEmpty();

        WIRE_MOCK.verify(1, getRequestedFor(urlEqualTo(EXPORT_ENDPOINT)));

        assertThat(regionRepository.count()).isEqualTo(1);
        assertThat(legacyServiceRepository.count()).isEqualTo(1);
        assertThat(serviceAreaRepository.count()).isEqualTo(1);
        assertThat(localAuthorityTypeRepository.count()).isEqualTo(1);
        assertThat(contactDescriptionTypeRepository.count()).isEqualTo(1);
        assertThat(openingHourTypeRepository.count()).isEqualTo(1);
        assertThat(courtTypeRepository.count()).isEqualTo(1);
        assertThat(courtRepository.count()).isEqualTo(1);
        assertThat(courtServiceAreasRepository.count()).isEqualTo(1);
        assertThat(courtAreasOfLawRepository.count()).isEqualTo(1);
        assertThat(courtSinglePointsOfEntryRepository.count()).isEqualTo(1);
        assertThat(courtLocalAuthoritiesRepository.count()).isEqualTo(1);
        assertThat(courtCodesRepository.count()).isEqualTo(1);
        assertThat(courtPostcodeRepository.count()).isEqualTo(1);
        assertThat(courtDxCodeRepository.count()).isEqualTo(1);
        assertThat(courtFaxRepository.count()).isEqualTo(1);

        Integer auditCount = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM migration_audit", Integer.class);
        assertThat(auditCount).isEqualTo(1);
        String auditStatus = jdbcTemplate.queryForObject("SELECT status FROM migration_audit LIMIT 1", String.class);
        assertThat(auditStatus).isEqualTo("SUCCESS");

        Region region = regionRepository.findAll().get(0);
        assertThat(region.getName()).isEqualTo("Midlands");
        assertThat(region.getCountry()).isEqualTo("England");

        LegacyService service = legacyServiceRepository.findAll().get(0);
        assertThat(service.getName()).isEqualTo("Civil Service");
        assertThat(service.getNameCy()).isEqualTo("Gwasanaeth Sifil");
        assertThat(service.getServiceAreas()).isNotNull();
        assertThat(service.getServiceAreas()).hasSize(1);

        Court court = courtRepository.findAll().get(0);
        assertThat(court.getSlug()).isEqualTo("example-court");
        assertThat(court.getIsServiceCentre()).isTrue();
    }

    @Test
    void shouldRollbackWhenPersistenceFails() throws IOException {
        stubLegacyExport("/migration/legacy-export-invalid-region.json", HttpStatus.OK);

        assertThatThrownBy(() -> restTemplate.exchange(migrationUrl(), POST, null, String.class))
            .hasMessageContaining("500");

        assertThat(regionRepository.count()).isZero();
        assertThat(legacyServiceRepository.count()).isZero();
        assertThat(serviceAreaRepository.count()).isZero();
        assertThat(localAuthorityTypeRepository.count()).isEqualTo(1);
        assertThat(contactDescriptionTypeRepository.count()).isZero();
        assertThat(openingHourTypeRepository.count()).isZero();
        assertThat(courtTypeRepository.count()).isZero();
        assertThat(courtRepository.count()).isZero();
        assertThat(courtServiceAreasRepository.count()).isZero();
        assertThat(courtAreasOfLawRepository.count()).isZero();
        assertThat(courtSinglePointsOfEntryRepository.count()).isZero();
        assertThat(courtLocalAuthoritiesRepository.count()).isZero();
        assertThat(courtCodesRepository.count()).isZero();
        assertThat(courtPostcodeRepository.count()).isZero();
        assertThat(courtDxCodeRepository.count()).isZero();
        assertThat(courtFaxRepository.count()).isZero();
        assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM migration_audit", Integer.class)).isZero();
    }

    @Test
    void shouldReportSkippedPostcodesInResponse() throws IOException {
        stubLegacyExport("/migration/legacy-export-invalid-postcodes.json", HttpStatus.OK);

        ResponseEntity<MigrationResponse> response = restTemplate.postForEntity(
            migrationUrl(), null, MigrationResponse.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        MigrationResponse body = response.getBody();
        assertThat(body).isNotNull();
        assertThat(body.skippedCourtPostcodes()).hasSize(2);
        assertThat(body.skippedCourtPostcodes()).anySatisfy(s -> assertThat(s).contains("SNB 6DB"));
        assertThat(body.skippedCourtPostcodes()).anySatisfy(s -> assertThat(s).contains("ME132 0PJ"));
        assertThat(courtPostcodeRepository.count()).isZero();

        Integer auditCount = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM migration_audit", Integer.class);
        assertThat(auditCount).isEqualTo(2);
        List<Map<String, Object>> auditRows = jdbcTemplate.queryForList("SELECT status, details FROM migration_audit");
        assertThat(auditRows).anySatisfy(row -> {
            assertThat(row.get("status")).isEqualTo("FAILURE");
            assertThat(row.get("details").toString()).contains("SNB 6DB");
        });
        assertThat(auditRows).anySatisfy(row -> {
            assertThat(row.get("status")).isEqualTo("FAILURE");
            assertThat(row.get("details").toString()).contains("ME132 0PJ");
        });
    }

    @Test
    void shouldNotInvokeLegacyEndpointWhenDataAlreadyPresent() throws IOException {
        regionRepository.save(uk.gov.hmcts.reform.fact.data.api.entities.Region.builder()
                                  .name("Existing Region")
                                  .country("England")
                                  .build());

        stubLegacyExport("/migration/legacy-export-success.json", HttpStatus.OK);

        assertThatThrownBy(() -> restTemplate.exchange(migrationUrl(), POST, null, String.class))
            .hasMessageContaining("409");

        WIRE_MOCK.verify(0, getRequestedFor(urlEqualTo(EXPORT_ENDPOINT)));
    }

    private void stubLegacyExport(String resourcePath, HttpStatus status) throws IOException {
        byte[] body = readResource(resourcePath);
        WIRE_MOCK.stubFor(
            get(urlEqualTo(EXPORT_ENDPOINT))
                .willReturn(
                    aResponse()
                        .withStatus(status.value())
                        .withHeader("Content-Type", "application/json")
                        .withBody(body)
                )
        );
    }

    private URI migrationUrl() {
        return URI.create("http://localhost:" + port + "/migration/import");
    }

    private byte[] readResource(String resourcePath) throws IOException {
        try (InputStream stream = getClass().getResourceAsStream(resourcePath)) {
            if (stream == null) {
                throw new IOException("Unable to load resource: " + resourcePath);
            }
            return stream.readAllBytes();
        }
    }
}
