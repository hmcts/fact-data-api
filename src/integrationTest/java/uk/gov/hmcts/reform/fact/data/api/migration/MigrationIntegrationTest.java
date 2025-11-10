package uk.gov.hmcts.reform.fact.data.api.migration;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.net.URI;
import java.util.List;
import java.util.Objects;
import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.web.client.RestTemplate;
import uk.gov.hmcts.reform.fact.data.api.entities.Court;
import uk.gov.hmcts.reform.fact.data.api.entities.CourtProfessionalInformation;
import uk.gov.hmcts.reform.fact.data.api.entities.LocalAuthorityType;
import uk.gov.hmcts.reform.fact.data.api.entities.Region;
import uk.gov.hmcts.reform.fact.data.api.migration.entities.LegacyService;
import uk.gov.hmcts.reform.fact.data.api.migration.model.CourtDto;
import uk.gov.hmcts.reform.fact.data.api.migration.model.LegacyExportResponse;
import uk.gov.hmcts.reform.fact.data.api.migration.model.MigrationResponse;
import uk.gov.hmcts.reform.fact.data.api.migration.repository.LegacyCourtMappingRepository;
import uk.gov.hmcts.reform.fact.data.api.migration.repository.LegacyServiceRepository;
import uk.gov.hmcts.reform.fact.data.api.repositories.ContactDescriptionTypeRepository;
import uk.gov.hmcts.reform.fact.data.api.repositories.CourtAreasOfLawRepository;
import uk.gov.hmcts.reform.fact.data.api.repositories.CourtCodesRepository;
import uk.gov.hmcts.reform.fact.data.api.repositories.CourtDxCodeRepository;
import uk.gov.hmcts.reform.fact.data.api.repositories.CourtFaxRepository;
import uk.gov.hmcts.reform.fact.data.api.repositories.CourtLocalAuthoritiesRepository;
import uk.gov.hmcts.reform.fact.data.api.repositories.CourtProfessionalInformationRepository;
import uk.gov.hmcts.reform.fact.data.api.repositories.CourtRepository;
import uk.gov.hmcts.reform.fact.data.api.repositories.CourtServiceAreasRepository;
import uk.gov.hmcts.reform.fact.data.api.repositories.CourtSinglePointsOfEntryRepository;
import uk.gov.hmcts.reform.fact.data.api.repositories.CourtTypeRepository;
import uk.gov.hmcts.reform.fact.data.api.repositories.LocalAuthorityTypeRepository;
import uk.gov.hmcts.reform.fact.data.api.repositories.OpeningHourTypeRepository;
import uk.gov.hmcts.reform.fact.data.api.repositories.RegionRepository;
import uk.gov.hmcts.reform.fact.data.api.repositories.ServiceAreaRepository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * Full-stack integration tests that stand up the application, hit the migration endpoint over HTTP,
 * and hydrate a real Postgres instance (via Testcontainers) using data pulled from the configured
 * legacy FaCT API. The tests verify that the number of records written to each table matches the
 * payload returned by the legacy endpoint, and that we short-circuit when courts already exist.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class MigrationIntegrationTest {

    private static final String EXPORT_ENDPOINT = "/private-migration/export";

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
    private CourtProfessionalInformationRepository courtProfessionalInformationRepository;

    @Autowired
    private CourtDxCodeRepository courtDxCodeRepository;

    @Autowired
    private CourtFaxRepository courtFaxRepository;

    @Autowired
    private LegacyCourtMappingRepository legacyCourtMappingRepository;

    @Autowired
    private ObjectMapper objectMapper;

    @Value("${migration.source-base-url}")
    private String migrationSourceBaseUrl;

    private LegacyExportResponse legacySnapshot;

    @BeforeAll
    void fetchLegacySnapshot() throws IOException {
        assumeTrue(StringUtils.isNotBlank(migrationSourceBaseUrl), "migration.source-base-url must be configured");
        RestTemplate template = new RestTemplate();
        ResponseEntity<String> response = template.getForEntity(migrationSourceBaseUrl + EXPORT_ENDPOINT, String.class);
        assumeTrue(response.getStatusCode().is2xxSuccessful(), "Legacy FaCT API is unavailable");
        legacySnapshot = objectMapper.readValue(response.getBody(), LegacyExportResponse.class);
    }

    @BeforeEach
    void insertReferenceDataBeforeTest() {
        if (legacySnapshot == null || legacySnapshot.localAuthorityTypes() == null) {
            return;
        }
        legacySnapshot.localAuthorityTypes().forEach(type ->
            localAuthorityTypeRepository.save(
                LocalAuthorityType.builder()
                    .name(type.name())
                    .build()
            )
        );
    }

    @Test
    void shouldImportLegacyData() {
        assumeTrue(legacySnapshot != null, "Legacy export snapshot unavailable");

        ResponseEntity<MigrationResponse> response = restTemplate.postForEntity(
            migrationUrl(), null, MigrationResponse.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        MigrationResponse body = response.getBody();
        assertThat(body).isNotNull();
        assertThat(body.message()).isEqualTo("Migration completed successfully");

        int expectedCourts = safeSize(legacySnapshot.courts());
        int expectedRegions = safeSize(legacySnapshot.regions());
        int expectedAreasOfLaw = safeSize(legacySnapshot.areaOfLawTypes());
        int expectedServiceAreas = safeSize(legacySnapshot.serviceAreas());
        int expectedServices = safeSize(legacySnapshot.services());
        int expectedLocalAuthorityTypes = safeSize(legacySnapshot.localAuthorityTypes());
        int expectedContactDescriptionTypes = safeSize(legacySnapshot.contactDescriptionTypes());
        int expectedOpeningHourTypes = safeSize(legacySnapshot.openingHourTypes());
        int expectedCourtTypes = safeSize(legacySnapshot.courtTypes());
        long expectedCourtLocalAuthorities = countCourtLocalAuthorities();
        long expectedCourtServiceAreas = countCourtServiceAreas();
        long expectedCourtAreasOfLaw = countCourtAreasOfLaw();
        long expectedCourtSpocs = countCourtSinglePointsOfEntry();
        long expectedCourtProfessionalInformation = countCourtsWithProfessionalInformation();
        long expectedCourtDxCodes = countCourtDxCodes();
        long expectedCourtFax = countCourtFax();
        long expectedCourtCodes = countCourtCodes();

        assertThat(body.result().courtsMigrated()).isEqualTo(expectedCourts);
        assertThat(body.result().regionsMigrated()).isEqualTo(expectedRegions);
        assertThat(body.result().areaOfLawTypesMigrated()).isEqualTo(expectedAreasOfLaw);
        assertThat(body.result().serviceAreasMigrated()).isEqualTo(expectedServiceAreas);
        assertThat(body.result().servicesMigrated()).isEqualTo(expectedServices);
        assertThat(body.result().localAuthorityTypesMigrated()).isEqualTo(expectedLocalAuthorityTypes);
        assertThat(body.result().contactDescriptionTypesMigrated()).isEqualTo(expectedContactDescriptionTypes);
        assertThat(body.result().openingHourTypesMigrated()).isEqualTo(expectedOpeningHourTypes);
        assertThat(body.result().courtTypesMigrated()).isEqualTo(expectedCourtTypes);
        assertThat(body.result().courtLocalAuthoritiesMigrated()).isEqualTo(expectedCourtLocalAuthorities);
        assertThat(body.result().courtProfessionalInformationMigrated())
            .isEqualTo(expectedCourtProfessionalInformation);

        assertThat(regionRepository.count()).isEqualTo(expectedRegions);
        assertThat(legacyServiceRepository.count()).isEqualTo(expectedServices);
        assertThat(serviceAreaRepository.count()).isEqualTo(expectedServiceAreas);
        assertThat(localAuthorityTypeRepository.count()).isEqualTo(expectedLocalAuthorityTypes);
        assertThat(contactDescriptionTypeRepository.count()).isEqualTo(expectedContactDescriptionTypes);
        assertThat(openingHourTypeRepository.count()).isEqualTo(expectedOpeningHourTypes);
        assertThat(courtTypeRepository.count()).isEqualTo(expectedCourtTypes);
        assertThat(courtRepository.count()).isEqualTo(expectedCourts);
        assertThat(courtServiceAreasRepository.count()).isEqualTo(expectedCourtServiceAreas);
        assertThat(courtAreasOfLawRepository.count()).isEqualTo(expectedCourtAreasOfLaw);
        assertThat(courtSinglePointsOfEntryRepository.count()).isEqualTo(expectedCourtSpocs);
        assertThat(courtLocalAuthoritiesRepository.count()).isEqualTo(expectedCourtLocalAuthorities);
        assertThat(courtCodesRepository.count()).isEqualTo(expectedCourtCodes);
        assertThat(courtProfessionalInformationRepository.count()).isEqualTo(expectedCourtProfessionalInformation);
        assertThat(courtDxCodeRepository.count()).isEqualTo(expectedCourtDxCodes);
        assertThat(courtFaxRepository.count()).isEqualTo(expectedCourtFax);
        assertThat(legacyCourtMappingRepository.count()).isEqualTo(expectedCourts);

        LegacyService service = legacyServiceRepository.findAll().get(0);
        assertThat(service.getName()).isNotBlank();
        Court court = courtRepository.findAll().get(0);
        CourtProfessionalInformation professionalInformation = courtProfessionalInformationRepository.findAll().get(0);
        assertThat(professionalInformation.getCourtId()).isEqualTo(court.getId());
    }

    @Test
    void shouldNotInvokeLegacyEndpointWhenDataAlreadyPresent() {
        Region region = regionRepository.save(
            Region.builder()
                .name("Existing Region")
                .country("England")
                .build()
        );
        courtRepository.save(
            Court.builder()
                .name("Existing Court")
                .slug("existing-court")
                .open(true)
                .regionId(region.getId())
                .isServiceCentre(false)
                .build()
        );

        assertThatThrownBy(() -> restTemplate.postForEntity(migrationUrl(), null, String.class))
            .hasMessageContaining("409");
    }

    private URI migrationUrl() {
        return URI.create("http://localhost:" + port + "/migration/import");
    }

    private int safeSize(List<?> values) {
        return values == null ? 0 : values.size();
    }

    private long countCourtServiceAreas() {
        return legacySnapshot.courts().stream()
            .map(CourtDto::courtServiceAreas)
            .filter(Objects::nonNull)
            .mapToLong(List::size)
            .sum();
    }

    private long countCourtLocalAuthorities() {
        return legacySnapshot.courts().stream()
            .map(CourtDto::courtLocalAuthorities)
            .filter(Objects::nonNull)
            .mapToLong(List::size)
            .sum();
    }

    private long countCourtAreasOfLaw() {
        return legacySnapshot.courts().stream()
            .map(CourtDto::courtAreasOfLaw)
            .filter(Objects::nonNull)
            .filter(aol -> aol.areaOfLawIds() != null && !aol.areaOfLawIds().isEmpty())
            .count();
    }

    private long countCourtSinglePointsOfEntry() {
        return legacySnapshot.courts().stream()
            .map(CourtDto::courtSinglePointsOfEntry)
            .filter(Objects::nonNull)
            .filter(spoe -> spoe.areaOfLawIds() != null && !spoe.areaOfLawIds().isEmpty())
            .count();
    }

    private long countCourtsWithProfessionalInformation() {
        return legacySnapshot.courts().stream()
            .map(CourtDto::courtProfessionalInformation)
            .filter(Objects::nonNull)
            .count();
    }

    private long countCourtDxCodes() {
        return legacySnapshot.courts().stream()
            .map(CourtDto::courtDxCodes)
            .filter(Objects::nonNull)
            .mapToLong(List::size)
            .sum();
    }

    private long countCourtFax() {
        return legacySnapshot.courts().stream()
            .map(CourtDto::courtFax)
            .filter(Objects::nonNull)
            .mapToLong(List::size)
            .sum();
    }

    private long countCourtCodes() {
        return legacySnapshot.courts().stream()
            .map(CourtDto::courtCodes)
            .filter(Objects::nonNull)
            .count();
    }
}
