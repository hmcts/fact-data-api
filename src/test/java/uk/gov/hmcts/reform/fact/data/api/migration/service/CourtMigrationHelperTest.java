package uk.gov.hmcts.reform.fact.data.api.migration.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.fact.data.api.entities.Court;
import uk.gov.hmcts.reform.fact.data.api.entities.Region;
import uk.gov.hmcts.reform.fact.data.api.migration.model.CourtAreasOfLawDto;
import uk.gov.hmcts.reform.fact.data.api.migration.model.CourtDto;
import uk.gov.hmcts.reform.fact.data.api.migration.model.CourtLocalAuthorityDto;
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
        context.getLocalAuthorityTypeIds().put(20, UUID.randomUUID());
        when(courtService.createCourt(any(Court.class))).thenAnswer(invocation -> {
            Court court = invocation.getArgument(0);
            court.setId(courtId);
            return court;
        });

        CourtDto courtDto = new CourtDto(
            500L,
            "Sample Court",
            "sample-court",
            true,
            1,
            List.of(new CourtServiceAreaDto(1, "LOCAL", List.of(100))),
            List.of(new CourtLocalAuthorityDto(10, 10, List.of(20))),
            null,
            null,
            new CourtAreasOfLawDto("areas", List.of(10)),
            new CourtSinglePointOfEntryDto("spoe", List.of(10)),
            null,
            null,
            null,
            false
        );

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

        CourtDto serviceCentre = new CourtDto(
            600L,
            "Service Centre Court",
            "service-centre-court",
            true,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            true
        );

        int migrated = helper.migrateCourts(List.of(serviceCentre), context);
        assertThat(migrated).isEqualTo(1);
        assertThat(context.getServiceCentreRegionId()).isEqualTo(fallbackRegion);
    }
}
