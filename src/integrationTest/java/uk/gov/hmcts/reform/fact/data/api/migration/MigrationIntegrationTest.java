package uk.gov.hmcts.reform.fact.data.api.migration;

import java.net.URI;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import uk.gov.hmcts.reform.fact.data.api.controllers.CourtController;
import uk.gov.hmcts.reform.fact.data.api.entities.Court;
import uk.gov.hmcts.reform.fact.data.api.entities.CourtProfessionalInformation;
import uk.gov.hmcts.reform.fact.data.api.entities.LocalAuthorityType;
import uk.gov.hmcts.reform.fact.data.api.entities.Region;
import uk.gov.hmcts.reform.fact.data.api.migration.client.LegacyFactClient;
import uk.gov.hmcts.reform.fact.data.api.migration.entities.LegacyService;
import uk.gov.hmcts.reform.fact.data.api.migration.model.AreaOfLawTypeDto;
import uk.gov.hmcts.reform.fact.data.api.migration.model.CourtAreasOfLawDto;
import uk.gov.hmcts.reform.fact.data.api.migration.model.CourtDto;
import uk.gov.hmcts.reform.fact.data.api.migration.model.CourtLocalAuthorityDto;
import uk.gov.hmcts.reform.fact.data.api.migration.model.CourtSinglePointOfEntryDto;
import uk.gov.hmcts.reform.fact.data.api.migration.model.LocalAuthorityTypeDto;
import uk.gov.hmcts.reform.fact.data.api.migration.model.LegacyExportResponse;
import uk.gov.hmcts.reform.fact.data.api.migration.model.MigrationResponse;
import uk.gov.hmcts.reform.fact.data.api.migration.model.RegionDto;
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

/**
 * Full-stack integration tests that stand up the application, hit the migration endpoint over HTTP,
 * and hydrate a real Postgres instance (via Testcontainers) using data pulled from the configured
 * legacy FaCT API. The tests verify that the number of records written to each table matches the
 * payload returned by the legacy endpoint, and that we short-circuit when courts already exist.
 */
@WebMvcTest(CourtController.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
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
    private final LegacyFactClient legacyFactClient;

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
        this.legacyFactClient = legacyFactClient;
    }

    private LegacyExportResponse legacySnapshot;
    private List<CourtDto> migratableCourts = Collections.emptyList();
    private Set<Integer> exportedRegionIds = Collections.emptySet();
    private Set<Integer> exportedAreaOfLawIds = Collections.emptySet();
    private Set<Integer> mappedLocalAuthorityTypeIds = Collections.emptySet();

    @BeforeAll
    void fetchLegacySnapshot() {
        legacySnapshot = legacyFactClient.fetchExport();
        assertThat(legacySnapshot).as("Legacy FaCT export must be available").isNotNull();
        exportedRegionIds = extractIds(legacySnapshot.regions(), RegionDto::id);
        exportedAreaOfLawIds = extractIds(legacySnapshot.areaOfLawTypes(), AreaOfLawTypeDto::id);
        mappedLocalAuthorityTypeIds = legacySnapshot.localAuthorityTypes() == null
            ? Collections.emptySet()
            : legacySnapshot.localAuthorityTypes().stream()
                .filter(type -> StringUtils.isNotBlank(type.name()))
                .map(LocalAuthorityTypeDto::id)
                .collect(Collectors.toSet());
        migratableCourts = legacySnapshot.courts() == null
            ? Collections.emptyList()
            : legacySnapshot.courts().stream()
                .filter(this::isMigratableCourt)
                .collect(Collectors.toList());
    }

    @BeforeEach
    void insertReferenceDataBeforeTest() {
        if (legacySnapshot == null || legacySnapshot.localAuthorityTypes() == null) {
            return;
        }
        localAuthorityTypeRepository.deleteAll();
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
        TableCounts before = captureTableCounts();

        ResponseEntity<MigrationResponse> response = restTemplate.postForEntity(
            migrationUrl(), null, MigrationResponse.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        MigrationResponse body = response.getBody();
        assertThat(body).isNotNull();
        assertThat(body.message()).isEqualTo("Migration completed successfully");

        int expectedCourts = countMigratableCourts();
        int expectedRegions = safeSize(legacySnapshot.regions());
        int expectedAreasOfLaw = safeSize(legacySnapshot.areaOfLawTypes());
        int expectedServiceAreas = safeSize(legacySnapshot.serviceAreas());
        int expectedServices = safeSize(legacySnapshot.services());
        int expectedLocalAuthorityTypes = countMigratableLocalAuthorityTypes(legacySnapshot);
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

        assertThat(regionRepository.count()).isEqualTo(before.regions() + expectedRegions);
        assertThat(legacyServiceRepository.count()).isEqualTo(before.services() + expectedServices);
        assertThat(serviceAreaRepository.count()).isEqualTo(before.serviceAreas() + expectedServiceAreas);
        assertThat(localAuthorityTypeRepository.count()).isEqualTo(before.localAuthorityTypes());
        assertThat(contactDescriptionTypeRepository.count())
            .isEqualTo(before.contactDescriptionTypes() + expectedContactDescriptionTypes);
        assertThat(openingHourTypeRepository.count()).isEqualTo(before.openingHourTypes() + expectedOpeningHourTypes);
        assertThat(courtTypeRepository.count()).isEqualTo(before.courtTypes() + expectedCourtTypes);
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

        ResponseEntity<String> response = restTemplate.postForEntity(migrationUrl(), null, String.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
    }

    private URI migrationUrl() {
        return URI.create("http://localhost:" + port + "/migration/import");
    }

    private int safeSize(List<?> values) {
        return values == null ? 0 : values.size();
    }

    private int countMigratableCourts() {
        return migratableCourts.size();
    }

    private int countMigratableLocalAuthorityTypes(LegacyExportResponse snapshot) {
        return (int) snapshot.localAuthorityTypes().stream()
            .filter(type -> StringUtils.isNotBlank(type.name()))
            .count();
    }

    private long countCourtServiceAreas() {
        return migratableCourts.stream()
            .map(CourtDto::courtServiceAreas)
            .filter(Objects::nonNull)
            .mapToLong(List::size)
            .sum();
    }

    private long countCourtLocalAuthorities() {
        return migratableCourts.stream()
            .map(CourtDto::courtLocalAuthorities)
            .filter(Objects::nonNull)
            .flatMap(List::stream)
            .filter(this::isPersistableCourtLocalAuthority)
            .count();
    }

    private long countCourtAreasOfLaw() {
        return migratableCourts.stream()
            .map(CourtDto::courtAreasOfLaw)
            .filter(Objects::nonNull)
            .filter(this::hasMappedAreaOfLawIds)
            .count();
    }

    private long countCourtSinglePointsOfEntry() {
        return migratableCourts.stream()
            .map(CourtDto::courtSinglePointsOfEntry)
            .filter(Objects::nonNull)
            .filter(this::hasMappedAreaOfLawIds)
            .count();
    }

    private long countCourtsWithProfessionalInformation() {
        return migratableCourts.stream()
            .map(CourtDto::courtProfessionalInformation)
            .filter(Objects::nonNull)
            .count();
    }

    private long countCourtDxCodes() {
        return migratableCourts.stream()
            .map(CourtDto::courtDxCodes)
            .filter(Objects::nonNull)
            .flatMap(List::stream)
            .filter(dto -> StringUtils.isNotBlank(dto.dxCode()) || StringUtils.isNotBlank(dto.explanation()))
            .count();
    }

    private long countCourtFax() {
        return migratableCourts.stream()
            .map(CourtDto::courtFax)
            .filter(Objects::nonNull)
            .flatMap(List::stream)
            .filter(dto -> StringUtils.isNotBlank(dto.faxNumber()))
            .count();
    }

    private long countCourtCodes() {
        return migratableCourts.stream()
            .map(CourtDto::courtCodes)
            .filter(Objects::nonNull)
            .count();
    }

    private boolean isMigratableCourt(CourtDto court) {
        return court != null
            && court.regionId() != null
            && exportedRegionIds.contains(court.regionId());
    }

    private boolean hasMappedAreaOfLawIds(CourtAreasOfLawDto dto) {
        return dto != null && hasMappedIds(dto.areaOfLawIds());
    }

    private boolean hasMappedAreaOfLawIds(CourtSinglePointOfEntryDto dto) {
        return dto != null && hasMappedIds(dto.areaOfLawIds());
    }

    private boolean hasMappedIds(List<Integer> ids) {
        return ids != null
            && !ids.isEmpty()
            && ids.stream().anyMatch(exportedAreaOfLawIds::contains);
    }

    private boolean isPersistableCourtLocalAuthority(CourtLocalAuthorityDto dto) {
        if (dto == null || dto.localAuthorityIds() == null || dto.localAuthorityIds().isEmpty()) {
            return false;
        }
        if (dto.areaOfLawId() != null && !exportedAreaOfLawIds.contains(dto.areaOfLawId())) {
            return false;
        }
        return dto.localAuthorityIds().stream()
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
