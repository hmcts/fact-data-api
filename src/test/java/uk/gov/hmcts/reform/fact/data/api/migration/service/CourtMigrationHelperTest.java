package uk.gov.hmcts.reform.fact.data.api.migration.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.fact.data.api.entities.Court;
import uk.gov.hmcts.reform.fact.data.api.entities.CourtDxCode;
import uk.gov.hmcts.reform.fact.data.api.entities.CourtLocalAuthorities;
import uk.gov.hmcts.reform.fact.data.api.entities.CourtProfessionalInformation;
import uk.gov.hmcts.reform.fact.data.api.entities.Region;
import uk.gov.hmcts.reform.fact.data.api.migration.model.CourtAreasOfLawDto;
import uk.gov.hmcts.reform.fact.data.api.migration.model.CourtDto;
import uk.gov.hmcts.reform.fact.data.api.migration.model.CourtDxCodeDto;
import uk.gov.hmcts.reform.fact.data.api.migration.model.CourtLocalAuthorityDto;
import uk.gov.hmcts.reform.fact.data.api.migration.model.CourtProfessionalInformationDto;
import uk.gov.hmcts.reform.fact.data.api.migration.model.CourtServiceAreaDto;
import uk.gov.hmcts.reform.fact.data.api.migration.model.CourtSinglePointOfEntryDto;
import uk.gov.hmcts.reform.fact.data.api.repositories.CourtAreasOfLawRepository;
import uk.gov.hmcts.reform.fact.data.api.repositories.CourtCodesRepository;
import uk.gov.hmcts.reform.fact.data.api.repositories.CourtDxCodeRepository;
import uk.gov.hmcts.reform.fact.data.api.repositories.CourtFaxRepository;
import uk.gov.hmcts.reform.fact.data.api.repositories.CourtLocalAuthoritiesRepository;
import uk.gov.hmcts.reform.fact.data.api.repositories.CourtProfessionalInformationRepository;
import uk.gov.hmcts.reform.fact.data.api.repositories.CourtServiceAreasRepository;
import uk.gov.hmcts.reform.fact.data.api.repositories.CourtSinglePointsOfEntryRepository;
import uk.gov.hmcts.reform.fact.data.api.repositories.RegionRepository;
import uk.gov.hmcts.reform.fact.data.api.services.CourtService;
import uk.gov.hmcts.reform.fact.data.api.migration.repository.LegacyCourtMappingRepository;

@ExtendWith(MockitoExtension.class)
class CourtMigrationHelperTest {

    @Mock private RegionRepository regionRepository;
    @Mock private CourtServiceAreasRepository courtServiceAreasRepository;
    @Mock private CourtAreasOfLawRepository courtAreasOfLawRepository;
    @Mock private CourtSinglePointsOfEntryRepository courtSinglePointsOfEntryRepository;
    @Mock private CourtLocalAuthoritiesRepository courtLocalAuthoritiesRepository;
    @Mock private CourtProfessionalInformationRepository courtProfessionalInformationRepository;
    @Mock private CourtCodesRepository courtCodesRepository;
    @Mock private CourtDxCodeRepository courtDxCodeRepository;
    @Mock private CourtFaxRepository courtFaxRepository;
    @Mock private LegacyCourtMappingRepository legacyCourtMappingRepository;
    @Mock private CourtService courtService;

    private CourtMigrationHelper helper;
    private MigrationContext context;

    @BeforeEach
    void setUp() {
        helper = new CourtMigrationHelper(
            regionRepository,
            courtServiceAreasRepository,
            courtAreasOfLawRepository,
            courtSinglePointsOfEntryRepository,
            courtLocalAuthoritiesRepository,
            courtProfessionalInformationRepository,
            courtCodesRepository,
            courtDxCodeRepository,
            courtFaxRepository,
            legacyCourtMappingRepository,
            courtService
        );
        context = new MigrationContext();
    }

    @Test
    void shouldPersistCourtRelationshipsAndIncrementCounters() {
        UUID regionId = UUID.randomUUID();
        UUID courtId = UUID.randomUUID();
        context.getRegionIds().put(1, regionId);
        context.getAreaOfLawIds().put(10, UUID.randomUUID());
        context.getServiceAreaIds().put(100, UUID.randomUUID());
        context.getLocalAuthorityTypeIds().put(20, List.of(UUID.randomUUID()));
        when(courtService.createCourt(any(Court.class))).thenAnswer(invocation -> {
            Court court = invocation.getArgument(0);
            court.setId(courtId);
            return court;
        });

        CourtDto courtDto = new CourtDto();
        courtDto.setId(500L);
        courtDto.setName("Sample Court");
        courtDto.setSlug("sample-court");
        courtDto.setOpen(true);
        courtDto.setRegionId(1);
        courtDto.setCourtServiceAreas(List.of(new CourtServiceAreaDto(1, "LOCAL", List.of(100))));
        courtDto.setCourtLocalAuthorities(List.of(new CourtLocalAuthorityDto(10, 10, List.of(20))));
        courtDto.setCourtAreasOfLaw(new CourtAreasOfLawDto("areas", List.of(10)));
        courtDto.setCourtSinglePointsOfEntry(new CourtSinglePointOfEntryDto("spoe", List.of(10)));
        courtDto.setIsServiceCentre(false);

        int migrated = helper.migrateCourts(List.of(courtDto), context);
        assertThat(migrated).isEqualTo(1);
        assertThat(context.getCourtServiceAreasMigrated()).isEqualTo(1);
        assertThat(context.getCourtAreasOfLawMigrated()).isEqualTo(1);
        assertThat(context.getCourtLocalAuthoritiesMigrated()).isEqualTo(1);
        assertThat(context.getCourtSinglePointsOfEntryMigrated()).isEqualTo(1);
    }

    @Test
    void shouldUseServiceCentreFallbackRegion() {
        UUID fallbackRegion = UUID.randomUUID();
        when(regionRepository.findByNameAndCountry("Service Centre", "England"))
            .thenReturn(Optional.of(Region.builder().id(fallbackRegion).build()));
        when(courtService.createCourt(any(Court.class))).thenAnswer(invocation -> {
            Court court = invocation.getArgument(0);
            court.setId(UUID.randomUUID());
            return court;
        });

        CourtDto serviceCentre = new CourtDto();
        serviceCentre.setId(600L);
        serviceCentre.setName("Service Centre Court");
        serviceCentre.setSlug("service-centre-court");
        serviceCentre.setOpen(true);
        serviceCentre.setIsServiceCentre(true);

        int migrated = helper.migrateCourts(List.of(serviceCentre), context);
        assertThat(migrated).isEqualTo(1);
        assertThat(context.getServiceCentreRegionId()).isEqualTo(fallbackRegion);
    }

    @Test
    void shouldAllowNullInterviewRoomCount() {
        context.getRegionIds().put(1, UUID.randomUUID());
        when(courtService.createCourt(any(Court.class))).thenAnswer(invocation -> {
            Court court = invocation.getArgument(0);
            court.setId(UUID.randomUUID());
            return court;
        });

        CourtProfessionalInformationDto info = new CourtProfessionalInformationDto(null, null, null, null, null, null);
        CourtDto dto = new CourtDto();
        dto.setId(700L);
        dto.setName("Null Interview Court");
        dto.setSlug("null-interview-court");
        dto.setOpen(true);
        dto.setRegionId(1);
        dto.setCourtProfessionalInformation(info);
        dto.setIsServiceCentre(false);

        helper.migrateCourts(List.of(dto), context);
        assertThat(context.getCourtProfessionalInformationMigrated()).isEqualTo(1);
        ArgumentCaptor<CourtProfessionalInformation> captor =
            ArgumentCaptor.forClass(CourtProfessionalInformation.class);
        verify(courtProfessionalInformationRepository).save(captor.capture());
        assertThat(captor.getValue().getInterviewRoomCount()).isNull();
    }

    @Test
    void shouldSanitiseCourtNameToRemoveCommas() {
        context.getRegionIds().put(1, UUID.randomUUID());
        when(courtService.createCourt(any(Court.class))).thenAnswer(invocation -> {
            Court court = invocation.getArgument(0);
            court.setId(UUID.randomUUID());
            return court;
        });

        CourtDto dto = new CourtDto();
        dto.setId(705L);
        dto.setName("Enforcement (Crime) Contact Centre - Wales, South West & London");
        dto.setSlug("enforcement-crime-contact-centre");
        dto.setOpen(true);
        dto.setRegionId(1);
        dto.setIsServiceCentre(false);

        int migrated = helper.migrateCourts(List.of(dto), context);

        assertThat(migrated).isEqualTo(1);
        ArgumentCaptor<Court> captor = ArgumentCaptor.forClass(Court.class);
        verify(courtService).createCourt(captor.capture());
        assertThat(captor.getValue().getName())
            .isEqualTo("Enforcement (Crime) Contact Centre - Wales South West & London");
    }

    @Test
    void shouldSanitiseDxCodeExplanationBeforePersisting() {
        context.getRegionIds().put(1, UUID.randomUUID());
        when(courtService.createCourt(any(Court.class))).thenAnswer(invocation -> {
            Court court = invocation.getArgument(0);
            court.setId(UUID.randomUUID());
            return court;
        });

        CourtDto dto = new CourtDto();
        dto.setId(701L);
        dto.setName("DX Court");
        dto.setSlug("dx-court");
        dto.setOpen(true);
        dto.setRegionId(1);
        dto.setCourtDxCodes(List.of(new CourtDxCodeDto("dx-1", "123", "Main office – Room 2")));
        dto.setIsServiceCentre(false);

        helper.migrateCourts(List.of(dto), context);

        ArgumentCaptor<CourtDxCode> captor = ArgumentCaptor.forClass(CourtDxCode.class);
        verify(courtDxCodeRepository).save(captor.capture());
        assertThat(captor.getValue().getDxCode()).isEqualTo("123");
        assertThat(captor.getValue().getExplanation()).isEqualTo("Main office Room 2");
        assertThat(context.getCourtDxCodesMigrated()).isEqualTo(1);
    }

    @Test
    void shouldSkipDxCodeWhenCodeBlankAfterSanitisation() {
        context.getRegionIds().put(1, UUID.randomUUID());
        when(courtService.createCourt(any(Court.class))).thenAnswer(invocation -> {
            Court court = invocation.getArgument(0);
            court.setId(UUID.randomUUID());
            return court;
        });

        CourtDto dto = new CourtDto();
        dto.setId(702L);
        dto.setName("Invalid DX Court");
        dto.setSlug("invalid-dx-court");
        dto.setOpen(true);
        dto.setRegionId(1);
        dto.setCourtDxCodes(List.of(new CourtDxCodeDto("dx-1", null, "Explanation only")));
        dto.setIsServiceCentre(false);

        helper.migrateCourts(List.of(dto), context);

        verify(courtDxCodeRepository, never()).save(any());
        assertThat(context.getCourtDxCodesMigrated()).isZero();
    }

    @Test
    void shouldPersistDxCodeWithNullExplanationWhenSanitisedExplanationBecomesBlank() {
        context.getRegionIds().put(1, UUID.randomUUID());
        when(courtService.createCourt(any(Court.class))).thenAnswer(invocation -> {
            Court court = invocation.getArgument(0);
            court.setId(UUID.randomUUID());
            return court;
        });

        CourtDto dto = new CourtDto();
        dto.setId(704L);
        dto.setName("DX Explanation Court");
        dto.setSlug("dx-explanation-court");
        dto.setOpen(true);
        dto.setRegionId(1);
        dto.setCourtDxCodes(List.of(new CourtDxCodeDto("dx-1", "123", "—")));
        dto.setIsServiceCentre(false);

        helper.migrateCourts(List.of(dto), context);

        ArgumentCaptor<CourtDxCode> captor = ArgumentCaptor.forClass(CourtDxCode.class);
        verify(courtDxCodeRepository).save(captor.capture());
        assertThat(captor.getValue().getDxCode()).isEqualTo("123");
        assertThat(captor.getValue().getExplanation()).isNull();
        assertThat(context.getCourtDxCodesMigrated()).isEqualTo(1);
    }

    @Test
    void shouldExpandMappedLocalAuthorityIdToAllSuccessorAuthorities() {
        UUID courtId = UUID.randomUUID();
        UUID northNorthamptonshireId = UUID.randomUUID();
        UUID westNorthamptonshireId = UUID.randomUUID();
        context.getRegionIds().put(1, UUID.randomUUID());
        context.getAreaOfLawIds().put(10, UUID.randomUUID());
        context.getLocalAuthorityTypeIds().put(397392, List.of(northNorthamptonshireId, westNorthamptonshireId));
        when(courtService.createCourt(any(Court.class))).thenAnswer(invocation -> {
            Court court = invocation.getArgument(0);
            court.setId(courtId);
            return court;
        });

        CourtDto dto = new CourtDto();
        dto.setId(703L);
        dto.setName("Northamptonshire Court");
        dto.setSlug("northamptonshire-court");
        dto.setOpen(true);
        dto.setRegionId(1);
        dto.setCourtLocalAuthorities(List.of(new CourtLocalAuthorityDto(1, 10, List.of(397392))));
        dto.setIsServiceCentre(false);

        helper.migrateCourts(List.of(dto), context);

        ArgumentCaptor<CourtLocalAuthorities> captor = ArgumentCaptor.forClass(CourtLocalAuthorities.class);
        verify(courtLocalAuthoritiesRepository).save(captor.capture());
        assertThat(captor.getValue().getLocalAuthorityIds())
            .containsExactly(northNorthamptonshireId, westNorthamptonshireId);
    }
}
