package uk.gov.hmcts.reform.fact.data.api.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.atMost;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import uk.gov.hmcts.reform.fact.data.api.dto.CourtProfessionalInformationDetailsDto;
import uk.gov.hmcts.reform.fact.data.api.entities.AreaOfLawType;
import uk.gov.hmcts.reform.fact.data.api.entities.ContactDescriptionType;
import uk.gov.hmcts.reform.fact.data.api.entities.Court;
import uk.gov.hmcts.reform.fact.data.api.entities.CourtAccessibilityOptions;
import uk.gov.hmcts.reform.fact.data.api.entities.CourtAddress;
import uk.gov.hmcts.reform.fact.data.api.entities.CourtAreasOfLaw;
import uk.gov.hmcts.reform.fact.data.api.entities.CourtContactDetails;
import uk.gov.hmcts.reform.fact.data.api.entities.CourtCounterServiceOpeningHours;
import uk.gov.hmcts.reform.fact.data.api.entities.CourtFacilities;
import uk.gov.hmcts.reform.fact.data.api.entities.CourtOpeningHours;
import uk.gov.hmcts.reform.fact.data.api.entities.CourtPhoto;
import uk.gov.hmcts.reform.fact.data.api.entities.CourtServiceAreas;
import uk.gov.hmcts.reform.fact.data.api.entities.CourtTranslation;
import uk.gov.hmcts.reform.fact.data.api.entities.CourtType;
import uk.gov.hmcts.reform.fact.data.api.entities.LocalAuthorityType;
import uk.gov.hmcts.reform.fact.data.api.entities.OpeningHourType;
import uk.gov.hmcts.reform.fact.data.api.entities.Region;
import uk.gov.hmcts.reform.fact.data.api.entities.ServiceArea;
import uk.gov.hmcts.reform.fact.data.api.entities.types.AllowedLocalAuthorityAreasOfLaw;
import uk.gov.hmcts.reform.fact.data.api.models.AreaOfLawSelectionDto;
import uk.gov.hmcts.reform.fact.data.api.models.CourtLocalAuthorityDto;
import uk.gov.hmcts.reform.fact.data.api.repositories.AreaOfLawTypeRepository;
import uk.gov.hmcts.reform.fact.data.api.repositories.ContactDescriptionTypeRepository;
import uk.gov.hmcts.reform.fact.data.api.repositories.CourtPhotoRepository;
import uk.gov.hmcts.reform.fact.data.api.repositories.CourtServiceAreasRepository;
import uk.gov.hmcts.reform.fact.data.api.repositories.CourtTypeRepository;
import uk.gov.hmcts.reform.fact.data.api.repositories.LocalAuthorityTypeRepository;
import uk.gov.hmcts.reform.fact.data.api.repositories.OpeningHoursTypeRepository;
import uk.gov.hmcts.reform.fact.data.api.repositories.RegionRepository;
import uk.gov.hmcts.reform.fact.data.api.repositories.ServiceAreaRepository;

import java.util.List;
import java.util.Random;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class TestingSupportServiceTest {

    @Mock
    private CourtService courtService;
    @Mock
    private CourtAccessibilityOptionsService courtAccessibilityOptionsService;
    @Mock
    private CourtAddressService courtAddressService;
    @Mock
    private CourtAreasOfLawService courtAreasOfLawService;
    @Mock
    private CourtContactDetailsService courtContactDetailsService;
    @Mock
    private CourtOpeningHoursService courtOpeningHoursService;
    @Mock
    private CourtFacilitiesService courtFacilitiesService;
    @Mock
    private CourtSinglePointsOfEntryService courtSinglePointsOfEntryService;
    @Mock
    private CourtTranslationService courtTranslationService;
    @Mock
    private CourtProfessionalInformationService courtProfessionalInformationService;
    @Mock
    private CourtLocalAuthoritiesService courtLocalAuthoritiesService;
    @Mock
    private CourtServiceAreasRepository courtServiceAreasRepository;
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
    private ServiceAreaRepository serviceAreaRepository;

    @InjectMocks
    private TestingSupportService testingSupportService;

    // Randomised UUID lists for the type repos
    private final List<UUID> regionIds = randomUuidList();
    private final List<AreaOfLawType> areasOfLaw = randomAreasOfLaw();


    private final List<CourtType> courtTypes = randomCourtTypes();
    private final List<UUID> contactDescIds = randomUuidList();
    private final List<UUID> localAuthorityIds = randomUuidList();
    private final List<UUID> openingHourTypeIds = randomUuidList();
    private final List<UUID> serviceAreaIds = randomUuidList();

    private static List<AreaOfLawType> randomAreasOfLaw() {
        Random random = new Random();
        int size = random.nextInt(20) + 1;
        List<AreaOfLawType> types = new java.util.ArrayList<>();
        for (int i = 0; i < size; i++) {
            if (i == 0) {
                types.add(
                    AreaOfLawType.builder()
                        .name(AllowedLocalAuthorityAreasOfLaw.displayNames().get(random.nextInt(
                            AllowedLocalAuthorityAreasOfLaw.displayNames().size())))
                        .displayName(UUID.randomUUID().toString())
                        .id(UUID.randomUUID())
                        .build()
                );
            } else {
                types.add(
                    AreaOfLawType.builder()
                        .name(UUID.randomUUID().toString())
                        .displayName(UUID.randomUUID().toString())
                        .id(UUID.randomUUID())
                        .build()
                );
            }
        }
        return types;
    }

    private static List<CourtType> randomCourtTypes() {
        Random random = new Random();
        int size = random.nextInt(20) + 1;
        List<CourtType> types = new java.util.ArrayList<>();
        for (int i = 0; i < size; i++) {
            types.add(
                CourtType.builder()
                    .name(UUID.randomUUID().toString())
                    .id(UUID.randomUUID())
                    .build()
            );
        }
        return types;
    }

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
        lenient().when(areaOfLawTypeRepository.findAll()).thenReturn(areasOfLaw);
        // CourtTypeRepository
        lenient().when(courtTypeRepository.findAll()).thenReturn(courtTypes);
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

        String result = testingSupportService.createCourt(courtName, null, false, false, true);

        assertNotNull(result);
        assertEquals("test-court", result);

        // things that are always called once
        verify(courtService, times(1)).createCourt(any());
        verify(courtAccessibilityOptionsService, times(1)).setAccessibilityOptions(any(), any());
        verify(courtAreasOfLawService, times(1)).setCourtAreasOfLaw(any(), any());
        verify(courtContactDetailsService, times(1)).createContactDetail(any(), any());
        verify(courtOpeningHoursService, times(1)).setCounterServiceOpeningHours(any(), any());
        verify(courtFacilitiesService, times(1)).setFacilities(any(), any());
        verify(courtProfessionalInformationService, times(1)).setProfessionalInformation(any(), any());
        verify(courtPhotoService, times(1)).setCourtPhoto(any(), any());

        // things that are called at least once
        verify(courtOpeningHoursService, atLeast(1)).setOpeningHours(any(), any());
        verify(courtAddressService, atLeast(1)).createAddress(any(), any());

        // things that are optional, but have a ceiling
        verify(courtServiceAreasRepository, atMost(1)).save(any());
        verify(courtLocalAuthoritiesService, atMost(1)).setCourtLocalAuthorities(any(), any());
        verify(courtTranslationService, times(1)).setTranslation(any(), any());
        verify(courtPhotoRepository, atMost(1)).save(any());
        verify(courtSinglePointsOfEntryService, atMost(1)).updateCourtSinglePointsOfEntry(any(), any());
    }

    @Test
    void createCourtWithWarningNotice() {
        String courtName = "Test Court";
        when(courtService.createCourt(any())).thenAnswer(inv -> {
            Court c = Court.class.cast(inv.getArguments()[0]);
            c.setSlug("test-court");
            return c;
        });

        String result = testingSupportService.createCourt(courtName, null, false, false, true);

        assertNotNull(result);
        assertEquals("test-court", result);

        ArgumentCaptor<Court> captor = ArgumentCaptor.forClass(Court.class);
        verify(courtService, times(1)).createCourt(captor.capture());
        assertThat(captor.getValue().getWarningNotice()).isNotNull();
    }

    @Test
    void createCourtWithoutWarningNotice() {
        String courtName = "Test Court";
        when(courtService.createCourt(any())).thenAnswer(inv -> {
            Court c = Court.class.cast(inv.getArguments()[0]);
            c.setSlug("test-court");
            return c;
        });

        String result = testingSupportService.createCourt(courtName, null, false, false, false);

        assertNotNull(result);
        assertEquals("test-court", result);

        ArgumentCaptor<Court> captor = ArgumentCaptor.forClass(Court.class);
        verify(courtService, times(1)).createCourt(captor.capture());
        assertThat(captor.getValue().getWarningNotice()).isNull();
    }

    @Test
    void createCourtWithEmptyNameThrowsException() {
        String courtName = "";
        assertThrows(
            NullPointerException.class,
            () -> testingSupportService.createCourt(courtName, null, false, false, true)
        );
    }

    @Test
    void createCourtWithoutTranslationsSkipsTranslationService() {
        String courtName = "Test Court";
        when(courtService.createCourt(any())).thenAnswer(inv -> {
            Court c = Court.class.cast(inv.getArguments()[0]);
            c.setSlug("test-court");
            return c;
        });

        String result = testingSupportService.createCourt(courtName, null, false, false, true, false);

        assertNotNull(result);
        assertEquals("test-court", result);
        verify(courtTranslationService, never()).setTranslation(any(), any());
    }

    @Test
    void createCourtWithoutEnquiriesContactSkipsContactCreation() {
        String courtName = "Test Court";
        when(courtService.createCourt(any())).thenAnswer(inv -> {
            Court c = Court.class.cast(inv.getArguments()[0]);
            c.setSlug("test-court");
            return c;
        });

        String result = testingSupportService.createCourt(courtName, null, false, false, true, true, false, false);

        assertNotNull(result);
        assertEquals("test-court", result);
        verify(courtContactDetailsService, never()).createContactDetail(any(), any());
    }


    @Captor
    private ArgumentCaptor<List<AreaOfLawSelectionDto>> aolSelectionDtoArgumentCaptor;
    @Captor
    private ArgumentCaptor<List<CourtLocalAuthorityDto>> courtLocalAuthorityDtoArgumentCaptor;

    @RepeatedTest(20)
    @SuppressWarnings("java:S5961")
    void createCourtWithSeedGeneratesConsistentResults() {
        String courtName = "Seeded Court";
        UUID courtId = UUID.randomUUID();
        long seed = System.nanoTime();

        when(courtService.createCourt(any())).thenAnswer(inv -> {
            Court c = Court.class.cast(inv.getArguments()[0]);
            c.setSlug("test-court");
            c.setId(courtId);
            return c;
        });

        testingSupportService.createCourt(courtName, seed, false, false, true);

        // things that are always called once
        verify(courtService, times(1)).createCourt(any());

        ArgumentCaptor<CourtAccessibilityOptions> courtAccessibilityOptionsArgumentCaptor =
            ArgumentCaptor.forClass(CourtAccessibilityOptions.class);
        verify(courtAccessibilityOptionsService, times(1))
            .setAccessibilityOptions(eq(courtId), courtAccessibilityOptionsArgumentCaptor.capture());
        ArgumentCaptor<CourtAreasOfLaw> courtAreasOfLawArgumentCaptor =
            ArgumentCaptor.forClass(CourtAreasOfLaw.class);
        verify(courtAreasOfLawService, times(1))
            .setCourtAreasOfLaw(eq(courtId), courtAreasOfLawArgumentCaptor.capture());
        ArgumentCaptor<CourtContactDetails> courtContactDetailsArgumentCaptor =
            ArgumentCaptor.forClass(CourtContactDetails.class);
        verify(courtContactDetailsService, times(1))
            .createContactDetail(eq(courtId), courtContactDetailsArgumentCaptor.capture());
        ArgumentCaptor<CourtCounterServiceOpeningHours> courtCounterServiceOpeningHoursArgumentCaptor =
            ArgumentCaptor.forClass(CourtCounterServiceOpeningHours.class);
        verify(courtOpeningHoursService, times(1))
            .setCounterServiceOpeningHours(eq(courtId), courtCounterServiceOpeningHoursArgumentCaptor.capture());
        ArgumentCaptor<CourtFacilities> courtFacilitiesArgumentCaptor =
            ArgumentCaptor.forClass(CourtFacilities.class);
        verify(courtFacilitiesService, times(1))
            .setFacilities(eq(courtId), courtFacilitiesArgumentCaptor.capture());
        ArgumentCaptor<CourtProfessionalInformationDetailsDto> courtProfessionalInformationArgumentCaptor =
            ArgumentCaptor.forClass(CourtProfessionalInformationDetailsDto.class);
        verify(courtProfessionalInformationService, times(1))
            .setProfessionalInformation(eq(courtId), courtProfessionalInformationArgumentCaptor.capture());

        // can't check court photo

        // things that are called at least once
        ArgumentCaptor<CourtAddress> courtAddressArgumentCaptor = ArgumentCaptor.forClass(CourtAddress.class);
        verify(courtAddressService, atLeast(1)).createAddress(eq(courtId), courtAddressArgumentCaptor.capture());
        ArgumentCaptor<CourtOpeningHours> courtOpeningHoursArgumentCaptor =
            ArgumentCaptor.forClass(CourtOpeningHours.class);
        verify(courtOpeningHoursService, atLeast(1))
            .setOpeningHours(eq(courtId), courtOpeningHoursArgumentCaptor.capture());

        // things that are optional, but only called once if they are called
        verify(courtLocalAuthoritiesService, atMost(1))
            .setCourtLocalAuthorities(eq(courtId), courtLocalAuthorityDtoArgumentCaptor.capture());
        ArgumentCaptor<CourtTranslation> courtTranslationArgumentCaptor =
            ArgumentCaptor.forClass(CourtTranslation.class);
        verify(courtTranslationService, times(1))
            .setTranslation(eq(courtId), courtTranslationArgumentCaptor.capture());
        ArgumentCaptor<CourtPhoto> courtPhotoArgumentCaptor = ArgumentCaptor.forClass(CourtPhoto.class);
        verify(courtPhotoRepository, atMost(1)).save(courtPhotoArgumentCaptor.capture());
        verify(courtSinglePointsOfEntryService, atMost(1))
            .updateCourtSinglePointsOfEntry(eq(courtId), aolSelectionDtoArgumentCaptor.capture());
        ArgumentCaptor<CourtServiceAreas> courtServiceAreasArgumentCaptor =
            ArgumentCaptor.forClass(CourtServiceAreas.class);
        verify(courtServiceAreasRepository, atMost(1))
            .save(courtServiceAreasArgumentCaptor.capture());

        // second call with same seed should generate same results
        testingSupportService.createCourt(courtName, seed, false, false, true);

        verify(courtService, times(2)).createCourt(any());

        // mandatory calls
        verify(courtAccessibilityOptionsService, times(2))
            .setAccessibilityOptions(courtId, courtAccessibilityOptionsArgumentCaptor.getValue());
        verify(courtAreasOfLawService, times(2))
            .setCourtAreasOfLaw(courtId, courtAreasOfLawArgumentCaptor.getValue());
        verify(courtContactDetailsService, times(2))
            .createContactDetail(courtId, courtContactDetailsArgumentCaptor.getValue());
        verify(courtOpeningHoursService, times(2))
            .setCounterServiceOpeningHours(courtId, courtCounterServiceOpeningHoursArgumentCaptor.getValue());
        verify(courtFacilitiesService, times(2))
            .setFacilities(courtId, courtFacilitiesArgumentCaptor.getValue());
        verify(courtProfessionalInformationService, times(2))
            .setProfessionalInformation(courtId, courtProfessionalInformationArgumentCaptor.getValue());

        // optional or multiple calls
        courtAddressArgumentCaptor.getAllValues().forEach(v ->
                                                              verify(courtAddressService, times(2)).createAddress(
                                                                  courtId,
                                                                  v
                                                              )
        );
        courtOpeningHoursArgumentCaptor.getAllValues().forEach(v ->
                                                                   verify(
                                                                       courtOpeningHoursService,
                                                                       times(2)
                                                                   ).setOpeningHours(courtId, v)
        );
        courtLocalAuthorityDtoArgumentCaptor.getAllValues().forEach(v ->
                                                                        verify(
                                                                            courtLocalAuthoritiesService,
                                                                            times(2)
                                                                        ).setCourtLocalAuthorities(courtId, v)
        );
        courtTranslationArgumentCaptor.getAllValues().forEach(v ->
                                                                  verify(
                                                                      courtTranslationService,
                                                                      times(2)
                                                                  ).setTranslation(courtId, v)
        );
        courtPhotoArgumentCaptor.getAllValues().forEach(v ->
                                                            verify(courtPhotoRepository, times(2)).save(v)
        );
        aolSelectionDtoArgumentCaptor.getAllValues().forEach(v ->
                                                                 verify(
                                                                     courtSinglePointsOfEntryService,
                                                                     times(2)
                                                                 ).updateCourtSinglePointsOfEntry(courtId, v)
        );
        courtServiceAreasArgumentCaptor.getAllValues().forEach(v -> verify(
            courtServiceAreasRepository,
            times(2)
        ).save(v));
    }
}
