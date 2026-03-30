package uk.gov.hmcts.reform.fact.data.api.services;

import uk.gov.hmcts.reform.fact.data.api.dto.CourtCodesDto;
import uk.gov.hmcts.reform.fact.data.api.dto.CourtDxCodeDto;
import uk.gov.hmcts.reform.fact.data.api.dto.CourtFaxDto;
import uk.gov.hmcts.reform.fact.data.api.dto.CourtProfessionalInformationDetailsDto;
import uk.gov.hmcts.reform.fact.data.api.dto.ProfessionalInformationDto;
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
import uk.gov.hmcts.reform.fact.data.api.entities.types.AddressType;
import uk.gov.hmcts.reform.fact.data.api.entities.types.AllowedLocalAuthorityAreasOfLaw;
import uk.gov.hmcts.reform.fact.data.api.entities.types.CatchmentType;
import uk.gov.hmcts.reform.fact.data.api.entities.types.DayOfTheWeek;
import uk.gov.hmcts.reform.fact.data.api.entities.types.HearingEnhancementEquipment;
import uk.gov.hmcts.reform.fact.data.api.entities.types.OpeningTimesDetail;
import uk.gov.hmcts.reform.fact.data.api.migration.model.InMemoryMultipartFile;
import uk.gov.hmcts.reform.fact.data.api.models.AreaOfLawSelectionDto;
import uk.gov.hmcts.reform.fact.data.api.models.CourtLocalAuthorityDto;
import uk.gov.hmcts.reform.fact.data.api.models.LocalAuthoritySelectionDto;
import uk.gov.hmcts.reform.fact.data.api.repositories.AreaOfLawTypeRepository;
import uk.gov.hmcts.reform.fact.data.api.repositories.ContactDescriptionTypeRepository;
import uk.gov.hmcts.reform.fact.data.api.repositories.CourtPhotoRepository;
import uk.gov.hmcts.reform.fact.data.api.repositories.CourtServiceAreasRepository;
import uk.gov.hmcts.reform.fact.data.api.repositories.CourtTypeRepository;
import uk.gov.hmcts.reform.fact.data.api.repositories.LocalAuthorityTypeRepository;
import uk.gov.hmcts.reform.fact.data.api.repositories.OpeningHoursTypeRepository;
import uk.gov.hmcts.reform.fact.data.api.repositories.RegionRepository;
import uk.gov.hmcts.reform.fact.data.api.repositories.ServiceAreaRepository;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.font.FontRenderContext;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import javax.imageio.ImageIO;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.Synchronized;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

@Service
@ConditionalOnProperty(prefix = "testingSupport", name = "enableApi", havingValue = "true")
@RequiredArgsConstructor
@Slf4j
@SuppressWarnings("java:S2245") // test helper uses seeded pseudorandom data intentionally
public class TestingSupportService {

    // used to generate alphanumeric strings
    private static final char[] ALPHA_NUMERICS = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789".toCharArray();

    // ------------------------------------------------------------------------
    // static data caches - we load these once and then reuse for all generated courts

    private static final List<UUID> REGION_IDS = new ArrayList<>();
    private static final List<AreaOfLawType> AREAS_OF_LAW = new ArrayList<>();
    private static final List<CourtType> COURT_TYPES = new ArrayList<>();
    private static final List<UUID> CONTACT_DESCRIPTION_IDS = new ArrayList<>();
    private static final List<UUID> LOCAL_AUTHORITY_TYPE_IDS = new ArrayList<>();
    private static final List<UUID> OPENING_HOUR_TYPE_IDS = new ArrayList<>();
    private static final List<UUID> SERVICE_AREA_IDS = new ArrayList<>();

    // ------------------------------------------------------------------------
    // Data variations for randomisation

    private static final List<String> ACCESSIBLE_TOILET_DESCRIPTIONS = List.of(
        "Accessible toilet available on the ground floor.",
        "Wheelchair-accessible toilet located near the main entrance.",
        "Adapted toilet with support rails in the public area.",
        "Accessible toilet with emergency assistance alarm.",
        "Disabled toilet with wide door and low sink facilities."
    );

    private static final List<String> CONTACT_DESCRIPTION_VALUES = List.of(
        "General enquiries and information",
        "Urgent case support and assistance",
        "Technical support for online services",
        "Accessibility and facilities information",
        "Feedback and complaints handling"
    );

    public static final List<String> WARNING_NOTICE_VALUES = List.of(
        "Court temporarily closed for maintenance",
        "Limited access due to ongoing construction",
        "Hearing delays expected due to staff shortages",
        "Parking restrictions in effect",
        "COVID-19 safety protocols in place"
    );

    private static final List<String> ADDRESS_LINE_1 = List.of(
        "1 Main Street", "2 High Road", "3 Market Place", "4 Station Avenue", "5 Park Lane",
        "6 Church Street", "7 Victoria Road", "8 King Street", "9 Queen's Parade", "10 Mill Lane"
    );

    private static final List<String> ADDRESS_LINE_2 = List.of(
        "Suite 101", "Flat 2B", "Building 3", "Unit 4", "Floor 5",
        "Block A", "Room 6", "Annex 7", "Office 8", "Wing 9"
    );

    private static final Map<String, String> LOCATION_MAP = Map.of(
        "London", "Greater London", "Manchester", "Greater Manchester",
        "Birmingham", "West Midlands", "Leeds", "West Yorkshire",
        "Liverpool", "Merseyside", "Sheffield", "South Yorkshire",
        "Bristol", "City of Bristol", "Nottingham", "Nottinghamshire",
        "Leicester", "Leicestershire", "Newcastle", "Tyne and Wear"
    );

    private static final List<String> POST_CODES = List.of(
        "TQ7 2NB", "SR2 8SP", "S35 1QG", "GL51 9QP", "LU2 7ED",
        "YO26 7NY", "W12 7AQ", "DA12 5DU", "HU5 4QJ", "GL19 4DQ", "GU22 9BT", "IP30 0JG",
        "BN13 3BS", "N8 0FT", "N7 8EP", "RM17 5WX", "M12 5GW",
        "BH8 9PL", "NG19 9JN", "LS27 8GQ", "E3 2BP", "ST5 6LR", "WA13 0AE", "SY7 9AE",
        "YO41 1AZ", "IP28 7JH", "WA4 9YH", "LS14 3LA", "SO51 8JL", "WS7 0BN", "OX10 6AS",
        "HR2 8RR", "BA9 9ET", "LE3 3PH", "UB6 7EP", "BH25 6NA", "NN11 4LN", "WR3 7DD",
        "EX34 9HE", "SE27 9BW", "TF7 9SD", "B17 0HR", "SA1 8DX", "DE73 8AY",
        "OX28 1EH", "BL1 2DJ", "ST18 0XX", "LE2 7BA", "SP7 9PX"
    );

    private static final List<String> EMAIL_NAMES = List.of(
        "maria", "thomas", "elena", "gary", "muriel",
        "nathan", "rachel", "luke", "admin", "support", "operations",
        "services", "facilities", "team", "coordination"
    );

    private static final List<String> EMAIL_DOMAINS = List.of(
        "hmcts.org", "justice.gov.uk", "tribunalnetwork.org", "outsourcedjustice.com"
    );

    // ------------------------------------------------------------------------
    // Repositories and services required to insert the data

    // Court Details repo is used for initial insertion and final retrieval
    private final CourtService courtService;
    private final CourtAccessibilityOptionsService courtAccessibilityOptionsService;
    private final CourtAddressService courtAddressService;
    private final CourtAreasOfLawService courtAreasOfLawService;
    private final CourtContactDetailsService courtContactDetailsService;
    private final CourtOpeningHoursService courtOpeningHoursService;
    private final CourtFacilitiesService courtFacilitiesService;
    private final CourtSinglePointsOfEntryService courtSinglePointsOfEntryService;
    private final CourtTranslationService courtTranslationService;
    private final CourtProfessionalInformationService courtProfessionalInformationService;
    private final CourtLocalAuthoritiesService courtLocalAuthoritiesService;
    private final CourtServiceAreasRepository courtServiceAreasRepository;
    // for photos, we try the service first, then fall back to the repository if required
    private final CourtPhotoService courtPhotoService;
    private final CourtPhotoRepository courtPhotoRepository;

    // ------------------------------------------------------------------------
    // Services and repositories required for supporting data

    private final AreaOfLawTypeRepository areaOfLawTypeRepository;
    private final ContactDescriptionTypeRepository contactDescriptionTypeRepository;
    private final CourtTypeRepository courtTypeRepository;
    private final LocalAuthorityTypeRepository localAuthorityTypeRepository;
    private final OpeningHoursTypeRepository openingHoursTypeRepository;
    private final RegionRepository regionRepository;
    private final ServiceAreaRepository serviceAreaRepository;

    /**
     * Creates a court with the specified name and randomised data for all other fields. The randomisation is based on
     * the supplied seed (if provided), or the current system time if no seed is supplied.
     *
     * @param courtName     the name of the court to create
     * @param seed          the seed for randomisation - if not provided, a random seed will be used
     * @param serviceCentre whether the court should be marked as a service centre (true/false)
     * @return the slug of the created court entry
     **/
    public String createCourt(
        @NonNull String courtName,
        Long seed,
        boolean serviceCentre,
        boolean open,
        boolean addWarningNotice
    ) {
        return createCourt(courtName, seed, serviceCentre, open, addWarningNotice, true);
    }

    public String createCourt(
        @NonNull String courtName,
        Long seed,
        boolean serviceCentre,
        boolean open,
        boolean addWarningNotice,
        boolean withTranslations
    ) {
        return createCourt(courtName, seed, serviceCentre, open, addWarningNotice, withTranslations, true, false);
    }

    // Suppressing the "too many params" warning for now as this is a test setup
    // though it's probably a good idea to convert this to a context object if
    // we add many more
    @SuppressWarnings("java:S107")
    public String createCourt(
        @NonNull String courtName,
        Long seed,
        boolean serviceCentre,
        boolean open,
        boolean addWarningNotice,
        boolean withTranslations,
        boolean withEnquiriesContact,
        boolean associateServiceAreas
    ) {
        initialiseCaches();

        Random random = new Random(Optional.ofNullable(seed).orElse(System.currentTimeMillis()));

        Court court = createCourt(courtName, serviceCentre, addWarningNotice, random);
        UUID courtId = court.getId();
        List<AreaOfLawType> areasOfLaw = setAreasOfLaw(courtId, random);
        List<CourtType> courtTypes = COURT_TYPES.stream().filter(l -> random.nextBoolean()).toList();
        if (courtTypes.isEmpty()) {
            courtTypes = List.of(COURT_TYPES.get(random.nextInt(COURT_TYPES.size())));
        }

        setAccessibilityOptions(courtId, random);
        setAddresses(courtId, areasOfLaw, random);
        if (open) {
            openCourt(court);
        }
        setContactDetails(courtId, random, withEnquiriesContact);
        setCounterServiceOpeningHours(courtId, courtTypes, random);
        setFacilities(courtId, random);
        setLocalAuthorities(courtId, areasOfLaw, random);
        setOpeningHours(courtId, random);
        setProfessionalInformation(courtId, random);
        if(associateServiceAreas) {
            setServiceAreas(courtId, random);
        }
        setSinglePointsOfEntry(courtId, areasOfLaw, random);
        setTranslations(courtId, random, withTranslations);
        setPhotos(courtId, courtName);

        // return the unique slug for the created court
        return court.getSlug();
    }

    private Court createCourt(String name, boolean serviceCenter, boolean addWarningNotice, Random random) {
        Court court = Court.builder()
            .name(name)
            .isServiceCentre(serviceCenter)
            .regionId(REGION_IDS.get(random.nextInt(REGION_IDS.size())))
            .mrdId(rndAlphaNumeric(random.nextInt(16, 32), random))
            .open(false)
            .openOnCath(false)
            .warningNotice(
                addWarningNotice
                    ? WARNING_NOTICE_VALUES.get(random.nextInt(WARNING_NOTICE_VALUES.size()))
                    : null
            )
            .build();

        return courtService.createCourt(court);
    }

    private void openCourt(final Court court) {
        court.setOpen(true);
        courtService.updateCourt(court.getId(), court);
    }

    @Synchronized
    private void initialiseCaches() {
        // using region ids as the gate here
        if (REGION_IDS.isEmpty()) {
            REGION_IDS.addAll(regionRepository.findAll().stream().map(Region::getId).toList());
            AREAS_OF_LAW.addAll(areaOfLawTypeRepository.findAll().stream().toList());
            COURT_TYPES.addAll(courtTypeRepository.findAll());
            CONTACT_DESCRIPTION_IDS.addAll(contactDescriptionTypeRepository.findAll().stream().map(
                ContactDescriptionType::getId).toList()
            );
            LOCAL_AUTHORITY_TYPE_IDS.addAll(localAuthorityTypeRepository.findAll().stream().map(
                LocalAuthorityType::getId).toList()
            );
            OPENING_HOUR_TYPE_IDS.addAll(openingHoursTypeRepository.findAll().stream().map(
                OpeningHourType::getId).toList()
            );
            SERVICE_AREA_IDS.addAll(serviceAreaRepository.findAll().stream().map(ServiceArea::getId).toList());

            if (REGION_IDS.isEmpty() || AREAS_OF_LAW.isEmpty() || COURT_TYPES.isEmpty()
                || CONTACT_DESCRIPTION_IDS.isEmpty() || LOCAL_AUTHORITY_TYPE_IDS.isEmpty()
                || OPENING_HOUR_TYPE_IDS.isEmpty() || SERVICE_AREA_IDS.isEmpty()) {
                throw new IllegalStateException("Unable to initialize testing support service caches "
                                                    + "- one or more required reference data tables are empty");
            }
        }
    }

    private void setAccessibilityOptions(final UUID courtId, final Random random) {
        CourtAccessibilityOptions courtAccessibilityOptions = CourtAccessibilityOptions.builder()
            .courtId(courtId)
            .accessibleEntrance(random.nextBoolean())
            .accessibleParking(random.nextBoolean())
            .lift(random.nextBoolean())
            .quietRoom(random.nextBoolean())
            .hearingEnhancementEquipment(HearingEnhancementEquipment.values()[random.nextInt(
                HearingEnhancementEquipment.values().length)])
            .accessibleToiletDescription(ACCESSIBLE_TOILET_DESCRIPTIONS.get(random.nextInt(
                ACCESSIBLE_TOILET_DESCRIPTIONS.size())))
            .build();

        if (courtAccessibilityOptions.getAccessibleEntrance().booleanValue()) {
            courtAccessibilityOptions.setAccessibleEntrancePhoneNumber(rndPhoneNumber(random));
        }

        if (courtAccessibilityOptions.getAccessibleParking().booleanValue()) {
            courtAccessibilityOptions.setAccessibleParkingPhoneNumber(rndPhoneNumber(random));
        }

        if (courtAccessibilityOptions.getLift().booleanValue()) {
            courtAccessibilityOptions.setLiftDoorWidth(200 + random.nextInt(600));
            courtAccessibilityOptions.setLiftDoorLimit(3000 + random.nextInt(5000));
        }

        courtAccessibilityOptionsService.setAccessibilityOptions(courtId, courtAccessibilityOptions);
    }

    private void setAddresses(final UUID courtId, List<AreaOfLawType> areasOfLaw, final Random random) {

        if (random.nextBoolean()) {
            // do a pair visit us and write to us addresses
            CourtAddress visitUsAddress = rndAddress(courtId, AddressType.VISIT_US, areasOfLaw, random);
            courtAddressService.createAddress(courtId, visitUsAddress);
            CourtAddress writeToUsAddress = rndAddress(courtId, AddressType.WRITE_TO_US, areasOfLaw, random);
            courtAddressService.createAddress(courtId, writeToUsAddress);
        } else {
            // do a single main address
            CourtAddress mainAddress = rndAddress(courtId, AddressType.VISIT_OR_CONTACT_US, areasOfLaw, random);
            courtAddressService.createAddress(courtId, mainAddress);
        }

        if (random.nextBoolean()) {
            // bonus address!
            AddressType addressType = AddressType.values()[random.nextInt(AddressType.values().length)];
            List<AreaOfLawType> aol = areasOfLaw.stream().filter(l -> random.nextBoolean()).toList();
            CourtAddress bonusAddress = rndAddress(courtId, addressType, aol, random);
            courtAddressService.createAddress(courtId, bonusAddress);
        }
    }

    private List<AreaOfLawType> setAreasOfLaw(final UUID courtId, final Random random) {
        final List<AreaOfLawType> areasOfLaw = new ArrayList<>(
            AREAS_OF_LAW.stream()
                .filter(l -> random.nextInt(100) < 30)
                .toList()
        );

        if (areasOfLaw.isEmpty()) {
            areasOfLaw.add(AREAS_OF_LAW.get(random.nextInt(AREAS_OF_LAW.size())));
        }

        CourtAreasOfLaw courtAreasOfLaw = CourtAreasOfLaw.builder()
            .courtId(courtId)
            .areasOfLaw(areasOfLaw.stream().map(AreaOfLawType::getId).toList())
            .build();

        courtAreasOfLawService.setCourtAreasOfLaw(courtId, courtAreasOfLaw);

        return areasOfLaw;
    }

    private void setContactDetails(final UUID courtId, final Random random, final boolean withEnquiriesContact) {
        if (!withEnquiriesContact) {
            return;
        }

        CourtContactDetails courtContactDetails = CourtContactDetails.builder()
            .courtId(courtId)
            .courtContactDescriptionId(CONTACT_DESCRIPTION_IDS.get(random.nextInt(CONTACT_DESCRIPTION_IDS.size())))
            .explanation(CONTACT_DESCRIPTION_VALUES.get(random.nextInt(CONTACT_DESCRIPTION_VALUES.size())))
            .phoneNumber(rndPhoneNumber(random))
            .email(rndEmail(random))
            .build();
        courtContactDetailsService.createContactDetail(courtId, courtContactDetails);
    }

    private void setCounterServiceOpeningHours(final UUID courtId, List<CourtType> courtTypes, final Random random) {

        CourtCounterServiceOpeningHours openingHours = CourtCounterServiceOpeningHours.builder()
            .courtId(courtId)
            .appointmentNeeded(random.nextBoolean())
            .counterService(random.nextBoolean())
            .assistWithDocuments(random.nextBoolean())
            .assistWithForms(random.nextBoolean())
            .assistWithSupport(random.nextBoolean())
            .courtTypes(courtTypes.stream().map(CourtType::getId).toList())
            .build();

        if (openingHours.getAppointmentNeeded().booleanValue()) {
            if (random.nextBoolean()) {
                openingHours.setAppointmentContact(rndEmail(random));
            } else {
                openingHours.setAppointmentContact(rndPhoneNumber(random));
            }
        }

        setOpeningTimesDetails(random, openingHours::setOpeningTimesDetails);

        courtOpeningHoursService.setCounterServiceOpeningHours(courtId, openingHours);
    }

    private void setFacilities(final UUID courtId, final Random random) {
        CourtFacilities facilities = CourtFacilities.builder()
            .courtId(courtId)
            .cafeteria(random.nextBoolean())
            .freeWaterDispensers(random.nextBoolean())
            .wifi(random.nextBoolean())
            .babyChanging(random.nextBoolean())
            .parking(random.nextBoolean())
            .quietRoom(random.nextBoolean())
            .drinkVendingMachines(random.nextBoolean())
            .snackVendingMachines(random.nextBoolean())
            .waitingAreaChildren(random.nextBoolean())
            .waitingArea(random.nextBoolean())
            .build();

        courtFacilitiesService.setFacilities(courtId, facilities);
    }

    private void setLocalAuthorities(final UUID courtId, List<AreaOfLawType> areasOfLaw, final Random random) {
        if (random.nextBoolean()) {
            List<String> allowedAols = Arrays.stream(AllowedLocalAuthorityAreasOfLaw.values())
                .map(AllowedLocalAuthorityAreasOfLaw::getDisplayName).toList();
            List<AreaOfLawType> aolForLas = areasOfLaw.stream()
                .filter(aol -> allowedAols.contains(aol.getName()))
                .toList();
            // there's a chance that this court doesn't have AOLs that allow local authorities
            // if that's the case, we just skip setting local authorities for this court
            if (aolForLas.isEmpty()) {
                return;
            }

            List<CourtLocalAuthorityDto> courtLocalAuthorityDtos = new ArrayList<>();
            for (AreaOfLawType areaOfLawType : aolForLas) {
                List<UUID> laSelection = LOCAL_AUTHORITY_TYPE_IDS.stream().filter(l -> random.nextBoolean()).toList();
                if (laSelection.isEmpty()) {
                    laSelection = List.of(LOCAL_AUTHORITY_TYPE_IDS.get(
                        random.nextInt(LOCAL_AUTHORITY_TYPE_IDS.size()))
                    );
                }

                List<LocalAuthoritySelectionDto> courtLocalAuthorities = laSelection.stream()
                    .map(laId -> {
                        LocalAuthoritySelectionDto lasd = new LocalAuthoritySelectionDto();
                        lasd.setId(laId);
                        lasd.setName("Local Authority " + laId.toString().substring(0, 5));
                        lasd.setSelected(true);
                        return lasd;
                    })
                    .toList();

                courtLocalAuthorityDtos.add(CourtLocalAuthorityDto.from(areaOfLawType, courtLocalAuthorities));
            }

            courtLocalAuthoritiesService.setCourtLocalAuthorities(
                courtId,
                courtLocalAuthorityDtos
            );
        }
    }

    private void setOpeningHours(final UUID courtId, final Random random) {
        int count = OPENING_HOUR_TYPE_IDS.size() > 1 ? random.nextInt(1, OPENING_HOUR_TYPE_IDS.size()) : 1;
        ArrayList<UUID> ohtids = new ArrayList<>(OPENING_HOUR_TYPE_IDS);
        Collections.shuffle(ohtids, random);
        List<UUID> typeIds = ohtids.stream().limit(count).toList();
        for (int i = 0; i < count; i++) {
            CourtOpeningHours openingHours = CourtOpeningHours.builder()
                .courtId(courtId)
                .openingHourTypeId(typeIds.get(i))
                .build();

            setOpeningTimesDetails(random, openingHours::setOpeningTimesDetails);

            courtOpeningHoursService.setOpeningHours(courtId, openingHours);
        }
    }

    private void setPhotos(final UUID courtId, final String courtName) {
        // attempt to upload an actual image using the service. If that fails (e.g. due to storage issues),
        // fall back to directly saving a photo record with a placeholder link
        boolean actualUpload = true;
        try {
            InMemoryMultipartFile file = new InMemoryMultipartFile(
                "photo.png",
                "photo.png",
                "image/png",
                genTestImage(400, 400, courtName)
            );
            courtPhotoService.setCourtPhoto(courtId, file);
        } catch (Exception e) {
            log.warn("Error while uploading test image", e);
            actualUpload = false;
        }

        // fallback
        if (!actualUpload) {
            CourtPhoto photo = CourtPhoto.builder()
                .courtId(courtId)
                .fileLink("http://localhost:3000/photos/" + courtId.toString() + ".jpg")
                .build();
            courtPhotoRepository.save(photo);
        }
    }

    private void setProfessionalInformation(final UUID courtId, final Random random) {
        CourtProfessionalInformationDetailsDto dto = CourtProfessionalInformationDetailsDto.builder()
            .professionalInformation(createProfessionalInformation(random))
            .codes(createCodes(random).orElse(null))
            .dxCodes(createDxCodes(random))
            .faxNumbers(createFaxNumbers(random))
            .build();

        courtProfessionalInformationService.setProfessionalInformation(courtId, dto);
    }

    private ProfessionalInformationDto createProfessionalInformation(final Random random) {

        ProfessionalInformationDto dto = ProfessionalInformationDto.builder()
            .accessScheme(random.nextBoolean())
            .commonPlatform(random.nextBoolean())
            .videoHearings(random.nextBoolean())
            .interviewRooms(random.nextBoolean())
            .build();

        if (dto.getInterviewRooms().booleanValue()) {
            dto.setInterviewRoomCount(random.nextInt(1, 5));
            dto.setInterviewPhoneNumber(rndPhoneNumber(random));
        }

        return dto;
    }

    private Optional<CourtCodesDto> createCodes(final Random random) {
        if (random.nextBoolean()) {
            CourtCodesDto courtCodes = CourtCodesDto.builder()
                .build();

            if (random.nextBoolean()) {
                courtCodes.setMagistrateCourtCode(random.nextInt(1000));
            }

            if (random.nextBoolean()) {
                courtCodes.setFamilyCourtCode(random.nextInt(1000));
            }

            if (random.nextBoolean()) {
                courtCodes.setTribunalCode(random.nextInt(1000));
            }

            if (random.nextBoolean()) {
                courtCodes.setCountyCourtCode(random.nextInt(1000));
            }

            if (random.nextBoolean()) {
                courtCodes.setCrownCourtCode(random.nextInt(1000));
            }

            if (random.nextBoolean()) {
                courtCodes.setGbs(rndAlphaNumeric(10, random));
            }

            return Optional.of(courtCodes);
        }
        return Optional.empty();
    }

    private List<CourtDxCodeDto> createDxCodes(final Random random) {
        List<CourtDxCodeDto> dxCodes = new ArrayList<>();
        int dxCodeCount = random.nextInt(3);
        for (int i = 0; i < dxCodeCount; i++) {
            if (random.nextBoolean()) {
                CourtDxCodeDto code = CourtDxCodeDto.builder()
                    .dxCode(rndAlphaNumeric(6, random))
                    .explanation(COURT_TYPES.stream().findAny().map(CourtType::getName).orElse("General") + " DX code")
                    .build();
                dxCodes.add(code);
            }
        }
        return dxCodes;
    }

    private List<CourtFaxDto> createFaxNumbers(final Random random) {
        List<CourtFaxDto> faxNumbers = new ArrayList<>();
        int faxCount = random.nextInt(3);
        for (int i = 0; i < faxCount; i++) {
            faxNumbers.add(
                CourtFaxDto.builder()
                    .faxNumber(rndPhoneNumber(random))
                    .description(i == 0 ? "Fax number" : "Urgent documents fax number")
                    .build()
            );
        }
        return faxNumbers;
    }

    private void setServiceAreas(final UUID courtId, final Random random) {

        List<UUID> serviceAreaIds = SERVICE_AREA_IDS.stream().filter(id -> random.nextBoolean()).toList();
        if (serviceAreaIds.isEmpty()) {
            serviceAreaIds = List.of(SERVICE_AREA_IDS.get(random.nextInt(SERVICE_AREA_IDS.size())));
        }

        CourtServiceAreas serviceAreas = CourtServiceAreas.builder()
            .courtId(courtId)
            .serviceAreaId(serviceAreaIds)
            .catchmentType(CatchmentType.values()[random.nextInt(CatchmentType.values().length)])
            .build();

        courtServiceAreasRepository.save(serviceAreas);
    }

    private void setSinglePointsOfEntry(final UUID courtId, final List<AreaOfLawType> areasOfLaw, final Random random) {
        if (random.nextBoolean()) {
            List<String> allowedAols = Arrays.stream(AllowedLocalAuthorityAreasOfLaw.values())
                .map(AllowedLocalAuthorityAreasOfLaw::getDisplayName).toList();
            List<AreaOfLawType> aolForSPoE = areasOfLaw.stream()
                .filter(aol -> allowedAols.contains(aol.getName()))
                .filter(a -> random.nextBoolean())
                .toList();
            // if we didn't get any from the random selection, just pick one but still make sure it's an allowed value
            if (aolForSPoE.isEmpty()) {
                aolForSPoE = areasOfLaw.stream()
                    .filter(aol -> allowedAols.contains(aol.getName()))
                    .limit(1)
                    .toList();
            }
            // if we still don't have any (e.g. because the court doesn't have any areas of
            // law that are allowed for SPoE), then skip setting SPoE
            if (aolForSPoE.isEmpty()) {
                return;
            }

            List<AreaOfLawSelectionDto> dtos = aolForSPoE.stream()
                .map(AreaOfLawSelectionDto::from)
                .toList();
            dtos.forEach(dto -> dto.setSelected(true));
            courtSinglePointsOfEntryService.updateCourtSinglePointsOfEntry(courtId, dtos);
        }
    }

    private void setTranslations(final UUID courtId, final Random random, final boolean withTranslations) {
        if (withTranslations) {
            CourtTranslation translation = CourtTranslation.builder()
                .courtId(courtId)
                .email(rndEmail(random))
                .phoneNumber(rndPhoneNumber(random))
                .build();

            courtTranslationService.setTranslation(courtId, translation);
        }
    }

    // DRY

    private void setOpeningTimesDetails(final Random random, Consumer<List<OpeningTimesDetail>> openingHours) {
        if (random.nextBoolean()) {
            // every day
            openingHours.accept(rndOpeningTimesDetails(random, List.of(DayOfTheWeek.EVERYDAY)));
        } else {
            // specific days
            List<DayOfTheWeek> days = Arrays.asList(DayOfTheWeek.values()).stream()
                .filter(d -> d != DayOfTheWeek.EVERYDAY)
                .filter(b -> random.nextInt(100) > 30)
                .toList();
            if (days.isEmpty()) {
                days = List.of(DayOfTheWeek.values()[random.nextInt(DayOfTheWeek.values().length - 1)]);
            }
            openingHours.accept(rndOpeningTimesDetails(random, days));
        }
    }

    // random data generators

    private static String rndAlphaNumeric(int length, Random random) {
        return random.ints(length, 0, ALPHA_NUMERICS.length)
            .mapToObj(i -> ALPHA_NUMERICS[i])
            .collect(StringBuilder::new, StringBuilder::append, StringBuilder::append)
            .toString();
    }

    private static String rndPhoneNumber(Random random) {
        String code = new String[]{"020", "01xxx", "07xxx"}[random.nextInt(3)];
        while (code.contains("x")) {
            code = code.replaceFirst("x", String.valueOf(random.nextInt(10)));
        }
        return code
            + " "
            + random.ints(3, 0, 10).mapToObj(Integer::toString).collect(Collectors.joining())
            + " "
            + random.ints(3, 0, 10).mapToObj(Integer::toString).collect(Collectors.joining());
    }

    private static String rndEmail(Random random) {
        String name = EMAIL_NAMES.get(random.nextInt(EMAIL_NAMES.size()));
        String domain = EMAIL_DOMAINS.get(random.nextInt(EMAIL_DOMAINS.size()));
        return name + "@" + domain;
    }

    private static CourtAddress rndAddress(
        final UUID courtId,
        final AddressType addressType,
        final List<AreaOfLawType> areasOfLaw,
        final Random random) {
        String city = LOCATION_MAP.keySet().stream().toList().get(random.nextInt(LOCATION_MAP.size()));
        List<UUID> courtTypes = COURT_TYPES.stream().filter(l -> random.nextBoolean()).map(CourtType::getId).toList();
        if (courtTypes.isEmpty()) {
            courtTypes = List.of(COURT_TYPES.get(random.nextInt(COURT_TYPES.size())).getId());
        }
        return CourtAddress.builder()
            .courtId(courtId)
            .addressLine1(ADDRESS_LINE_1.get(random.nextInt(ADDRESS_LINE_1.size())))
            .addressLine2(ADDRESS_LINE_2.get(random.nextInt(ADDRESS_LINE_2.size())))
            .townCity(city)
            .county(LOCATION_MAP.get(city))
            .postcode(POST_CODES.get(random.nextInt(POST_CODES.size())))
            .addressType(addressType)
            .areasOfLaw(areasOfLaw.stream().map(AreaOfLawType::getId).toList())
            .courtTypes(courtTypes)
            .epimId(rndAlphaNumeric(random.nextInt(5, 10), random))
            .lat(BigDecimal.valueOf(random.nextDouble()))
            .lon(BigDecimal.valueOf(random.nextDouble()))
            .build();
    }

    private List<OpeningTimesDetail> rndOpeningTimesDetails(final Random random, final List<DayOfTheWeek> days) {
        LocalTime openingTime = LocalTime.of(random.nextInt(6, 11), random.nextBoolean() ? 0 : 30);
        LocalTime closingTime = LocalTime.of(random.nextInt(12, 19), random.nextBoolean() ? 0 : 30);
        return days.stream().map(day -> OpeningTimesDetail.builder()
            .openingTime(openingTime)
            .closingTime(closingTime)
            .dayOfWeek(day)
            .build()
        ).toList();
    }

    // img generation for testing photo upload.
    private byte[] genTestImage(final int width, final int height, String courtName) throws IOException {

        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = image.createGraphics();

        try {
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);

            // Clear
            g2.setComposite(AlphaComposite.Clear);
            g2.fillRect(0, 0, width, height);
            g2.setComposite(AlphaComposite.SrcOver);

            // Start with a big size and scale down
            int fontSize = 64;
            Font baseFont = new Font("SansSerif", Font.BOLD, fontSize);

            // Measure and adapt font size to fit within margins
            int margin = 12;
            FontRenderContext frc = g2.getFontRenderContext();
            Rectangle2D bounds;
            Font font = baseFont;

            // Reduce font size until the text fits within width - 2*margin
            while (fontSize > 10) {
                font = baseFont.deriveFont((float) fontSize);
                bounds = font.getStringBounds(courtName, frc);
                if (bounds.getWidth() <= (width - 2 * margin) && bounds.getHeight() <= (height - 2 * margin)) {
                    break;
                }
                fontSize -= 2;
            }

            g2.setColor(Color.BLACK);
            g2.setFont(font);

            // compute bounds and center the text
            bounds = g2.getFontMetrics().getStringBounds(courtName, g2);
            double textWidth = bounds.getWidth();
            double textHeight = bounds.getHeight();
            int ascent = g2.getFontMetrics().getAscent();

            // work out where to put it so it's centered within the image
            int x = (int) Math.round((width - textWidth) / 2.0);
            int y = (int) Math.round((height - textHeight) / 2.0 + ascent);

            g2.drawString(courtName, x, y);

        } finally {
            g2.dispose();
        }

        // Encode to PNG bytes
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            if (!ImageIO.write(image, "png", baos)) {
                throw new IOException("No appropriate PNG writer found.");
            }
            return baos.toByteArray();
        }
    }

}
