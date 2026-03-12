package uk.gov.hmcts.reform.fact.data.api.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.atMost;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import uk.gov.hmcts.reform.fact.data.api.entities.AreaOfLawType;
import uk.gov.hmcts.reform.fact.data.api.entities.ContactDescriptionType;
import uk.gov.hmcts.reform.fact.data.api.entities.Court;
import uk.gov.hmcts.reform.fact.data.api.entities.CourtAccessibilityOptions;
import uk.gov.hmcts.reform.fact.data.api.entities.CourtAddress;
import uk.gov.hmcts.reform.fact.data.api.entities.CourtAreasOfLaw;
import uk.gov.hmcts.reform.fact.data.api.entities.CourtCodes;
import uk.gov.hmcts.reform.fact.data.api.entities.CourtContactDetails;
import uk.gov.hmcts.reform.fact.data.api.entities.CourtCounterServiceOpeningHours;
import uk.gov.hmcts.reform.fact.data.api.entities.CourtDxCode;
import uk.gov.hmcts.reform.fact.data.api.entities.CourtFacilities;
import uk.gov.hmcts.reform.fact.data.api.entities.CourtFax;
import uk.gov.hmcts.reform.fact.data.api.entities.CourtLocalAuthorities;
import uk.gov.hmcts.reform.fact.data.api.entities.CourtOpeningHours;
import uk.gov.hmcts.reform.fact.data.api.entities.CourtPhoto;
import uk.gov.hmcts.reform.fact.data.api.entities.CourtProfessionalInformation;
import uk.gov.hmcts.reform.fact.data.api.entities.CourtServiceAreas;
import uk.gov.hmcts.reform.fact.data.api.entities.CourtSinglePointsOfEntry;
import uk.gov.hmcts.reform.fact.data.api.entities.CourtTranslation;
import uk.gov.hmcts.reform.fact.data.api.entities.CourtType;
import uk.gov.hmcts.reform.fact.data.api.entities.LocalAuthorityType;
import uk.gov.hmcts.reform.fact.data.api.entities.OpeningHourType;
import uk.gov.hmcts.reform.fact.data.api.entities.Region;
import uk.gov.hmcts.reform.fact.data.api.entities.ServiceArea;
import uk.gov.hmcts.reform.fact.data.api.repositories.AreaOfLawTypeRepository;
import uk.gov.hmcts.reform.fact.data.api.repositories.ContactDescriptionTypeRepository;
import uk.gov.hmcts.reform.fact.data.api.repositories.CourtAccessibilityOptionsRepository;
import uk.gov.hmcts.reform.fact.data.api.repositories.CourtAddressRepository;
import uk.gov.hmcts.reform.fact.data.api.repositories.CourtAreasOfLawRepository;
import uk.gov.hmcts.reform.fact.data.api.repositories.CourtCodesRepository;
import uk.gov.hmcts.reform.fact.data.api.repositories.CourtContactDetailsRepository;
import uk.gov.hmcts.reform.fact.data.api.repositories.CourtCounterServiceOpeningHoursRepository;
import uk.gov.hmcts.reform.fact.data.api.repositories.CourtDxCodeRepository;
import uk.gov.hmcts.reform.fact.data.api.repositories.CourtFacilitiesRepository;
import uk.gov.hmcts.reform.fact.data.api.repositories.CourtFaxRepository;
import uk.gov.hmcts.reform.fact.data.api.repositories.CourtLocalAuthoritiesRepository;
import uk.gov.hmcts.reform.fact.data.api.repositories.CourtOpeningHoursRepository;
import uk.gov.hmcts.reform.fact.data.api.repositories.CourtPhotoRepository;
import uk.gov.hmcts.reform.fact.data.api.repositories.CourtProfessionalInformationRepository;
import uk.gov.hmcts.reform.fact.data.api.repositories.CourtServiceAreasRepository;
import uk.gov.hmcts.reform.fact.data.api.repositories.CourtSinglePointsOfEntryRepository;
import uk.gov.hmcts.reform.fact.data.api.repositories.CourtTranslationRepository;
import uk.gov.hmcts.reform.fact.data.api.repositories.CourtTypeRepository;
import uk.gov.hmcts.reform.fact.data.api.repositories.LocalAuthorityTypeRepository;
import uk.gov.hmcts.reform.fact.data.api.repositories.OpeningHoursTypeRepository;
import uk.gov.hmcts.reform.fact.data.api.repositories.RegionRepository;
import uk.gov.hmcts.reform.fact.data.api.repositories.ServiceAreaRepository;
import uk.gov.hmcts.reform.fact.data.api.repositories.ServiceRepository;

import java.util.List;
import java.util.Random;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class TestingSupportServiceTest {

    @Mock
    private CourtService courtService;
    @Mock
    private CourtAccessibilityOptionsRepository courtAccessibilityOptionsRepository;
    @Mock
    private CourtAddressRepository courtAddressRepository;
    @Mock
    private CourtAreasOfLawRepository courtAreasOfLawRepository;
    @Mock
    private CourtCodesRepository courtCodesRepository;
    @Mock
    private CourtContactDetailsRepository courtContactDetailsRepository;
    @Mock
    private CourtCounterServiceOpeningHoursRepository courtCounterServiceOpeningHoursRepository;
    @Mock
    private CourtDxCodeRepository courtDxCodeRepository;
    @Mock
    private CourtFacilitiesRepository courtFacilitiesRepository;
    @Mock
    private CourtFaxRepository courtFaxRepository;
    @Mock
    private CourtLocalAuthoritiesRepository courtLocalAuthoritiesRepository;
    @Mock
    private CourtOpeningHoursRepository courtOpeningHoursRepository;
    @Mock
    private CourtProfessionalInformationRepository courtProfessionalInformationRepository;
    @Mock
    private CourtServiceAreasRepository courtServiceAreasRepository;
    @Mock
    private CourtSinglePointsOfEntryRepository courtSinglePointsOfEntryRepository;
    @Mock
    private CourtTranslationRepository courtTranslationRepository;
    @Mock
    private CourtPhotoService courtPhotoService;
    @Mock
    private CourtPhotoRepository courtPhotoRepository;
    @Mock
    private AreaOfLawTypeRepository areaOfLawTypeRepository;
    @Mock
    private ContactDescriptionTypeRepository contactDescriptionTypeRepository;
    @Mock
    private CourtTypeRepository courtTypeRepository;
    @Mock
    private LocalAuthorityTypeRepository localAuthorityTypeRepository;
    @Mock
    private OpeningHoursTypeRepository openingHoursTypeRepository;
    @Mock
    private RegionRepository regionRepository;
    @Mock
    private ServiceRepository serviceRepository;
    @Mock
    private ServiceAreaRepository serviceAreaRepository;

    @InjectMocks
    private TestingSupportService testingSupportService;

    // Randomised UUID lists for the type repos
    private final List<UUID> regionIds = randomUuidList();
    private final List<UUID> areaOfLawUuids = randomUuidList();
    private final List<UUID> courtTypeIds = randomUuidList();
    private final List<UUID> contactDescIds = randomUuidList();
    private final List<UUID> localAuthorityIds = randomUuidList();
    private final List<UUID> openingHourTypeIds = randomUuidList();
    private final List<UUID> serviceAreaIds = randomUuidList();

    private static List<UUID> randomUuidList() {
        Random random = new Random();
        int size = random.nextInt(20) + 1;
        List<UUID> uuids = new java.util.ArrayList<>();
        for (int i = 0; i < size; i++) {
            uuids.add(UUID.randomUUID());
        }
        return uuids;
    }

    @BeforeEach
    void setup() {
        // RegionRepository
        lenient().when(regionRepository.findAll()).thenReturn(
            regionIds.stream().map(id -> Region.builder().id(id).build()).toList()
        );
        // AreaOfLawTypeRepository
        lenient().when(areaOfLawTypeRepository.findAll()).thenReturn(
            areaOfLawUuids.stream().map(id -> AreaOfLawType.builder().id(id).build()).toList()
        );
        // CourtTypeRepository
        lenient().when(courtTypeRepository.findAll()).thenReturn(
            courtTypeIds.stream().map(id -> CourtType.builder().id(id).name(id.toString()).build()).toList()
        );
        // ContactDescriptionTypeRepository
        lenient().when(contactDescriptionTypeRepository.findAll()).thenReturn(
            contactDescIds.stream().map(id -> ContactDescriptionType.builder().id(id).build()).toList()
        );
        // LocalAuthorityTypeRepository
        lenient().when(localAuthorityTypeRepository.findAll()).thenReturn(
            localAuthorityIds.stream().map(id -> LocalAuthorityType.builder().id(id).build()).toList()
        );
        // OpeningHoursTypeRepository
        lenient().when(openingHoursTypeRepository.findAll()).thenReturn(
            openingHourTypeIds.stream().map(id -> OpeningHourType.builder().id(id).build()).toList()
        );
        // ServiceAreaRepository
        lenient().when(serviceAreaRepository.findAll()).thenReturn(
            serviceAreaIds.stream().map(id -> ServiceArea.builder().id(id).build()).toList()
        );
    }

    @Test
    void createCourtWithValidName() {
        String courtName = "Test Court";
        when(courtService.createCourt(any())).thenAnswer(inv -> {
            Court c = Court.class.cast(inv.getArguments()[0]);
            c.setSlug("test-court");
            return c;
        });

        String result = testingSupportService.createCourt(courtName, null, false);

        assertNotNull(result);
        assertEquals("test-court", result);

        // things that are always called once
        verify(courtService, times(1)).createCourt(any());
        verify(courtAccessibilityOptionsRepository, times(1)).save(any());
        verify(courtAreasOfLawRepository, times(1)).save(any());
        verify(courtContactDetailsRepository, times(1)).save(any());
        verify(courtCounterServiceOpeningHoursRepository, times(1)).save(any());
        verify(courtFacilitiesRepository, times(1)).save(any());
        verify(courtProfessionalInformationRepository, times(1)).save(any());
        verify(courtServiceAreasRepository, times(1)).save(any());
        verify(courtPhotoService, times(1)).setCourtPhoto(any(), any());

        // things that are called at least once
        verify(courtAddressRepository, atLeast(1)).save(any());
        verify(courtOpeningHoursRepository, atLeast(1)).save(any());
        verify(courtLocalAuthoritiesRepository, atLeast(1)).save(any());

        // things that are optional, but only called once if they are called
        verify(courtTranslationRepository, atMost(1)).save(any());
        verify(courtPhotoRepository, atMost(1)).save(any());
        verify(courtSinglePointsOfEntryRepository, atMost(1)).save(any());
        verify(courtCodesRepository, atMost(1)).save(any());
        verify(courtDxCodeRepository, atMost(1)).save(any());
        verify(courtFaxRepository, atMost(2)).save(any());
    }

    @Test
    void createCourtWithEmptyNameThrowsException() {
        String courtName = "";
        assertThrows(NullPointerException.class, () -> testingSupportService.createCourt(courtName, null, false));
    }

    @RepeatedTest(20)
    void createCourtWithSeedGeneratesConsistentResults() {
        String courtName = "Seeded Court";
        long seed = System.nanoTime();

        when(courtService.createCourt(any())).thenAnswer(inv -> inv.getArguments()[0]);

        testingSupportService.createCourt(courtName, seed, false);

        // things that are always called once
        verify(courtService, times(1)).createCourt(any());

        ArgumentCaptor<CourtAccessibilityOptions> courtAccessibilityOptionsArgumentCaptor =
            ArgumentCaptor.forClass(CourtAccessibilityOptions.class);
        verify(courtAccessibilityOptionsRepository, times(1))
            .save(courtAccessibilityOptionsArgumentCaptor.capture());
        ArgumentCaptor<CourtAreasOfLaw> courtAreasOfLawArgumentCaptor =
            ArgumentCaptor.forClass(CourtAreasOfLaw.class);
        verify(courtAreasOfLawRepository, times(1))
            .save(courtAreasOfLawArgumentCaptor.capture());
        ArgumentCaptor<CourtContactDetails> courtContactDetailsArgumentCaptor =
            ArgumentCaptor.forClass(CourtContactDetails.class);
        verify(courtContactDetailsRepository, times(1))
            .save(courtContactDetailsArgumentCaptor.capture());
        ArgumentCaptor<CourtCounterServiceOpeningHours> courtCounterServiceOpeningHoursArgumentCaptor =
            ArgumentCaptor.forClass(CourtCounterServiceOpeningHours.class);
        verify(courtCounterServiceOpeningHoursRepository, times(1))
            .save(courtCounterServiceOpeningHoursArgumentCaptor.capture());
        ArgumentCaptor<CourtFacilities> courtFacilitiesArgumentCaptor =
            ArgumentCaptor.forClass(CourtFacilities.class);
        verify(courtFacilitiesRepository, times(1))
            .save(courtFacilitiesArgumentCaptor.capture());
        ArgumentCaptor<CourtProfessionalInformation> courtProfessionalInformationArgumentCaptor =
            ArgumentCaptor.forClass(CourtProfessionalInformation.class);
        verify(courtProfessionalInformationRepository, times(1))
            .save(courtProfessionalInformationArgumentCaptor.capture());
        ArgumentCaptor<CourtServiceAreas> courtServiceAreasArgumentCaptor =
            ArgumentCaptor.forClass(CourtServiceAreas.class);
        verify(courtServiceAreasRepository, times(1))
            .save(courtServiceAreasArgumentCaptor.capture());

        // can't check court photo

        // things that are called at least once
        ArgumentCaptor<CourtAddress> courtAddressArgumentCaptor = ArgumentCaptor.forClass(CourtAddress.class);
        verify(courtAddressRepository, atLeast(1)).save(courtAddressArgumentCaptor.capture());
        ArgumentCaptor<CourtOpeningHours> courtOpeningHoursArgumentCaptor =
            ArgumentCaptor.forClass(CourtOpeningHours.class);
        verify(courtOpeningHoursRepository, atLeast(1))
            .save(courtOpeningHoursArgumentCaptor.capture());
        ArgumentCaptor<CourtLocalAuthorities> courtLocalAuthoritiesArgumentCaptor =
            ArgumentCaptor.forClass(CourtLocalAuthorities.class);
        verify(courtLocalAuthoritiesRepository, atLeast(1))
            .save(courtLocalAuthoritiesArgumentCaptor.capture());

        // things that are optional, but only called once if they are called
        ArgumentCaptor<CourtTranslation> courtTranslationArgumentCaptor =
            ArgumentCaptor.forClass(CourtTranslation.class);
        verify(courtTranslationRepository, atMost(1))
            .save(courtTranslationArgumentCaptor.capture());
        ArgumentCaptor<CourtPhoto> courtPhotoArgumentCaptor = ArgumentCaptor.forClass(CourtPhoto.class);
        verify(courtPhotoRepository, atMost(1)).save(courtPhotoArgumentCaptor.capture());
        ArgumentCaptor<CourtSinglePointsOfEntry> courtSinglePointsOfEntryArgumentCaptor =
            ArgumentCaptor.forClass(CourtSinglePointsOfEntry.class);
        verify(courtSinglePointsOfEntryRepository, atMost(1))
            .save(courtSinglePointsOfEntryArgumentCaptor.capture());
        ArgumentCaptor<CourtCodes> courtCodesArgumentCaptor = ArgumentCaptor.forClass(CourtCodes.class);
        verify(courtCodesRepository, atMost(1)).save(courtCodesArgumentCaptor.capture());
        ArgumentCaptor<CourtDxCode> courtDxCodeArgumentCaptor = ArgumentCaptor.forClass(CourtDxCode.class);
        verify(courtDxCodeRepository, atMost(1)).save(courtDxCodeArgumentCaptor.capture());
        ArgumentCaptor<CourtFax> courtFaxArgumentCaptor = ArgumentCaptor.forClass(CourtFax.class);
        verify(courtFaxRepository, atMost(2)).save(courtFaxArgumentCaptor.capture());

        // second call with same seed should generate same results
        testingSupportService.createCourt(courtName, seed, false);

        verify(courtService, times(2)).createCourt(any());

        // mandatory calls
        verify(courtAccessibilityOptionsRepository, times(2))
            .save(courtAccessibilityOptionsArgumentCaptor.getValue());
        verify(courtAreasOfLawRepository, times(2))
            .save(courtAreasOfLawArgumentCaptor.getValue());
        verify(courtContactDetailsRepository, times(2))
            .save(courtContactDetailsArgumentCaptor.getValue());
        verify(courtCounterServiceOpeningHoursRepository, times(2))
            .save(courtCounterServiceOpeningHoursArgumentCaptor.getValue());
        verify(courtFacilitiesRepository, times(2))
            .save(courtFacilitiesArgumentCaptor.getValue());
        verify(courtProfessionalInformationRepository, times(2))
            .save(courtProfessionalInformationArgumentCaptor.getValue());
        verify(courtServiceAreasRepository, times(2))
            .save(courtServiceAreasArgumentCaptor.getValue());

        // optional or multiple calls
        courtAddressArgumentCaptor.getAllValues().forEach(v -> {
            verify(courtAddressRepository, times(2)).save(v);
        });
        courtOpeningHoursArgumentCaptor.getAllValues().forEach(v -> {
            verify(courtOpeningHoursRepository, times(2)).save(v);
        });
        courtLocalAuthoritiesArgumentCaptor.getAllValues().forEach(v -> {
            verify(courtLocalAuthoritiesRepository, times(2)).save(v);
        });
        courtTranslationArgumentCaptor.getAllValues().forEach(v -> {
            verify(courtTranslationRepository, times(2)).save(v);
        });
        courtPhotoArgumentCaptor.getAllValues().forEach(v -> {
            verify(courtPhotoRepository, times(2)).save(v);
        });
        courtSinglePointsOfEntryArgumentCaptor.getAllValues().forEach(v -> {
            verify(courtSinglePointsOfEntryRepository, times(2)).save(v);
        });
        courtCodesArgumentCaptor.getAllValues().forEach(v -> {
            verify(courtCodesRepository, times(2)).save(v);
        });
        courtDxCodeArgumentCaptor.getAllValues().forEach(v -> {
            verify(courtDxCodeRepository, times(2)).save(v);
        });
        courtFaxArgumentCaptor.getAllValues().forEach(v -> {
            verify(courtFaxRepository, times(2)).save(v);
        });
    }
}
