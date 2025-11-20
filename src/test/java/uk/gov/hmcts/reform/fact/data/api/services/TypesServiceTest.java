package uk.gov.hmcts.reform.fact.data.api.services;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.fact.data.api.entities.AreaOfLawType;
import uk.gov.hmcts.reform.fact.data.api.entities.ContactDescriptionType;
import uk.gov.hmcts.reform.fact.data.api.entities.CourtType;
import uk.gov.hmcts.reform.fact.data.api.entities.OpeningHourType;
import uk.gov.hmcts.reform.fact.data.api.entities.Region;
import uk.gov.hmcts.reform.fact.data.api.entities.ServiceArea;
import uk.gov.hmcts.reform.fact.data.api.repositories.AreaOfLawTypeRepository;
import uk.gov.hmcts.reform.fact.data.api.repositories.ContactDescriptionTypeRepository;
import uk.gov.hmcts.reform.fact.data.api.repositories.CourtTypeRepository;
import uk.gov.hmcts.reform.fact.data.api.repositories.OpeningHoursTypeRepository;
import uk.gov.hmcts.reform.fact.data.api.repositories.RegionRepository;
import uk.gov.hmcts.reform.fact.data.api.repositories.ServiceAreaRepository;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TypesServiceTest {

    @Mock
    private AreaOfLawTypeRepository areaOfLawTypeRepository;

    @Mock
    private CourtTypeRepository courtTypeRepository;

    @Mock
    private OpeningHoursTypeRepository openingHoursTypeRepository;

    @Mock
    private ContactDescriptionTypeRepository contactDescriptionTypeRepository;

    @Mock
    private RegionRepository regionRepository;

    @Mock
    private ServiceAreaRepository serviceAreaRepository;

    @InjectMocks
    private TypesService typesService;

    private List<AreaOfLawType> areaOfLawTypes;
    private List<CourtType> courtTypes;
    private List<OpeningHourType> openingHourTypes;
    private List<ContactDescriptionType> contactDescriptionTypes;
    private List<Region> regions;
    private List<ServiceArea> serviceAreas;

    @BeforeEach
    void setup() {
        areaOfLawTypes = List.of(
            AreaOfLawType.builder()
                .id(UUID.randomUUID())
                .name("area of law type")
                .build());

        courtTypes = List.of(
            CourtType.builder()
                .id(UUID.randomUUID())
                .name("court type")
                .build());

        openingHourTypes = List.of(
            OpeningHourType.builder()
                .id(UUID.randomUUID())
                .name("opening hour type")
                .build());

        contactDescriptionTypes = List.of(
            ContactDescriptionType.builder()
                .id(UUID.randomUUID())
                .name("contact description")
                .build());

        regions = List.of(
            Region.builder()
                .id(UUID.randomUUID())
                .name("region")
                .build());

        serviceAreas = List.of(
            ServiceArea.builder()
                .id(UUID.randomUUID())
                .name("service area")
                .build());
    }

    @Test
    void getAreasOfLawTypesReturnsAreasOfLawTypesWhenFound() {
        when(areaOfLawTypeRepository.findAll()).thenReturn(areaOfLawTypes);

        List<AreaOfLawType> result = typesService.getAreaOfLawTypes();

        assertThat(result).isEqualTo(areaOfLawTypes);
    }

    @Test
    void getAreasOfLawTypesReturnsEmptyListWhenNoneFound() {
        when(areaOfLawTypeRepository.findAll()).thenReturn(List.of());

        List<AreaOfLawType> result = typesService.getAreaOfLawTypes();

        assertThat(result).isEmpty();
    }

    @Test
    void getAllAreasOfLawTypesByIdsReturnsAreasOfLawTypesWhenFound() {
        List<UUID> ids = List.of(areaOfLawTypes.get(0).getId());
        when(areaOfLawTypeRepository.findAllById(ids)).thenReturn(areaOfLawTypes);

        List<AreaOfLawType> result = typesService.getAllAreasOfLawTypesByIds(ids);

        assertThat(result).isEqualTo(areaOfLawTypes);
    }

    @Test
    void getAllAreasOfLawTypesByIdsReturnsEmptyListWhenNoneFound() {
        List<UUID> ids = List.of(UUID.randomUUID());
        when(areaOfLawTypeRepository.findAllById(ids)).thenReturn(List.of());

        List<AreaOfLawType> result = typesService.getAllAreasOfLawTypesByIds(ids);

        assertThat(result).isEmpty();
    }


    @Test
    void getCourtTypesReturnsCourtTypesWhenFound() {
        when(courtTypeRepository.findAll()).thenReturn(courtTypes);

        List<CourtType> result = typesService.getCourtTypes();

        assertThat(result).isEqualTo(courtTypes);
    }

    @Test
    void getCourtTypesReturnsEmptyListWhenNoneFound() {
        when(courtTypeRepository.findAll()).thenReturn(List.of());

        List<CourtType> result = typesService.getCourtTypes();

        assertThat(result).isEmpty();
    }

    @Test
    void getOpeningHoursTypesReturnsOpeningHoursTypesWhenFound() {
        when(openingHoursTypeRepository.findAll()).thenReturn(openingHourTypes);

        List<OpeningHourType> result = typesService.getOpeningHoursTypes();

        assertThat(result).isEqualTo(openingHourTypes);
    }

    @Test
    void getOpeningHoursTypesReturnsEmptyListWhenNoneFound() {
        when(openingHoursTypeRepository.findAll()).thenReturn(List.of());

        List<OpeningHourType> result = typesService.getOpeningHoursTypes();

        assertThat(result).isEmpty();
    }

    @Test
    void getContactDescriptionTypesReturnsContactDescriptionTypesWhenFound() {
        when(contactDescriptionTypeRepository.findAll()).thenReturn(contactDescriptionTypes);

        List<ContactDescriptionType> result = typesService.getContactDescriptionTypes();

        assertThat(result).isEqualTo(contactDescriptionTypes);
    }

    @Test
    void getContactDescriptionTypesReturnsEmptyListWhenNoneFound() {
        when(contactDescriptionTypeRepository.findAll()).thenReturn(List.of());

        List<ContactDescriptionType> result = typesService.getContactDescriptionTypes();

        assertThat(result).isEmpty();
    }

    @Test
    void getRegionsReturnsRegionsWhenFound() {
        when(regionRepository.findAll()).thenReturn(regions);

        List<Region> result = typesService.getRegions();

        assertThat(result).isEqualTo(regions);
    }

    @Test
    void getRegionsReturnsEmptyListWhenNoneFound() {
        when(regionRepository.findAll()).thenReturn(List.of());

        List<Region> result = typesService.getRegions();

        assertThat(result).isEmpty();
    }

    @Test
    void getServiceAreasReturnsServiceAreasWhenFound() {
        when(serviceAreaRepository.findAll()).thenReturn(serviceAreas);

        List<ServiceArea> result = typesService.getServiceAreas();

        assertThat(result).isEqualTo(serviceAreas);
    }

    @Test
    void getServiceAreasReturnsEmptyListWhenNoneFound() {
        when(serviceAreaRepository.findAll()).thenReturn(List.of());

        List<ServiceArea> result = typesService.getServiceAreas();

        assertThat(result).isEmpty();
    }
}

