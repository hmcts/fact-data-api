package uk.gov.hmcts.reform.fact.data.api.migration.service;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
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
import uk.gov.hmcts.reform.fact.data.api.entities.LocalAuthorityType;
import uk.gov.hmcts.reform.fact.data.api.entities.ServiceArea;
import uk.gov.hmcts.reform.fact.data.api.entities.Region;
import uk.gov.hmcts.reform.fact.data.api.entities.ServiceArea;
import uk.gov.hmcts.reform.fact.data.api.entities.types.CatchmentMethod;
import uk.gov.hmcts.reform.fact.data.api.entities.types.CatchmentType;
import uk.gov.hmcts.reform.fact.data.api.entities.types.ServiceAreaType;
import uk.gov.hmcts.reform.fact.data.api.migration.client.LegacyFactClient;
import uk.gov.hmcts.reform.fact.data.api.migration.entities.LegacyService;
import uk.gov.hmcts.reform.fact.data.api.migration.entities.MigrationAudit;
import uk.gov.hmcts.reform.fact.data.api.migration.entities.MigrationStatus;
import uk.gov.hmcts.reform.fact.data.api.migration.exception.MigrationAlreadyAppliedException;
import uk.gov.hmcts.reform.fact.data.api.migration.exception.MigrationClientException;
import uk.gov.hmcts.reform.fact.data.api.migration.model.AreaOfLawTypeDto;
import uk.gov.hmcts.reform.fact.data.api.migration.model.ContactDescriptionTypeDto;
import uk.gov.hmcts.reform.fact.data.api.migration.model.CourtAreasOfLawDto;
import uk.gov.hmcts.reform.fact.data.api.migration.model.CourtCodesDto;
import uk.gov.hmcts.reform.fact.data.api.migration.model.CourtDto;
import uk.gov.hmcts.reform.fact.data.api.migration.model.CourtDxCodeDto;
import uk.gov.hmcts.reform.fact.data.api.migration.model.CourtFaxDto;
import uk.gov.hmcts.reform.fact.data.api.migration.model.CourtLocalAuthorityDto;
import uk.gov.hmcts.reform.fact.data.api.migration.model.CourtProfessionalInformationDto;
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
import uk.gov.hmcts.reform.fact.data.api.migration.repository.LegacyCourtMappingRepository;
import uk.gov.hmcts.reform.fact.data.api.migration.repository.LegacyServiceRepository;
import uk.gov.hmcts.reform.fact.data.api.migration.repository.MigrationAuditRepository;
import uk.gov.hmcts.reform.fact.data.api.repositories.AreaOfLawTypeRepository;
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
import uk.gov.hmcts.reform.fact.data.api.services.CourtService;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.atLeastOnce;
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
    private CourtProfessionalInformationRepository courtProfessionalInformationRepository;
    @Mock
    private CourtCodesRepository courtCodesRepository;
    @Mock
    private CourtDxCodeRepository courtDxCodeRepository;
    @Mock
    private CourtFaxRepository courtFaxRepository;
    @Mock
    private LegacyCourtMappingRepository legacyCourtMappingRepository;
    @Mock
    private TransactionTemplate transactionTemplate;
    @Mock
    private CourtService courtService;
    @Mock
    private MigrationAuditRepository migrationAuditRepository;

    @InjectMocks
    private MigrationService migrationService;

    @BeforeEach
    void setUp() {
        lenient().when(regionRepository.findByNameAndCountry(anyString(), anyString())).thenAnswer(invocation -> {
            String name = invocation.getArgument(0);
            String country = invocation.getArgument(1);
            return Optional.of(Region.builder()
                .id(UUID.randomUUID())
                .name(name)
                .country(country)
                .build());
        });

        lenient().when(areaOfLawTypeRepository.save(any(AreaOfLawType.class))).thenAnswer(invocation -> {
            AreaOfLawType areaOfLawType = invocation.getArgument(0);
            areaOfLawType.setId(AREA_OF_LAW_ID);
            return areaOfLawType;
        });
        lenient().when(areaOfLawTypeRepository.findByName(anyString())).thenAnswer(invocation -> {
            String name = invocation.getArgument(0);
            return Optional.of(AreaOfLawType.builder()
                .id(UUID.randomUUID())
                .name(name)
                .nameCy(name + " CY")
                .build());
        });
        lenient().when(areaOfLawTypeRepository.findByNameIgnoreCase(anyString())).thenAnswer(invocation -> {
            String name = invocation.getArgument(0);
            return Optional.of(AreaOfLawType.builder()
                .id(UUID.randomUUID())
                .name(name)
                .nameCy(name + " CY")
                .build());
        });

        lenient().when(serviceAreaRepository.findByNameIgnoreCase(anyString())).thenAnswer(invocation -> {
            ServiceArea serviceArea = new ServiceArea();
            serviceArea.setId(SERVICE_AREA_ID);
            return Optional.of(serviceArea);
        });
        lenient().when(legacyServiceRepository.findByName(anyString())).thenReturn(Optional.empty());

        lenient().when(courtService.createCourt(any(Court.class))).thenAnswer(invocation -> {
            Court court = invocation.getArgument(0);
            court.setId(COURT_ID);
            return court;
        });

        lenient().when(transactionTemplate.execute(any())).thenAnswer(invocation -> {
            @SuppressWarnings("unchecked")
            TransactionCallback<MigrationSummary> callback = invocation.getArgument(0);
            return callback.doInTransaction(null);
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

        lenient().when(migrationAuditRepository.findByMigrationName(anyString())).thenReturn(Optional.empty());
        lenient().when(migrationAuditRepository.save(any(MigrationAudit.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void shouldPreventDuplicateExecution() {
        MigrationAudit audit = MigrationAudit.builder()
            .migrationName("legacy-data-migration")
            .status(MigrationStatus.SUCCESS)
            .updatedAt(Instant.now())
            .build();
        when(migrationAuditRepository.findByMigrationName(anyString())).thenReturn(Optional.of(audit));

        assertThrows(MigrationAlreadyAppliedException.class, () -> migrationService.migrate());

        verifyNoInteractions(legacyFactClient);
    }

    @Test
    void shouldThrowWhenExportResponseEmpty() {
        when(legacyFactClient.fetchExport()).thenReturn(null);

        assertThrows(MigrationClientException.class, () -> migrationService.migrate());

        ArgumentCaptor<MigrationAudit> auditCaptor = ArgumentCaptor.forClass(MigrationAudit.class);
        verify(migrationAuditRepository, atLeastOnce()).save(auditCaptor.capture());
        assertThat(auditCaptor.getAllValues().get(0).getStatus()).isEqualTo(MigrationStatus.IN_PROGRESS);
        assertThat(auditCaptor.getAllValues().get(auditCaptor.getAllValues().size() - 1).getStatus())
            .isEqualTo(MigrationStatus.FAILED);
    }

    @Test
    void shouldPersistLegacyDataAndReturnSummary() {
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
        assertThat(result.courtAreasOfLawMigrated()).isEqualTo(1);
        assertThat(result.courtServiceAreasMigrated()).isEqualTo(1);
        assertThat(result.courtLocalAuthoritiesMigrated()).isEqualTo(1);
        assertThat(result.courtSinglePointsOfEntryMigrated()).isEqualTo(1);
        assertThat(result.courtProfessionalInformationMigrated()).isEqualTo(1);
        assertThat(result.courtCodesMigrated()).isEqualTo(1);
        assertThat(result.courtDxCodesMigrated()).isEqualTo(1);
        assertThat(result.courtFaxMigrated()).isEqualTo(1);

        verify(legacyServiceRepository).save(any(LegacyService.class));
        verify(courtServiceAreasRepository).save(any());
        verify(courtAreasOfLawRepository).save(any());
        verify(courtSinglePointsOfEntryRepository).save(any());
        verify(courtLocalAuthoritiesRepository).save(any());
        verify(courtProfessionalInformationRepository).save(any());
        verify(courtCodesRepository).save(any());
        verify(courtDxCodeRepository).save(any());
        verify(courtFaxRepository).save(any());
        verify(legacyCourtMappingRepository).save(any());

        ArgumentCaptor<MigrationAudit> auditCaptor = ArgumentCaptor.forClass(MigrationAudit.class);
        verify(migrationAuditRepository, atLeastOnce()).save(auditCaptor.capture());
        assertThat(auditCaptor.getAllValues().get(0).getStatus()).isEqualTo(MigrationStatus.IN_PROGRESS);
        assertThat(auditCaptor.getAllValues().get(auditCaptor.getAllValues().size() - 1).getStatus())
            .isEqualTo(MigrationStatus.SUCCESS);
    }

    @Test
    void shouldSkipCourtDxCodesWhenDetailsMissing() {
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

    @Test
    void shouldAssignFallbackRegionToServiceCentresWithoutRegion() {
        CourtDto serviceCentre = new CourtDto(
            999L,
            "Service Centre Court",
            "service-centre-court",
            true,
            null,
            List.of(new CourtServiceAreaDto(1, CatchmentType.NATIONAL.name(), List.of(1))),
            List.of(new CourtLocalAuthorityDto(1, 1, List.of(1))),
            professionalInformationDto(),
            new CourtCodesDto("codes", 1, 2, 3, 4, 5, "6"),
            new CourtAreasOfLawDto("areas", List.of(1)),
            new CourtSinglePointOfEntryDto("spoe", List.of(1)),
            List.of(new CourtDxCodeDto("dx", "123", "dx explanation")),
            List.of(new CourtFaxDto("fax", "01632960000")),
            null,
            true
        );
        LegacyExportResponse response = new LegacyExportResponse(
            List.of(serviceCentre),
            List.of(new LocalAuthorityTypeDto(1, "Local Authority")),
            List.of(serviceAreaDto()),
            List.of(serviceDto()),
            List.of(new ContactDescriptionTypeDto(1, "CDT", "CDT CY")),
            List.of(new OpeningHourTypeDto(1, "Monday", "Llun")),
            List.of(new CourtTypeDto(1, "Magistrates")),
            List.of(
                new RegionDto(2, "South East", "England"),
                new RegionDto(1, "Midlands", "England")
            ),
            List.of(new AreaOfLawTypeDto(1, "Housing", "Tai"))
        );

        when(legacyFactClient.fetchExport()).thenReturn(response);

        MigrationSummary summary = migrationService.migrate();

        assertThat(summary.result().courtsMigrated()).isEqualTo(1);
        ArgumentCaptor<Court> captor = ArgumentCaptor.forClass(Court.class);
        verify(courtService).createCourt(captor.capture());
        assertThat(captor.getValue().getRegionId()).isNotNull();
        verify(regionRepository).findByNameAndCountry("Service Centre", "England");
    }

    private CourtDto courtDto() {
        return new CourtDto(
            1L,
            "Example Court",
            "example-court",
            true,
            1,
            List.of(new CourtServiceAreaDto(1, CatchmentType.LOCAL.name(), List.of(1))),
            List.of(new CourtLocalAuthorityDto(1, 1, List.of(1))),
            professionalInformationDto(),
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

    private CourtDto courtDtoWithEmptyDxCode() {
        return new CourtDto(
            2L,
            "Example Court",
            "example-court",
            true,
            1,
            List.of(new CourtServiceAreaDto(1, CatchmentType.LOCAL.name(), List.of(1))),
            List.of(new CourtLocalAuthorityDto(1, 1, List.of(1))),
            professionalInformationDto(),
            new CourtCodesDto("codes", 1, 2, 3, 4, 5, "6"),
            new CourtAreasOfLawDto("areas", List.of(1)),
            new CourtSinglePointOfEntryDto("spoe", List.of(1)),
            List.of(new CourtDxCodeDto("dx", null, null)),
            List.of(new CourtFaxDto("fax", "01632960000")),
            null,
            true
        );
    }

    private CourtProfessionalInformationDto professionalInformationDto() {
        return new CourtProfessionalInformationDto(
            true,
            2,
            "01234567890",
            true,
            true,
            false
        );
    }
}
