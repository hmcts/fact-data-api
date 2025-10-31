package uk.gov.hmcts.reform.fact.data.api.migration.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;
import uk.gov.hmcts.reform.fact.data.api.entities.AreaOfLawType;
import uk.gov.hmcts.reform.fact.data.api.entities.Court;
import uk.gov.hmcts.reform.fact.data.api.entities.CourtPostcode;
import uk.gov.hmcts.reform.fact.data.api.entities.Region;
import uk.gov.hmcts.reform.fact.data.api.entities.LocalAuthorityType;
import uk.gov.hmcts.reform.fact.data.api.entities.ServiceArea;
import uk.gov.hmcts.reform.fact.data.api.entities.types.CatchmentMethod;
import uk.gov.hmcts.reform.fact.data.api.entities.types.CatchmentType;
import uk.gov.hmcts.reform.fact.data.api.entities.types.ServiceAreaType;
import uk.gov.hmcts.reform.fact.data.api.migration.client.LegacyFactClient;
import uk.gov.hmcts.reform.fact.data.api.migration.entities.LegacyService;
import uk.gov.hmcts.reform.fact.data.api.migration.entities.MigrationAudit;
import uk.gov.hmcts.reform.fact.data.api.migration.entities.MigrationAuditStatus;
import uk.gov.hmcts.reform.fact.data.api.migration.exception.MigrationAlreadyAppliedException;
import uk.gov.hmcts.reform.fact.data.api.migration.exception.MigrationClientException;
import uk.gov.hmcts.reform.fact.data.api.migration.model.AreaOfLawTypeDto;
import uk.gov.hmcts.reform.fact.data.api.migration.model.ContactDescriptionTypeDto;
import uk.gov.hmcts.reform.fact.data.api.migration.model.CourtAreasOfLawDto;
import uk.gov.hmcts.reform.fact.data.api.migration.model.CourtCodesDto;
import uk.gov.hmcts.reform.fact.data.api.migration.model.CourtDto;
import uk.gov.hmcts.reform.fact.data.api.migration.model.CourtLocalAuthorityDto;
import uk.gov.hmcts.reform.fact.data.api.migration.model.CourtDxCodeDto;
import uk.gov.hmcts.reform.fact.data.api.migration.model.CourtFaxDto;
import uk.gov.hmcts.reform.fact.data.api.migration.model.CourtPostcodeDto;
import uk.gov.hmcts.reform.fact.data.api.migration.model.CourtServiceAreaDto;
import uk.gov.hmcts.reform.fact.data.api.migration.model.CourtSinglePointOfEntryDto;
import uk.gov.hmcts.reform.fact.data.api.migration.model.CourtTypeDto;
import uk.gov.hmcts.reform.fact.data.api.migration.model.LegacyExportResponse;
import uk.gov.hmcts.reform.fact.data.api.migration.model.LocalAuthorityTypeDto;
import uk.gov.hmcts.reform.fact.data.api.migration.model.MigrationResult;
import uk.gov.hmcts.reform.fact.data.api.migration.model.MigrationSummary;
import uk.gov.hmcts.reform.fact.data.api.migration.model.OpeningHourTypeDto;
import uk.gov.hmcts.reform.fact.data.api.migration.model.RegionDto;
import uk.gov.hmcts.reform.fact.data.api.migration.model.ServiceAreaDto;
import uk.gov.hmcts.reform.fact.data.api.migration.model.ServiceDto;
import uk.gov.hmcts.reform.fact.data.api.repositories.AreaOfLawTypeRepository;
import uk.gov.hmcts.reform.fact.data.api.repositories.ContactDescriptionTypeRepository;
import uk.gov.hmcts.reform.fact.data.api.repositories.CourtAreasOfLawRepository;
import uk.gov.hmcts.reform.fact.data.api.repositories.CourtCodesRepository;
import uk.gov.hmcts.reform.fact.data.api.repositories.CourtDxCodeRepository;
import uk.gov.hmcts.reform.fact.data.api.repositories.CourtFaxRepository;
import uk.gov.hmcts.reform.fact.data.api.repositories.CourtLocalAuthoritiesRepository;
import uk.gov.hmcts.reform.fact.data.api.repositories.CourtPostcodeRepository;
import uk.gov.hmcts.reform.fact.data.api.repositories.CourtRepository;
import uk.gov.hmcts.reform.fact.data.api.repositories.CourtServiceAreasRepository;
import uk.gov.hmcts.reform.fact.data.api.repositories.CourtSinglePointsOfEntryRepository;
import uk.gov.hmcts.reform.fact.data.api.repositories.CourtTypeRepository;
import uk.gov.hmcts.reform.fact.data.api.repositories.LocalAuthorityTypeRepository;
import uk.gov.hmcts.reform.fact.data.api.repositories.OpeningHourTypeRepository;
import uk.gov.hmcts.reform.fact.data.api.repositories.RegionRepository;
import uk.gov.hmcts.reform.fact.data.api.repositories.ServiceAreaRepository;
import uk.gov.hmcts.reform.fact.data.api.migration.repository.LegacyServiceRepository;
import uk.gov.hmcts.reform.fact.data.api.migration.repository.MigrationAuditRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MigrationServiceTest {

    private static final UUID REGION_ID = UUID.randomUUID();
    private static final UUID AREA_OF_LAW_ID = UUID.randomUUID();
    private static final UUID SERVICE_AREA_ID = UUID.randomUUID();
    private static final UUID COURT_ID = UUID.randomUUID();
    private static final UUID LOCAL_AUTHORITY_TYPE_ID = UUID.randomUUID();

    @Mock
    private LegacyFactClient legacyFactClient;
    @Mock
    private RegionRepository regionRepository;
    @Mock
    private AreaOfLawTypeRepository areaOfLawTypeRepository;
    @Mock
    private ServiceAreaRepository serviceAreaRepository;
    @Mock
    private LegacyServiceRepository legacyServiceRepository;
    @Mock
    private MigrationAuditRepository migrationAuditRepository;
    @Mock
    private LocalAuthorityTypeRepository localAuthorityTypeRepository;
    @Mock
    private ContactDescriptionTypeRepository contactDescriptionTypeRepository;
    @Mock
    private OpeningHourTypeRepository openingHourTypeRepository;
    @Mock
    private CourtTypeRepository courtTypeRepository;
    @Mock
    private CourtRepository courtRepository;
    @Mock
    private CourtServiceAreasRepository courtServiceAreasRepository;
    @Mock
    private CourtAreasOfLawRepository courtAreasOfLawRepository;
    @Mock
    private CourtSinglePointsOfEntryRepository courtSinglePointsOfEntryRepository;
    @Mock
    private CourtLocalAuthoritiesRepository courtLocalAuthoritiesRepository;
    @Mock
    private CourtPostcodeRepository courtPostcodeRepository;
    @Mock
    private CourtCodesRepository courtCodesRepository;
    @Mock
    private CourtDxCodeRepository courtDxCodeRepository;
    @Mock
    private CourtFaxRepository courtFaxRepository;
    @Mock
    private TransactionTemplate transactionTemplate;

    @InjectMocks
    private MigrationService migrationService;

    @BeforeEach
    void setUp() {
        lenient().when(regionRepository.save(any(Region.class))).thenAnswer(invocation -> {
            Region region = invocation.getArgument(0);
            region.setId(REGION_ID);
            return region;
        });

        lenient().when(areaOfLawTypeRepository.save(any(AreaOfLawType.class))).thenAnswer(invocation -> {
            AreaOfLawType areaOfLawType = invocation.getArgument(0);
            areaOfLawType.setId(AREA_OF_LAW_ID);
            return areaOfLawType;
        });

        lenient().when(serviceAreaRepository.save(any(ServiceArea.class))).thenAnswer(invocation -> {
            ServiceArea serviceArea = invocation.getArgument(0);
            serviceArea.setId(SERVICE_AREA_ID);
            return serviceArea;
        });

        lenient().when(courtRepository.save(any(Court.class))).thenAnswer(invocation -> {
            Court court = invocation.getArgument(0);
            court.setId(COURT_ID);
            return court;
        });

        lenient().when(transactionTemplate.execute(any())).thenAnswer(invocation -> {
            @SuppressWarnings("unchecked")
            TransactionCallback<MigrationSummary> callback = invocation.getArgument(0);
            return callback.doInTransaction(null);
        });

        lenient().when(migrationAuditRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        lenient().when(courtPostcodeRepository.save(any(CourtPostcode.class))).thenAnswer(invocation -> {
            CourtPostcode entity = invocation.getArgument(0);
            entity.setId(UUID.randomUUID());
            return entity;
        });
        lenient().when(courtLocalAuthoritiesRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        LocalAuthorityType localAuthorityType = LocalAuthorityType.builder()
            .id(LOCAL_AUTHORITY_TYPE_ID)
            .name("Local Authority")
            .build();
        lenient().when(localAuthorityTypeRepository.findByName("Local Authority"))
            .thenReturn(Optional.of(localAuthorityType));
        lenient().when(localAuthorityTypeRepository.save(any(LocalAuthorityType.class))).thenAnswer(invocation -> {
            LocalAuthorityType entity = invocation.getArgument(0);
            entity.setId(UUID.randomUUID());
            return entity;
        });

        lenient().when(regionRepository.count()).thenReturn(0L);
        lenient().when(areaOfLawTypeRepository.count()).thenReturn(0L);
        lenient().when(courtRepository.count()).thenReturn(0L);
    }

    @Test
    void shouldPreventDuplicateExecution() {
        when(courtRepository.count()).thenReturn(1L);

        assertThrows(MigrationAlreadyAppliedException.class, () -> migrationService.migrate());

        verifyNoInteractions(legacyFactClient);
    }

    @Test
    void shouldThrowWhenExportResponseEmpty() {
        lenient().when(courtRepository.count()).thenReturn(0L);
        when(legacyFactClient.fetchExport()).thenReturn(null);

        assertThrows(MigrationClientException.class, () -> migrationService.migrate());
    }

    @Test
    void shouldPersistLegacyDataAndReturnSummary() {
        lenient().when(courtRepository.count()).thenReturn(0L);
        LegacyExportResponse response = new LegacyExportResponse(
            List.of(courtDto()),
            List.of(new LocalAuthorityTypeDto(1, "Local Authority")),
            List.of(serviceAreaDto()),
            List.of(serviceDto()),
            List.of(new ContactDescriptionTypeDto(1, "CDT", "CDT CY")),
            List.of(new OpeningHourTypeDto(1, "Monday", "Llun")),
            List.of(new CourtTypeDto(1, "Magistrates")),
            List.of(new RegionDto(1, "Midlands", "England")),
            List.of(new AreaOfLawTypeDto(1, "Housing", "Tai"))
        );

        when(legacyFactClient.fetchExport()).thenReturn(response);

        MigrationSummary summary = migrationService.migrate();
        MigrationResult result = summary.result();

        assertThat(result.courtsMigrated()).isEqualTo(1);
        assertThat(result.regionsMigrated()).isEqualTo(1);
        assertThat(result.areaOfLawTypesMigrated()).isEqualTo(1);
        assertThat(result.serviceAreasMigrated()).isEqualTo(1);
        assertThat(result.servicesMigrated()).isEqualTo(1);
        assertThat(result.localAuthorityTypesMigrated()).isEqualTo(1);
        assertThat(result.contactDescriptionTypesMigrated()).isEqualTo(1);
        assertThat(result.openingHourTypesMigrated()).isEqualTo(1);
        assertThat(result.courtTypesMigrated()).isEqualTo(1);
        assertThat(result.courtLocalAuthoritiesMigrated()).isEqualTo(1);
        assertThat(summary.skippedCourtPostcodes()).isEmpty();

        ArgumentCaptor<MigrationAudit> auditCaptor = ArgumentCaptor.forClass(MigrationAudit.class);
        verify(migrationAuditRepository).save(auditCaptor.capture());
        assertThat(auditCaptor.getValue().getStatus()).isEqualTo(MigrationAuditStatus.SUCCESS);
        assertThat(auditCaptor.getValue().getDetails()).contains("AB1 2CD");

        ArgumentCaptor<CourtPostcode> postcodeCaptor = ArgumentCaptor.forClass(CourtPostcode.class);
        verify(courtPostcodeRepository).save(postcodeCaptor.capture());
        assertThat(postcodeCaptor.getValue().getPostcode()).isEqualTo("AB1 2CD");

        verify(serviceAreaRepository).save(any(ServiceArea.class));
        verify(legacyServiceRepository).save(any(LegacyService.class));
        verify(courtServiceAreasRepository).save(any());
        verify(courtAreasOfLawRepository).save(any());
        verify(courtSinglePointsOfEntryRepository).save(any());
        verify(courtLocalAuthoritiesRepository).save(any());
        verify(courtCodesRepository).save(any());
        verify(courtDxCodeRepository).save(any());
        verify(courtFaxRepository).save(any());
    }

    @Test
    void shouldReportSkippedCourtPostcodes() {
        lenient().when(courtRepository.count()).thenReturn(0L);
        LegacyExportResponse response = new LegacyExportResponse(
            List.of(courtDtoWithInvalidPostcode()),
            List.of(new LocalAuthorityTypeDto(1, "Local Authority")),
            List.of(serviceAreaDto()),
            List.of(serviceDto()),
            List.of(new ContactDescriptionTypeDto(1, "CDT", "CDT CY")),
            List.of(new OpeningHourTypeDto(1, "Monday", "Llun")),
            List.of(new CourtTypeDto(1, "Magistrates")),
            List.of(new RegionDto(1, "Midlands", "England")),
            List.of(new AreaOfLawTypeDto(1, "Housing", "Tai"))
        );

        when(legacyFactClient.fetchExport()).thenReturn(response);

        MigrationSummary summary = migrationService.migrate();

        assertThat(summary.skippedCourtPostcodes())
            .containsExactly("example-court: SNB 6DB (failed regex validation)");
        verify(courtPostcodeRepository, never()).save(any());

        ArgumentCaptor<MigrationAudit> auditCaptor = ArgumentCaptor.forClass(MigrationAudit.class);
        verify(migrationAuditRepository).save(auditCaptor.capture());
        MigrationAudit auditRecord = auditCaptor.getValue();
        assertThat(auditRecord.getStatus()).isEqualTo(MigrationAuditStatus.FAILURE);
        assertThat(auditRecord.getDetails()).contains("SNB 6DB");
    }

    @Test
    void shouldSkipCourtDxCodesWhenDetailsMissing() {
        lenient().when(courtRepository.count()).thenReturn(0L);
        LegacyExportResponse response = new LegacyExportResponse(
            List.of(courtDtoWithEmptyDxCode()),
            List.of(new LocalAuthorityTypeDto(1, "Local Authority")),
            List.of(serviceAreaDto()),
            List.of(serviceDto()),
            List.of(new ContactDescriptionTypeDto(1, "CDT", "CDT CY")),
            List.of(new OpeningHourTypeDto(1, "Monday", "Llun")),
            List.of(new CourtTypeDto(1, "Magistrates")),
            List.of(new RegionDto(1, "Midlands", "England")),
            List.of(new AreaOfLawTypeDto(1, "Housing", "Tai"))
        );

        when(legacyFactClient.fetchExport()).thenReturn(response);

        MigrationSummary summary = migrationService.migrate();

        assertThat(summary.result().courtsMigrated()).isEqualTo(1);
        verify(courtDxCodeRepository, never()).save(any());
    }

    private CourtDto courtDto() {
        return new CourtDto(
            "legacy-court-id",
            "Example Court",
            "example-court",
            true,
            1,
            List.of(new CourtServiceAreaDto(1, CatchmentType.LOCAL.name(), List.of(1))),
            List.of(new CourtPostcodeDto(1, "ab12cd")),
            List.of(new CourtLocalAuthorityDto(1, 1, List.of(1))),
            new CourtCodesDto("codes", 1, 2, 3, 4, 5, "6"),
            new CourtAreasOfLawDto("areas", List.of(1)),
            new CourtSinglePointOfEntryDto("spoe", List.of(1)),
            List.of(new CourtDxCodeDto("dx", "123", "dx explanation")),
            List.of(new CourtFaxDto("fax", "01632960000")),
            null,
            true
        );
    }

    private ServiceAreaDto serviceAreaDto() {
        return new ServiceAreaDto(
            1,
            "Service Area",
            "Ardal Wasanaeth",
            "description",
            "disgrifiad",
            "http://example.com",
            "online text",
            "testun ar-lein",
            ServiceAreaType.CIVIL.name(),
            "text",
            "testun",
            CatchmentMethod.POSTCODE.name(),
            1
        );
    }

    private ServiceDto serviceDto() {
        return new ServiceDto(
            1,
            "Service",
            "Gwasanaeth",
            "desc",
            "disgrifiad",
            List.of(1)
        );
    }

    private CourtDto courtDtoWithInvalidPostcode() {
        return new CourtDto(
            "legacy-court-id",
            "Example Court",
            "example-court",
            true,
            1,
            List.of(new CourtServiceAreaDto(1, CatchmentType.LOCAL.name(), List.of(1))),
            List.of(new CourtPostcodeDto(1, "SNB 6DB")),
            List.of(new CourtLocalAuthorityDto(1, 1, List.of(1))),
            new CourtCodesDto("codes", 1, 2, 3, 4, 5, "6"),
            new CourtAreasOfLawDto("areas", List.of(1)),
            new CourtSinglePointOfEntryDto("spoe", List.of(1)),
            List.of(new CourtDxCodeDto("dx", "123", "dx explanation")),
            List.of(new CourtFaxDto("fax", "01632960000")),
            null,
            true
        );
    }

    private CourtDto courtDtoWithEmptyDxCode() {
        return new CourtDto(
            "legacy-court-id",
            "Example Court",
            "example-court",
            true,
            1,
            List.of(new CourtServiceAreaDto(1, CatchmentType.LOCAL.name(), List.of(1))),
            List.of(new CourtPostcodeDto(1, "ab12cd")),
            List.of(new CourtLocalAuthorityDto(1, 1, List.of(1))),
            new CourtCodesDto("codes", 1, 2, 3, 4, 5, "6"),
            new CourtAreasOfLawDto("areas", List.of(1)),
            new CourtSinglePointOfEntryDto("spoe", List.of(1)),
            List.of(new CourtDxCodeDto("dx", null, null)),
            List.of(new CourtFaxDto("fax", "01632960000")),
            null,
            true
        );
    }
}
