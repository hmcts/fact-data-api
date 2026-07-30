package uk.gov.hmcts.reform.fact.data.api.migration.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.fact.data.api.entities.AreaOfLawType;
import uk.gov.hmcts.reform.fact.data.api.entities.LocalAuthorityType;
import uk.gov.hmcts.reform.fact.data.api.entities.Region;
import uk.gov.hmcts.reform.fact.data.api.entities.ServiceArea;
import uk.gov.hmcts.reform.fact.data.api.migration.entities.LegacyService;
import uk.gov.hmcts.reform.fact.data.api.migration.model.AreaOfLawTypeDto;
import uk.gov.hmcts.reform.fact.data.api.migration.model.LegacyExportResponse;
import uk.gov.hmcts.reform.fact.data.api.migration.model.LocalAuthorityTypeDto;
import uk.gov.hmcts.reform.fact.data.api.migration.model.MigrationFindingSeverity;
import uk.gov.hmcts.reform.fact.data.api.migration.model.MigrationSection;
import uk.gov.hmcts.reform.fact.data.api.migration.model.RegionDto;
import uk.gov.hmcts.reform.fact.data.api.migration.model.ServiceAreaDto;
import uk.gov.hmcts.reform.fact.data.api.migration.model.ServiceDto;
import uk.gov.hmcts.reform.fact.data.api.repositories.AreaOfLawTypeRepository;
import uk.gov.hmcts.reform.fact.data.api.repositories.ContactDescriptionTypeRepository;
import uk.gov.hmcts.reform.fact.data.api.repositories.LocalAuthorityTypeRepository;
import uk.gov.hmcts.reform.fact.data.api.repositories.OpeningHoursTypeRepository;
import uk.gov.hmcts.reform.fact.data.api.repositories.RegionRepository;
import uk.gov.hmcts.reform.fact.data.api.repositories.ServiceAreaRepository;
import uk.gov.hmcts.reform.fact.data.api.migration.repository.LegacyServiceRepository;

@ExtendWith(MockitoExtension.class)
class ReferenceDataImporterTest {

    @Mock private RegionRepository regionRepository;
    @Mock private AreaOfLawTypeRepository areaOfLawTypeRepository;
    @Mock private ServiceAreaRepository serviceAreaRepository;
    @Mock private LegacyServiceRepository legacyServiceRepository;
    @Mock private LocalAuthorityTypeRepository localAuthorityTypeRepository;
    @Mock private ContactDescriptionTypeRepository contactDescriptionTypeRepository;
    @Mock private OpeningHoursTypeRepository openingHourTypeRepository;

    @InjectMocks
    private ReferenceDataImporter importer;

    private MigrationContext context;

    @BeforeEach
    void setUp() {
        context = new MigrationContext();
    }

    @Test
    void shouldMapExistingRegionsAndAreasOfLaw() {
        UUID regionId = UUID.randomUUID();
        UUID areaOfLawId = UUID.randomUUID();
        UUID localAuthorityId = UUID.randomUUID();
        when(regionRepository.findByNameAndCountry("South", "England"))
            .thenReturn(Optional.of(Region.builder().id(regionId).build()));
        when(areaOfLawTypeRepository.findByNameIgnoreCase("Family"))
            .thenReturn(Optional.of(AreaOfLawType.builder().id(areaOfLawId).build()));
        when(localAuthorityTypeRepository.findAll())
            .thenReturn(List.of(LocalAuthorityType.builder().id(localAuthorityId).name("LA").build()));
        ServiceArea serviceArea = new ServiceArea();
        serviceArea.setId(UUID.randomUUID());
        when(serviceAreaRepository.findByNameIgnoreCase("Money claims")).thenReturn(Optional.of(serviceArea));

        importer.importReferenceData(createResponse(), context);
        assertThat(context.getRegionIds()).containsEntry(5, regionId);
        assertThat(context.getAreaOfLawIds()).containsEntry(10, areaOfLawId);
        assertThat(context.getLocalAuthorityTypeIds()).containsEntry(1, List.of(localAuthorityId));
        assertThat(context.getServiceAreaIds()).containsValue(serviceArea.getId());
    }

    @Test
    void shouldMapLocalAuthorityTypeUsingNormalisedName() {
        UUID mappedId = UUID.randomUUID();
        when(localAuthorityTypeRepository.findAll()).thenReturn(List.of(
            LocalAuthorityType.builder()
                .id(mappedId)
                .name("Bolton Metropolitan Borough Council")
                .build()
        ));

        LegacyExportResponse response = new LegacyExportResponse(
            Collections.emptyList(),
            List.of(new LocalAuthorityTypeDto(42, "Bolton Borough Council")),
            Collections.emptyList(),
            Collections.emptyList(),
            null, null, null,
            Collections.emptyList(),
            Collections.emptyList()
        );

        importer.importReferenceData(response, context);

        assertThat(context.getLocalAuthorityTypeIds()).containsEntry(42, List.of(mappedId));
    }

    @Test
    void shouldMapLegacySplitCountyLocalAuthoritiesToBothSuccessors() {
        UUID northNorthamptonshireId = UUID.randomUUID();
        UUID westNorthamptonshireId = UUID.randomUUID();
        when(localAuthorityTypeRepository.findAll()).thenReturn(List.of(
            LocalAuthorityType.builder()
                .id(northNorthamptonshireId)
                .name("North Northamptonshire Council")
                .build(),
            LocalAuthorityType.builder()
                .id(westNorthamptonshireId)
                .name("West Northamptonshire Council")
                .build()
        ));

        LegacyExportResponse response = new LegacyExportResponse(
            Collections.emptyList(),
            List.of(new LocalAuthorityTypeDto(397392, "Northamptonshire County Council")),
            Collections.emptyList(),
            Collections.emptyList(),
            null, null, null,
            Collections.emptyList(),
            Collections.emptyList()
        );

        importer.importReferenceData(response, context);

        assertThat(context.getLocalAuthorityTypeIds().get(397392))
            .containsExactlyInAnyOrder(northNorthamptonshireId, westNorthamptonshireId);
    }

    @Test
    void shouldReportWhenRegionMissing() {
        LegacyExportResponse failingResponse = new LegacyExportResponse(
            Collections.emptyList(),
            null, null, null, null, null, null,
            List.of(new RegionDto(5, "Missing", "England")),
            null
        );
        importer.importReferenceData(failingResponse, context);

        assertThat(context.getFindings())
            .anySatisfy(finding -> {
                assertThat(finding.getSeverity()).isEqualTo(MigrationFindingSeverity.REVIEW);
                assertThat(finding.getReasonCode()).isEqualTo("REGION_REFERENCE_NOT_FOUND");
                assertThat(finding.getSourceRecordId()).isEqualTo(5);
            });
    }

    @Test
    void shouldUpdateExactlyOneSeededServiceAndEveryServiceAreaLink() {
        UUID serviceAreaId = UUID.randomUUID();
        ServiceArea serviceArea = new ServiceArea();
        serviceArea.setId(serviceAreaId);
        when(serviceAreaRepository.findByNameIgnoreCase("Money claims")).thenReturn(Optional.of(serviceArea));
        LegacyService seeded = LegacyService.builder()
            .id(UUID.randomUUID())
            .name("Service")
            .nameCy("Old Welsh")
            .description("Old description")
            .descriptionCy("Old Welsh description")
            .serviceAreas(List.of())
            .build();
        when(legacyServiceRepository.findAllByNameIgnoreCase("Service")).thenReturn(List.of(seeded));

        LegacyExportResponse response = new LegacyExportResponse(
            List.of(),
            List.of(),
            List.of(serviceAreaDto()),
            List.of(new ServiceDto(1, "Service", "Gwasanaeth", "desc", "desc cy", List.of(1))),
            List.of(),
            List.of(),
            List.of(),
            List.of(),
            List.of()
        );

        importer.importReferenceData(response, context);

        verify(legacyServiceRepository).save(seeded);
        assertThat(seeded.getServiceAreas()).containsExactly(serviceAreaId);
        assertThat(context.getServicesMigrated()).isEqualTo(1);
        assertThat(context.getServiceAreaLinksMigrated()).isEqualTo(1);
        assertThat(context.getSectionCounts().get(MigrationSection.SERVICES).getPersistedRecords())
            .isEqualTo(1);
    }

    @Test
    void shouldRejectNullServiceDescriptionWithoutOverwritingSeededData() {
        LegacyExportResponse response = new LegacyExportResponse(
            List.of(),
            List.of(),
            List.of(serviceAreaDto()),
            List.of(new ServiceDto(1, "Service", "Gwasanaeth", null, "desc cy", List.of(1))),
            List.of(),
            List.of(),
            List.of(),
            List.of(),
            List.of()
        );

        importer.importReferenceData(response, context);

        verify(legacyServiceRepository, never()).save(any());
        assertThat(context.getFindings())
            .anySatisfy(finding -> {
                assertThat(finding.getSeverity()).isEqualTo(MigrationFindingSeverity.ERROR);
                assertThat(finding.getReasonCode()).isEqualTo("SERVICE_REQUIRED_FIELDS_MISSING");
                assertThat(finding.getFields()).contains("description");
            });
    }

    private ServiceAreaDto serviceAreaDto() {
        return new ServiceAreaDto(
            1,
            "Money claims",
            "Hawliadau am arian",
            null,
            null,
            null,
            null,
            null,
            "CIVIL",
            null,
            null,
            "POSTCODE",
            10
        );
    }

    private LegacyExportResponse createResponse() {
        return new LegacyExportResponse(
            Collections.emptyList(),
            List.of(new LocalAuthorityTypeDto(1, "LA")),
            List.of(serviceAreaDto()),
            List.of(new ServiceDto(1, "Service", "Gwasanaeth", "desc", "desc cy", List.of(1))),
            null,
            null,
            null,
            List.of(new RegionDto(5, "South", "England")),
            List.of(new AreaOfLawTypeDto(10, "Family", "Teulu"))
        );
    }
}
