package uk.gov.hmcts.reform.fact.data.api.migration;

import feign.FeignException;
import java.net.URI;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.boot.context.properties.ConfigurationPropertiesBindException;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import uk.gov.hmcts.reform.fact.data.api.entities.Court;
import uk.gov.hmcts.reform.fact.data.api.entities.CourtProfessionalInformation;
import uk.gov.hmcts.reform.fact.data.api.entities.LocalAuthorityType;
import uk.gov.hmcts.reform.fact.data.api.entities.validation.ValidationConstants;
import uk.gov.hmcts.reform.fact.data.api.migration.client.LegacyFactClient;
import uk.gov.hmcts.reform.fact.data.api.migration.entities.LegacyService;
import uk.gov.hmcts.reform.fact.data.api.migration.entities.MigrationAudit;
import uk.gov.hmcts.reform.fact.data.api.migration.entities.MigrationStatus;
import uk.gov.hmcts.reform.fact.data.api.migration.exception.MigrationClientException;
import uk.gov.hmcts.reform.fact.data.api.migration.model.AreaOfLawTypeDto;
import uk.gov.hmcts.reform.fact.data.api.migration.model.CourtAreasOfLawDto;
import uk.gov.hmcts.reform.fact.data.api.migration.model.CourtDto;
import uk.gov.hmcts.reform.fact.data.api.migration.model.CourtDxCodeDto;
import uk.gov.hmcts.reform.fact.data.api.migration.model.CourtLocalAuthorityDto;
import uk.gov.hmcts.reform.fact.data.api.migration.model.CourtSinglePointOfEntryDto;
import uk.gov.hmcts.reform.fact.data.api.migration.model.LocalAuthorityTypeDto;
import uk.gov.hmcts.reform.fact.data.api.migration.model.LegacyExportResponse;
import uk.gov.hmcts.reform.fact.data.api.migration.model.MigrationResponse;
import uk.gov.hmcts.reform.fact.data.api.migration.model.RegionDto;
import uk.gov.hmcts.reform.fact.data.api.migration.repository.LegacyCourtMappingRepository;
import uk.gov.hmcts.reform.fact.data.api.migration.repository.LegacyServiceRepository;
import uk.gov.hmcts.reform.fact.data.api.migration.repository.MigrationAuditRepository;
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
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Full-stack integration tests that boot the entire Spring Boot application (embedded Tomcat plus
 * Testcontainers-backed Postgres), call the new /migration/import endpoint over HTTP, and compare
 * the results with the payload retrieved from the legacy FaCT API. No external instance of this
 * service needs to be running for the tests to pass.
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
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@ActiveProfiles("test")
class MigrationIntegrationTest {

    @LocalServerPort
    private int port;

    private final TestRestTemplate restTemplate;
    private final RegionRepository regionRepository;
    private final LegacyServiceRepository legacyServiceRepository;
    private final ServiceAreaRepository serviceAreaRepository;
    private final LocalAuthorityTypeRepository localAuthorityTypeRepository;
    private final ContactDescriptionTypeRepository contactDescriptionTypeRepository;
    private final OpeningHourTypeRepository openingHourTypeRepository;
    private final CourtTypeRepository courtTypeRepository;
    private final CourtRepository courtRepository;
    private final CourtServiceAreasRepository courtServiceAreasRepository;
    private final CourtAreasOfLawRepository courtAreasOfLawRepository;
    private final CourtSinglePointsOfEntryRepository courtSinglePointsOfEntryRepository;
    private final CourtLocalAuthoritiesRepository courtLocalAuthoritiesRepository;
    private final CourtCodesRepository courtCodesRepository;
    private final CourtProfessionalInformationRepository courtProfessionalInformationRepository;
    private final CourtDxCodeRepository courtDxCodeRepository;
    private final CourtFaxRepository courtFaxRepository;
    private final LegacyCourtMappingRepository legacyCourtMappingRepository;
    private final MigrationAuditRepository migrationAuditRepository;
    private final ConfigurableApplicationContext applicationContext;
    private final LegacyFactClient legacyFactClient;
    private static final Pattern COURT_NAME_PATTERN =
        Pattern.compile("^[A-Za-z&'(),\\- ]+$");
    private static final Pattern GENERIC_DESCRIPTION_PATTERN =
        Pattern.compile(ValidationConstants.GENERIC_DESCRIPTION_REGEX);

    @Autowired
    MigrationIntegrationTest(
        TestRestTemplate restTemplate,
        RegionRepository regionRepository,
        LegacyServiceRepository legacyServiceRepository,
        ServiceAreaRepository serviceAreaRepository,
        LocalAuthorityTypeRepository localAuthorityTypeRepository,
        ContactDescriptionTypeRepository contactDescriptionTypeRepository,
        OpeningHourTypeRepository openingHourTypeRepository,
        CourtTypeRepository courtTypeRepository,
        CourtRepository courtRepository,
        CourtServiceAreasRepository courtServiceAreasRepository,
        CourtAreasOfLawRepository courtAreasOfLawRepository,
        CourtSinglePointsOfEntryRepository courtSinglePointsOfEntryRepository,
        CourtLocalAuthoritiesRepository courtLocalAuthoritiesRepository,
        CourtCodesRepository courtCodesRepository,
        CourtProfessionalInformationRepository courtProfessionalInformationRepository,
        CourtDxCodeRepository courtDxCodeRepository,
        CourtFaxRepository courtFaxRepository,
        LegacyCourtMappingRepository legacyCourtMappingRepository,
        MigrationAuditRepository migrationAuditRepository,
        ConfigurableApplicationContext applicationContext,
        LegacyFactClient legacyFactClient
    ) {
        this.restTemplate = restTemplate;
        this.regionRepository = regionRepository;
        this.legacyServiceRepository = legacyServiceRepository;
        this.serviceAreaRepository = serviceAreaRepository;
        this.localAuthorityTypeRepository = localAuthorityTypeRepository;
        this.contactDescriptionTypeRepository = contactDescriptionTypeRepository;
        this.openingHourTypeRepository = openingHourTypeRepository;
        this.courtTypeRepository = courtTypeRepository;
        this.courtRepository = courtRepository;
        this.courtServiceAreasRepository = courtServiceAreasRepository;
        this.courtAreasOfLawRepository = courtAreasOfLawRepository;
        this.courtSinglePointsOfEntryRepository = courtSinglePointsOfEntryRepository;
        this.courtLocalAuthoritiesRepository = courtLocalAuthoritiesRepository;
        this.courtCodesRepository = courtCodesRepository;
        this.courtProfessionalInformationRepository = courtProfessionalInformationRepository;
        this.courtDxCodeRepository = courtDxCodeRepository;
        this.courtFaxRepository = courtFaxRepository;
        this.legacyCourtMappingRepository = legacyCourtMappingRepository;
        this.migrationAuditRepository = migrationAuditRepository;
        this.applicationContext = applicationContext;
        this.legacyFactClient = legacyFactClient;
    }

    /**
     * Legacy export captured at test start; null when the source endpoint is unavailable.
     * TODO(FACT-2486): Remove the assumptions once the legacy export endpoint is stable everywhere.
     */
    private LegacyExportResponse legacySnapshot;
    private List<CourtDto> migratableCourts = Collections.emptyList();
    private Set<Integer> exportedRegionIds = Collections.emptySet();
    private Set<Integer> exportedAreaOfLawIds = Collections.emptySet();
    private Set<Integer> mappedLocalAuthorityTypeIds = Collections.emptySet();

    @BeforeAll
    void fetchLegacySnapshot() {
        try {
            // Capture the payload returned by the legacy FaCT private migration endpoint so the
            // assertions below can compare each migrated table with the source data.
            legacySnapshot = legacyFactClient.fetchExport();
        } catch (FeignException | MigrationClientException ex) {
            legacySnapshot = null;
        }
        Assumptions.assumeTrue(legacySnapshot != null, "Legacy FaCT export endpoint is unavailable");
        exportedRegionIds = extractIds(legacySnapshot.getRegions(), RegionDto::getId);
        exportedAreaOfLawIds = extractIds(legacySnapshot.getAreaOfLawTypes(), AreaOfLawTypeDto::getId);
        mappedLocalAuthorityTypeIds = legacySnapshot.getLocalAuthorityTypes() == null
            ? Collections.emptySet()
            : legacySnapshot.getLocalAuthorityTypes().stream()
                .filter(type -> StringUtils.isNotBlank(type.getName()))
                .map(LocalAuthorityTypeDto::getId)
                .collect(Collectors.toSet());
        migratableCourts = legacySnapshot.getCourts() == null
            ? Collections.emptyList()
            : legacySnapshot.getCourts().stream()
                .filter(this::isMigratableCourt)
                .collect(Collectors.toList());
    }

    @BeforeEach
    void insertReferenceDataBeforeTest() {
        migrationAuditRepository.deleteAll();
        if (!applicationContext.isActive()
            || legacySnapshot == null
            || legacySnapshot.getLocalAuthorityTypes() == null) {
            return;
        }
        try {
            localAuthorityTypeRepository.deleteAll();
            legacySnapshot.getLocalAuthorityTypes().forEach(type ->
                localAuthorityTypeRepository.save(
                    LocalAuthorityType.builder()
                        .name(type.getName())
                        .build()
                )
            );
        } catch (ConfigurationPropertiesBindException ex) {
            return;
        } catch (RuntimeException ex) {
            if (ex.getCause() instanceof IllegalStateException) {
                return;
            }
            throw ex;
        }
    }

    @Test
    void shouldImportLegacyData() {
        Assumptions.assumeTrue(legacySnapshot != null, "Legacy FaCT export endpoint is unavailable");
        // Capture baseline counts so we can assert on the delta introduced by this migration run.
        final TableCounts before = captureTableCounts();

        // Trigger the new FaCT migration endpoint which fetches the legacy export again inside the service layer.
        ResponseEntity<MigrationResponse> response = restTemplate.postForEntity(
            migrationUrl(), null, MigrationResponse.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        MigrationResponse body = response.getBody();
        assertThat(body).isNotNull();
        assertThat(body.getMessage()).isEqualTo("Migration completed successfully");

        int expectedCourts = countMigratableCourts();
        long expectedCourtLocalAuthorities = countCourtLocalAuthorities();
        long expectedCourtServiceAreas = countCourtServiceAreas();
        long expectedCourtAreasOfLaw = countCourtAreasOfLaw();
        long expectedCourtSpocs = countCourtSinglePointsOfEntry();
        long expectedCourtProfessionalInformation = countCourtsWithProfessionalInformation();
        long expectedCourtDxCodes = countCourtDxCodes();
        long expectedCourtFax = countCourtFax();
        long expectedCourtCodes = countCourtCodes();

        assertThat(body.getResult().getCourtsMigrated()).isEqualTo(expectedCourts);
        assertThat(body.getResult().getCourtAreasOfLawMigrated()).isEqualTo(expectedCourtAreasOfLaw);
        assertThat(body.getResult().getCourtServiceAreasMigrated()).isEqualTo(expectedCourtServiceAreas);
        assertThat(body.getResult().getCourtLocalAuthoritiesMigrated()).isEqualTo(expectedCourtLocalAuthorities);
        assertThat(body.getResult().getCourtSinglePointsOfEntryMigrated()).isEqualTo(expectedCourtSpocs);
        assertThat(body.getResult().getCourtProfessionalInformationMigrated())
            .isEqualTo(expectedCourtProfessionalInformation);
        assertThat(body.getResult().getCourtCodesMigrated()).isEqualTo(expectedCourtCodes);
        assertThat(body.getResult().getCourtDxCodesMigrated()).isEqualTo(expectedCourtDxCodes);
        assertThat(body.getResult().getCourtFaxMigrated()).isEqualTo(expectedCourtFax);

        assertThat(legacyServiceRepository.count()).isEqualTo(before.services());
        assertThat(serviceAreaRepository.count()).isEqualTo(before.serviceAreas());
        assertThat(localAuthorityTypeRepository.count()).isEqualTo(before.localAuthorityTypes());
        assertThat(contactDescriptionTypeRepository.count()).isEqualTo(before.contactDescriptionTypes());
        assertThat(openingHourTypeRepository.count()).isEqualTo(before.openingHourTypes());
        // Reference tables (regions, court types, etc.) are populated via Flyway and not reasserted here.
        assertThat(courtRepository.count()).isEqualTo(before.courts() + expectedCourts);
        assertThat(courtServiceAreasRepository.count())
            .isEqualTo(before.courtServiceAreas() + expectedCourtServiceAreas);
        assertThat(courtAreasOfLawRepository.count()).isEqualTo(before.courtAreasOfLaw() + expectedCourtAreasOfLaw);
        assertThat(courtSinglePointsOfEntryRepository.count())
            .isEqualTo(before.courtSinglePointsOfEntry() + expectedCourtSpocs);
        assertThat(courtLocalAuthoritiesRepository.count())
            .isEqualTo(before.courtLocalAuthorities() + expectedCourtLocalAuthorities);
        assertThat(courtCodesRepository.count()).isEqualTo(before.courtCodes() + expectedCourtCodes);
        assertThat(courtProfessionalInformationRepository.count())
            .isEqualTo(before.courtProfessionalInformation() + expectedCourtProfessionalInformation);
        assertThat(courtDxCodeRepository.count()).isEqualTo(before.courtDxCodes() + expectedCourtDxCodes);
        assertThat(courtFaxRepository.count()).isEqualTo(before.courtFax() + expectedCourtFax);
        assertThat(legacyCourtMappingRepository.count()).isEqualTo(before.legacyCourtMappings() + expectedCourts);

        LegacyService service = legacyServiceRepository.findAll().get(0);
        assertThat(service.getName()).isNotBlank();
        Court court = courtRepository.findAll().get(0);
        CourtProfessionalInformation professionalInformation = courtProfessionalInformationRepository.findAll().get(0);
        assertThat(professionalInformation.getCourtId()).isEqualTo(court.getId());
    }

    @Test
    void shouldNotInvokeLegacyEndpointWhenDataAlreadyPresent() {
        Assumptions.assumeTrue(legacySnapshot != null, "Legacy FaCT export endpoint is unavailable");
        migrationAuditRepository.save(
            MigrationAudit.builder()
                .migrationName("legacy-data-migration")
                .status(MigrationStatus.SUCCESS)
                .updatedAt(Instant.now())
                .build()
        );

        ResponseEntity<String> response = restTemplate.postForEntity(migrationUrl(), null, String.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
    }

    private URI migrationUrl() {
        return URI.create("http://localhost:" + port + "/migration/import");
    }

    private int countMigratableCourts() {
        return migratableCourts.size();
    }

    private long countCourtServiceAreas() {
        return migratableCourts.stream()
            .map(CourtDto::getCourtServiceAreas)
            .filter(Objects::nonNull)
            .mapToLong(List::size)
            .sum();
    }

    private long countCourtLocalAuthorities() {
        return migratableCourts.stream()
            .map(CourtDto::getCourtLocalAuthorities)
            .filter(Objects::nonNull)
            .flatMap(List::stream)
            .filter(this::isPersistableCourtLocalAuthority)
            .count();
    }

    private long countCourtAreasOfLaw() {
        return migratableCourts.stream()
            .map(CourtDto::getCourtAreasOfLaw)
            .filter(Objects::nonNull)
            .filter(this::hasMappedAreaOfLawIds)
            .count();
    }

    private long countCourtSinglePointsOfEntry() {
        return migratableCourts.stream()
            .map(CourtDto::getCourtSinglePointsOfEntry)
            .filter(Objects::nonNull)
            .filter(this::hasMappedAreaOfLawIds)
            .count();
    }

    private long countCourtsWithProfessionalInformation() {
        return migratableCourts.stream()
            .map(CourtDto::getCourtProfessionalInformation)
            .filter(Objects::nonNull)
            .count();
    }

    private long countCourtDxCodes() {
        return migratableCourts.stream()
            .map(CourtDto::getCourtDxCodes)
            .filter(Objects::nonNull)
            .flatMap(List::stream)
            .filter(this::isPersistableDxCode)
            .count();
    }

    private long countCourtFax() {
        return migratableCourts.stream()
            .map(CourtDto::getCourtFax)
            .filter(Objects::nonNull)
            .flatMap(List::stream)
            .filter(dto -> StringUtils.isNotBlank(dto.getFaxNumber()))
            .count();
    }

    private long countCourtCodes() {
        return migratableCourts.stream()
            .map(CourtDto::getCourtCodes)
            .filter(Objects::nonNull)
            .count();
    }

    private boolean isMigratableCourt(CourtDto court) {
        if (court == null || !isValidCourtName(court.getName())) {
            return false;
        }
        if (Boolean.TRUE.equals(court.getIsServiceCentre())) {
            return true;
        }
        return court.getRegionId() != null && exportedRegionIds.contains(court.getRegionId());
    }

    private boolean hasMappedAreaOfLawIds(CourtAreasOfLawDto dto) {
        return dto != null && hasMappedIds(dto.getAreaOfLawIds());
    }

    private boolean hasMappedAreaOfLawIds(CourtSinglePointOfEntryDto dto) {
        return dto != null && hasMappedIds(dto.getAreaOfLawIds());
    }

    private boolean hasMappedIds(List<Integer> ids) {
        return ids != null
            && !ids.isEmpty()
            && ids.stream().anyMatch(exportedAreaOfLawIds::contains);
    }

    private boolean isPersistableCourtLocalAuthority(CourtLocalAuthorityDto dto) {
        if (dto == null || dto.getLocalAuthorityIds() == null || dto.getLocalAuthorityIds().isEmpty()) {
            return false;
        }
        if (dto.getAreaOfLawId() != null && !exportedAreaOfLawIds.contains(dto.getAreaOfLawId())) {
            return false;
        }
        return dto.getLocalAuthorityIds().stream()
            .anyMatch(mappedLocalAuthorityTypeIds::contains);
    }

    private <T> Set<Integer> extractIds(List<T> dtos, Function<T, Integer> extractor) {
        if (dtos == null) {
            return Collections.emptySet();
        }
        return dtos.stream()
            .map(extractor)
            .filter(Objects::nonNull)
            .collect(Collectors.toSet());
    }

    private boolean isValidCourtName(String name) {
        String sanitised = sanitiseCourtName(name);
        return StringUtils.isNotBlank(sanitised) && COURT_NAME_PATTERN.matcher(sanitised).matches();
    }

    private String sanitiseCourtName(String name) {
        if (StringUtils.isBlank(name)) {
            return name;
        }
        String cleaned = name.replaceAll("[^A-Za-z&'(),\\- ]", " ");
        return cleaned.replaceAll("\\s+", " ").trim();
    }

    private boolean isPersistableDxCode(CourtDxCodeDto dto) {
        if (dto == null) {
            return false;
        }
        if (StringUtils.isBlank(dto.getDxCode()) && StringUtils.isBlank(dto.getExplanation())) {
            return false;
        }
        if (StringUtils.length(dto.getDxCode()) > 200) {
            return false;
        }
        return StringUtils.isBlank(dto.getDxCode())
            || GENERIC_DESCRIPTION_PATTERN.matcher(dto.getDxCode()).matches();
    }

    private TableCounts captureTableCounts() {
        return new TableCounts(
            regionRepository.count(),
            legacyServiceRepository.count(),
            serviceAreaRepository.count(),
            localAuthorityTypeRepository.count(),
            contactDescriptionTypeRepository.count(),
            openingHourTypeRepository.count(),
            courtTypeRepository.count(),
            courtRepository.count(),
            courtServiceAreasRepository.count(),
            courtAreasOfLawRepository.count(),
            courtSinglePointsOfEntryRepository.count(),
            courtLocalAuthoritiesRepository.count(),
            courtCodesRepository.count(),
            courtProfessionalInformationRepository.count(),
            courtDxCodeRepository.count(),
            courtFaxRepository.count(),
            legacyCourtMappingRepository.count()
        );
    }

    private record TableCounts(
        long regions,
        long services,
        long serviceAreas,
        long localAuthorityTypes,
        long contactDescriptionTypes,
        long openingHourTypes,
        long courtTypes,
        long courts,
        long courtServiceAreas,
        long courtAreasOfLaw,
        long courtSinglePointsOfEntry,
        long courtLocalAuthorities,
        long courtCodes,
        long courtProfessionalInformation,
        long courtDxCodes,
        long courtFax,
        long legacyCourtMappings
    ) {
    }
}
