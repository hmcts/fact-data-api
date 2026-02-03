package uk.gov.hmcts.reform.fact.data.api.migration.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

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
import uk.gov.hmcts.reform.fact.data.api.migration.exception.MigrationClientException;
import uk.gov.hmcts.reform.fact.data.api.migration.model.AreaOfLawTypeDto;
import uk.gov.hmcts.reform.fact.data.api.migration.model.LegacyExportResponse;
import uk.gov.hmcts.reform.fact.data.api.migration.model.LocalAuthorityTypeDto;
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
        when(regionRepository.findByNameAndCountry("South", "England"))
            .thenReturn(Optional.of(Region.builder().id(regionId).build()));
        when(areaOfLawTypeRepository.findByNameIgnoreCase("Family"))
            .thenReturn(Optional.of(AreaOfLawType.builder().id(areaOfLawId).build()));

        when(localAuthorityTypeRepository.findByName("LA"))
            .thenReturn(Optional.of(LocalAuthorityType.builder().id(UUID.randomUUID()).name("LA").build()));
        ServiceArea serviceArea = new ServiceArea();
        serviceArea.setId(UUID.randomUUID());
        when(serviceAreaRepository.findByNameIgnoreCase("Money claims")).thenReturn(Optional.of(serviceArea));

        importer.importReferenceData(createResponse(), context);
        assertThat(context.getRegionIds()).containsEntry(5, regionId);
        assertThat(context.getAreaOfLawIds()).containsEntry(10, areaOfLawId);
        assertThat(context.getServiceAreaIds()).containsValue(serviceArea.getId());
    }

    @Test
    void shouldThrowWhenRegionMissing() {
        LegacyExportResponse failingResponse = new LegacyExportResponse(
            Collections.emptyList(),
            null, null, null, null, null, null,
            List.of(new RegionDto(5, "Missing", "England")),
            null
        );
        assertThatThrownBy(() -> importer.importReferenceData(failingResponse, context))
            .isInstanceOf(MigrationClientException.class);
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
