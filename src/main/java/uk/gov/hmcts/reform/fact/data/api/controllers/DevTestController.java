package uk.gov.hmcts.reform.fact.data.api.controllers;

import uk.gov.hmcts.reform.fact.data.api.entities.AreaOfLawType;
import uk.gov.hmcts.reform.fact.data.api.entities.Audit;
import uk.gov.hmcts.reform.fact.data.api.entities.ContactDescriptionType;
import uk.gov.hmcts.reform.fact.data.api.entities.Court;
import uk.gov.hmcts.reform.fact.data.api.entities.CourtAccessibilityOptions;
import uk.gov.hmcts.reform.fact.data.api.entities.CourtAddress;
import uk.gov.hmcts.reform.fact.data.api.entities.CourtAreasOfLaw;
import uk.gov.hmcts.reform.fact.data.api.entities.CourtCodes;
import uk.gov.hmcts.reform.fact.data.api.entities.CourtContactDetails;
import uk.gov.hmcts.reform.fact.data.api.entities.CourtCounterServiceOpeningHours;
import uk.gov.hmcts.reform.fact.data.api.entities.CourtDxCode;
import uk.gov.hmcts.reform.fact.data.api.entities.CourtFacility;
import uk.gov.hmcts.reform.fact.data.api.entities.CourtFax;
import uk.gov.hmcts.reform.fact.data.api.entities.CourtLocalAuthorities;
import uk.gov.hmcts.reform.fact.data.api.entities.CourtLock;
import uk.gov.hmcts.reform.fact.data.api.entities.CourtOpeningTime;
import uk.gov.hmcts.reform.fact.data.api.entities.CourtPhoto;
import uk.gov.hmcts.reform.fact.data.api.entities.CourtPostcode;
import uk.gov.hmcts.reform.fact.data.api.entities.CourtProfessionalInformation;
import uk.gov.hmcts.reform.fact.data.api.entities.CourtServiceAreas;
import uk.gov.hmcts.reform.fact.data.api.entities.CourtSinglePointsOfEntry;
import uk.gov.hmcts.reform.fact.data.api.entities.CourtTranslation;
import uk.gov.hmcts.reform.fact.data.api.entities.CourtType;
import uk.gov.hmcts.reform.fact.data.api.entities.LocalAuthorityType;
import uk.gov.hmcts.reform.fact.data.api.entities.OpeningHourType;
import uk.gov.hmcts.reform.fact.data.api.entities.Region;
import uk.gov.hmcts.reform.fact.data.api.entities.ServiceArea;
import uk.gov.hmcts.reform.fact.data.api.entities.User;
import uk.gov.hmcts.reform.fact.data.api.repositories.AreaOfLawTypeRepository;
import uk.gov.hmcts.reform.fact.data.api.repositories.AuditRepository;
import uk.gov.hmcts.reform.fact.data.api.repositories.ContactDescriptionTypeRepository;
import uk.gov.hmcts.reform.fact.data.api.repositories.CourtAccessibilityOptionsRepository;
import uk.gov.hmcts.reform.fact.data.api.repositories.CourtAddressRepository;
import uk.gov.hmcts.reform.fact.data.api.repositories.CourtAreasOfLawRepository;
import uk.gov.hmcts.reform.fact.data.api.repositories.CourtCodesRepository;
import uk.gov.hmcts.reform.fact.data.api.repositories.CourtContactDetailsRepository;
import uk.gov.hmcts.reform.fact.data.api.repositories.CourtCounterServiceOpeningHoursRepository;
import uk.gov.hmcts.reform.fact.data.api.repositories.CourtDxCodeRepository;
import uk.gov.hmcts.reform.fact.data.api.repositories.CourtFacilityRepository;
import uk.gov.hmcts.reform.fact.data.api.repositories.CourtFaxRepository;
import uk.gov.hmcts.reform.fact.data.api.repositories.CourtLocalAuthoritiesRepository;
import uk.gov.hmcts.reform.fact.data.api.repositories.CourtLockRepository;
import uk.gov.hmcts.reform.fact.data.api.repositories.CourtOpeningTimeRepository;
import uk.gov.hmcts.reform.fact.data.api.repositories.CourtPhotoRepository;
import uk.gov.hmcts.reform.fact.data.api.repositories.CourtPostcodeRepository;
import uk.gov.hmcts.reform.fact.data.api.repositories.CourtProfessionalInformationRepository;
import uk.gov.hmcts.reform.fact.data.api.repositories.CourtRepository;
import uk.gov.hmcts.reform.fact.data.api.repositories.CourtServiceAreasRepository;
import uk.gov.hmcts.reform.fact.data.api.repositories.CourtSinglePointsOfEntryRepository;
import uk.gov.hmcts.reform.fact.data.api.repositories.CourtTranslationRepository;
import uk.gov.hmcts.reform.fact.data.api.repositories.CourtTypeRepository;
import uk.gov.hmcts.reform.fact.data.api.repositories.LocalAuthorityTypeRepository;
import uk.gov.hmcts.reform.fact.data.api.repositories.OpeningHourTypeRepository;
import uk.gov.hmcts.reform.fact.data.api.repositories.RegionRepository;
import uk.gov.hmcts.reform.fact.data.api.repositories.ServiceAreaRepository;
import uk.gov.hmcts.reform.fact.data.api.repositories.UserRepository;

import java.util.Arrays;
import java.util.List;
import java.util.function.UnaryOperator;

import jakarta.validation.Valid;
import lombok.Generated;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Test controller for entity testing during dev.
 */
@Slf4j
@RestController
@RequestMapping(path = "/dev", produces = {MediaType.APPLICATION_JSON_VALUE})
@RequiredArgsConstructor
@Profile("dev")
@Validated
@Generated // tells jacoco/sonar/etc to look away
public class DevTestController {

    @GetMapping(value = "/generate/")
    public ResponseEntity<String> writeYourOwnCode() {
        var template = """

                // ------------------------------------------------------------------------
                // {class}
                // ------------------------------------------------------------------------

                private final {class}Repository {camelClass}Repository;

                @PostMapping(value = "/{path}/", consumes = MediaType.APPLICATION_JSON_VALUE)
                public ResponseEntity<{class}> create{class}(@Valid @RequestBody {class} {camelClass}) {
                    return ResponseEntity.ok({camelClass}Repository.save({camelClass}));
                }

                @GetMapping("/{path}/")
                public ResponseEntity<List<{class}>> getAll{class}() {
                    return ResponseEntity.ok().body({camelClass}Repository.findAll());
                }

            """;

        var classes = Arrays.asList(
            "AreaOfLawType",
            "Audit",
            "ContactDescriptionType",
            "Court",
            "CourtAccessibilityOptions",
            "CourtAddress",
            "CourtAreasOfLaw",
            "CourtCodes",
            "CourtContactDetails",
            "CourtCounterServiceOpeningHours",
            "CourtDxCode",
            "CourtFacility",
            "CourtFax",
            "CourtLocalAuthorities",
            "CourtLock",
            "CourtOpeningTime",
            "CourtPhoto",
            "CourtPostcode",
            "CourtProfessionalInformation",
            "CourtServiceAreas",
            "CourtSinglePointsOfEntry",
            "CourtTranslation",
            "CourtType",
            "LocalAuthorityType",
            "OpeningHourType",
            "Region",
            "ServiceArea",
            "User"
        );

        UnaryOperator<String> makeCamel = (String s) -> s != null && !s.isBlank()
            ? s.substring(0, 1).toLowerCase() + s.substring(1)
            : s;

        var camelClasses = classes.stream().map(makeCamel).toList();

        var b = new StringBuilder();
        for (var i = 0; i < classes.size(); i++) {
            var frag = template;
            frag = frag.replace("{class}", classes.get(i));
            frag = frag.replace("{camelClass}", camelClasses.get(i));
            frag = frag.replace("{path}", classes.get(i).toLowerCase());
            b.append(frag);
        }
        return ResponseEntity.ok(b.toString());
    }

    // ------------------------------------------------------------------------
    // AreaOfLawType
    // ------------------------------------------------------------------------

    private final AreaOfLawTypeRepository areaOfLawTypeRepository;

    @PostMapping(value = "/areaoflawtype/", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<AreaOfLawType> createAreaOfLawType(@Valid @RequestBody AreaOfLawType areaOfLawType) {
        return ResponseEntity.ok(areaOfLawTypeRepository.save(areaOfLawType));
    }

    @GetMapping("/areaoflawtype/")
    public ResponseEntity<List<AreaOfLawType>> getAllAreaOfLawType() {
        return ResponseEntity.ok().body(areaOfLawTypeRepository.findAll());
    }


    // ------------------------------------------------------------------------
    // Audit
    // ------------------------------------------------------------------------

    private final AuditRepository auditRepository;

    @PostMapping(value = "/audit/", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Audit> createAudit(@Valid @RequestBody Audit audit) {
        return ResponseEntity.ok(auditRepository.save(audit));
    }

    @GetMapping("/audit/")
    public ResponseEntity<List<Audit>> getAllAudit() {
        return ResponseEntity.ok().body(auditRepository.findAll());
    }


    // ------------------------------------------------------------------------
    // ContactDescriptionType
    // ------------------------------------------------------------------------

    private final ContactDescriptionTypeRepository contactDescriptionTypeRepository;

    @PostMapping(value = "/contactdescriptiontype/", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ContactDescriptionType> createContactDescriptionType(
        @Valid @RequestBody ContactDescriptionType contactDescriptionType) {
        return ResponseEntity.ok(contactDescriptionTypeRepository.save(contactDescriptionType));
    }

    @GetMapping("/contactdescriptiontype/")
    public ResponseEntity<List<ContactDescriptionType>> getAllContactDescriptionType() {
        return ResponseEntity.ok().body(contactDescriptionTypeRepository.findAll());
    }


    // ------------------------------------------------------------------------
    // Court
    // ------------------------------------------------------------------------

    private final CourtRepository courtRepository;

    @PostMapping(value = "/court/", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Court> createCourt(@Valid @RequestBody Court court) {
        return ResponseEntity.ok(courtRepository.save(court));
    }

    @GetMapping("/court/")
    public ResponseEntity<List<Court>> getAllCourt() {
        return ResponseEntity.ok().body(courtRepository.findAll());
    }


    // ------------------------------------------------------------------------
    // CourtAccessibilityOptions
    // ------------------------------------------------------------------------

    private final CourtAccessibilityOptionsRepository courtAccessibilityOptionsRepository;

    @PostMapping(value = "/courtaccessibilityoptions/", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<CourtAccessibilityOptions> createCourtAccessibilityOptions(
        @Valid @RequestBody CourtAccessibilityOptions courtAccessibilityOptions) {
        return ResponseEntity.ok(courtAccessibilityOptionsRepository.save(courtAccessibilityOptions));
    }

    @GetMapping("/courtaccessibilityoptions/")
    public ResponseEntity<List<CourtAccessibilityOptions>> getAllCourtAccessibilityOptions() {
        return ResponseEntity.ok().body(courtAccessibilityOptionsRepository.findAll());
    }


    // ------------------------------------------------------------------------
    // CourtAddress
    // ------------------------------------------------------------------------

    private final CourtAddressRepository courtAddressRepository;

    @PostMapping(value = "/courtaddress/", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<CourtAddress> createCourtAddress(@Valid @RequestBody CourtAddress courtAddress) {
        return ResponseEntity.ok(courtAddressRepository.save(courtAddress));
    }

    @GetMapping("/courtaddress/")
    public ResponseEntity<List<CourtAddress>> getAllCourtAddress() {
        return ResponseEntity.ok().body(courtAddressRepository.findAll());
    }


    // ------------------------------------------------------------------------
    // CourtAreasOfLaw
    // ------------------------------------------------------------------------

    private final CourtAreasOfLawRepository courtAreasOfLawRepository;

    @PostMapping(value = "/courtareasoflaw/", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<CourtAreasOfLaw> createCourtAreasOfLaw(@Valid @RequestBody CourtAreasOfLaw courtAreasOfLaw) {
        return ResponseEntity.ok(courtAreasOfLawRepository.save(courtAreasOfLaw));
    }

    @GetMapping("/courtareasoflaw/")
    public ResponseEntity<List<CourtAreasOfLaw>> getAllCourtAreasOfLaw() {
        return ResponseEntity.ok().body(courtAreasOfLawRepository.findAll());
    }


    // ------------------------------------------------------------------------
    // CourtCodes
    // ------------------------------------------------------------------------

    private final CourtCodesRepository courtCodesRepository;

    @PostMapping(value = "/courtcodes/", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<CourtCodes> createCourtCodes(@Valid @RequestBody CourtCodes courtCodes) {
        return ResponseEntity.ok(courtCodesRepository.save(courtCodes));
    }

    @GetMapping("/courtcodes/")
    public ResponseEntity<List<CourtCodes>> getAllCourtCodes() {
        return ResponseEntity.ok().body(courtCodesRepository.findAll());
    }


    // ------------------------------------------------------------------------
    // CourtContactDetails
    // ------------------------------------------------------------------------

    private final CourtContactDetailsRepository courtContactDetailsRepository;

    @PostMapping(value = "/courtcontactdetails/", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<CourtContactDetails> createCourtContactDetails(
        @Valid @RequestBody CourtContactDetails courtContactDetails) {
        return ResponseEntity.ok(courtContactDetailsRepository.save(courtContactDetails));
    }

    @GetMapping("/courtcontactdetails/")
    public ResponseEntity<List<CourtContactDetails>> getAllCourtContactDetails() {
        return ResponseEntity.ok().body(courtContactDetailsRepository.findAll());
    }


    // ------------------------------------------------------------------------
    // CourtCounterServiceOpeningHours
    // ------------------------------------------------------------------------

    private final CourtCounterServiceOpeningHoursRepository courtCounterServiceOpeningHoursRepository;

    @PostMapping(value = "/courtcounterserviceopeninghours/", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<CourtCounterServiceOpeningHours> createCourtCounterServiceOpeningHours(
        @Valid @RequestBody CourtCounterServiceOpeningHours courtCounterServiceOpeningHours) {
        return ResponseEntity.ok(courtCounterServiceOpeningHoursRepository.save(courtCounterServiceOpeningHours));
    }

    @GetMapping("/courtcounterserviceopeninghours/")
    public ResponseEntity<List<CourtCounterServiceOpeningHours>> getAllCourtCounterServiceOpeningHours() {
        return ResponseEntity.ok().body(courtCounterServiceOpeningHoursRepository.findAll());
    }


    // ------------------------------------------------------------------------
    // CourtDxCode
    // ------------------------------------------------------------------------

    private final CourtDxCodeRepository courtDxCodeRepository;

    @PostMapping(value = "/courtdxcode/", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<CourtDxCode> createCourtDxCode(@Valid @RequestBody CourtDxCode courtDxCode) {
        return ResponseEntity.ok(courtDxCodeRepository.save(courtDxCode));
    }

    @GetMapping("/courtdxcode/")
    public ResponseEntity<List<CourtDxCode>> getAllCourtDxCode() {
        return ResponseEntity.ok().body(courtDxCodeRepository.findAll());
    }


    // ------------------------------------------------------------------------
    // CourtFacility
    // ------------------------------------------------------------------------

    private final CourtFacilityRepository courtFacilityRepository;

    @PostMapping(value = "/courtfacility/", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<CourtFacility> createCourtFacility(@Valid @RequestBody CourtFacility courtFacility) {
        return ResponseEntity.ok(courtFacilityRepository.save(courtFacility));
    }

    @GetMapping("/courtfacility/")
    public ResponseEntity<List<CourtFacility>> getAllCourtFacility() {
        return ResponseEntity.ok().body(courtFacilityRepository.findAll());
    }


    // ------------------------------------------------------------------------
    // CourtFax
    // ------------------------------------------------------------------------

    private final CourtFaxRepository courtFaxRepository;

    @PostMapping(value = "/courtfax/", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<CourtFax> createCourtFax(@Valid @RequestBody CourtFax courtFax) {
        return ResponseEntity.ok(courtFaxRepository.save(courtFax));
    }

    @GetMapping("/courtfax/")
    public ResponseEntity<List<CourtFax>> getAllCourtFax() {
        return ResponseEntity.ok().body(courtFaxRepository.findAll());
    }


    // ------------------------------------------------------------------------
    // CourtLocalAuthorities
    // ------------------------------------------------------------------------

    private final CourtLocalAuthoritiesRepository courtLocalAuthoritiesRepository;

    @PostMapping(value = "/courtlocalauthorities/", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<CourtLocalAuthorities> createCourtLocalAuthorities(
        @Valid @RequestBody CourtLocalAuthorities courtLocalAuthorities) {
        return ResponseEntity.ok(courtLocalAuthoritiesRepository.save(courtLocalAuthorities));
    }

    @GetMapping("/courtlocalauthorities/")
    public ResponseEntity<List<CourtLocalAuthorities>> getAllCourtLocalAuthorities() {
        return ResponseEntity.ok().body(courtLocalAuthoritiesRepository.findAll());
    }


    // ------------------------------------------------------------------------
    // CourtLock
    // ------------------------------------------------------------------------

    private final CourtLockRepository courtLockRepository;

    @PostMapping(value = "/courtlock/", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<CourtLock> createCourtLock(@Valid @RequestBody CourtLock courtLock) {
        return ResponseEntity.ok(courtLockRepository.save(courtLock));
    }

    @GetMapping("/courtlock/")
    public ResponseEntity<List<CourtLock>> getAllCourtLock() {
        return ResponseEntity.ok().body(courtLockRepository.findAll());
    }


    // ------------------------------------------------------------------------
    // CourtOpeningTime
    // ------------------------------------------------------------------------

    private final CourtOpeningTimeRepository courtOpeningTimeRepository;

    @PostMapping(value = "/courtopeningtime/", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<CourtOpeningTime> createCourtOpeningTime(
        @Valid @RequestBody CourtOpeningTime courtOpeningTime) {
        return ResponseEntity.ok(courtOpeningTimeRepository.save(courtOpeningTime));
    }

    @GetMapping("/courtopeningtime/")
    public ResponseEntity<List<CourtOpeningTime>> getAllCourtOpeningTime() {
        return ResponseEntity.ok().body(courtOpeningTimeRepository.findAll());
    }


    // ------------------------------------------------------------------------
    // CourtPhoto
    // ------------------------------------------------------------------------

    private final CourtPhotoRepository courtPhotoRepository;

    @PostMapping(value = "/courtphoto/", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<CourtPhoto> createCourtPhoto(@Valid @RequestBody CourtPhoto courtPhoto) {
        return ResponseEntity.ok(courtPhotoRepository.save(courtPhoto));
    }

    @GetMapping("/courtphoto/")
    public ResponseEntity<List<CourtPhoto>> getAllCourtPhoto() {
        return ResponseEntity.ok().body(courtPhotoRepository.findAll());
    }


    // ------------------------------------------------------------------------
    // CourtPostcode
    // ------------------------------------------------------------------------

    private final CourtPostcodeRepository courtPostcodeRepository;

    @PostMapping(value = "/courtpostcode/", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<CourtPostcode> createCourtPostcode(@Valid @RequestBody CourtPostcode courtPostcode) {
        return ResponseEntity.ok(courtPostcodeRepository.save(courtPostcode));
    }

    @GetMapping("/courtpostcode/")
    public ResponseEntity<List<CourtPostcode>> getAllCourtPostcode() {
        return ResponseEntity.ok().body(courtPostcodeRepository.findAll());
    }


    // ------------------------------------------------------------------------
    // CourtProfessionalInformation
    // ------------------------------------------------------------------------

    private final CourtProfessionalInformationRepository courtProfessionalInformationRepository;

    @PostMapping(value = "/courtprofessionalinformation/", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<CourtProfessionalInformation> createCourtProfessionalInformation(
        @Valid @RequestBody CourtProfessionalInformation courtProfessionalInformation) {
        return ResponseEntity.ok(courtProfessionalInformationRepository.save(courtProfessionalInformation));
    }

    @GetMapping("/courtprofessionalinformation/")
    public ResponseEntity<List<CourtProfessionalInformation>> getAllCourtProfessionalInformation() {
        return ResponseEntity.ok().body(courtProfessionalInformationRepository.findAll());
    }


    // ------------------------------------------------------------------------
    // CourtServiceAreas
    // ------------------------------------------------------------------------

    private final CourtServiceAreasRepository courtServiceAreasRepository;

    @PostMapping(value = "/courtserviceareas/", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<CourtServiceAreas> createCourtServiceAreas(
        @Valid @RequestBody CourtServiceAreas courtServiceAreas) {
        return ResponseEntity.ok(courtServiceAreasRepository.save(courtServiceAreas));
    }

    @GetMapping("/courtserviceareas/")
    public ResponseEntity<List<CourtServiceAreas>> getAllCourtServiceAreas() {
        return ResponseEntity.ok().body(courtServiceAreasRepository.findAll());
    }


    // ------------------------------------------------------------------------
    // CourtSinglePointsOfEntry
    // ------------------------------------------------------------------------

    private final CourtSinglePointsOfEntryRepository courtSinglePointsOfEntryRepository;

    @PostMapping(value = "/courtsinglepointsofentry/", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<CourtSinglePointsOfEntry> createCourtSinglePointsOfEntry(
        @Valid @RequestBody CourtSinglePointsOfEntry courtSinglePointsOfEntry) {
        return ResponseEntity.ok(courtSinglePointsOfEntryRepository.save(courtSinglePointsOfEntry));
    }

    @GetMapping("/courtsinglepointsofentry/")
    public ResponseEntity<List<CourtSinglePointsOfEntry>> getAllCourtSinglePointsOfEntry() {
        return ResponseEntity.ok().body(courtSinglePointsOfEntryRepository.findAll());
    }


    // ------------------------------------------------------------------------
    // CourtTranslation
    // ------------------------------------------------------------------------

    private final CourtTranslationRepository courtTranslationRepository;

    @PostMapping(value = "/courttranslation/", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<CourtTranslation> createCourtTranslation(
        @Valid @RequestBody CourtTranslation courtTranslation) {
        return ResponseEntity.ok(courtTranslationRepository.save(courtTranslation));
    }

    @GetMapping("/courttranslation/")
    public ResponseEntity<List<CourtTranslation>> getAllCourtTranslation() {
        return ResponseEntity.ok().body(courtTranslationRepository.findAll());
    }


    // ------------------------------------------------------------------------
    // CourtType
    // ------------------------------------------------------------------------

    private final CourtTypeRepository courtTypeRepository;

    @PostMapping(value = "/courttype/", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<CourtType> createCourtType(@Valid @RequestBody CourtType courtType) {
        return ResponseEntity.ok(courtTypeRepository.save(courtType));
    }

    @GetMapping("/courttype/")
    public ResponseEntity<List<CourtType>> getAllCourtType() {
        return ResponseEntity.ok().body(courtTypeRepository.findAll());
    }


    // ------------------------------------------------------------------------
    // LocalAuthorityType
    // ------------------------------------------------------------------------

    private final LocalAuthorityTypeRepository localAuthorityTypeRepository;

    @PostMapping(value = "/localauthoritytype/", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<LocalAuthorityType> createLocalAuthorityType(
        @Valid @RequestBody LocalAuthorityType localAuthorityType) {
        return ResponseEntity.ok(localAuthorityTypeRepository.save(localAuthorityType));
    }

    @GetMapping("/localauthoritytype/")
    public ResponseEntity<List<LocalAuthorityType>> getAllLocalAuthorityType() {
        return ResponseEntity.ok().body(localAuthorityTypeRepository.findAll());
    }


    // ------------------------------------------------------------------------
    // OpeningHourType
    // ------------------------------------------------------------------------

    private final OpeningHourTypeRepository openingHourTypeRepository;

    @PostMapping(value = "/openinghourtype/", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<OpeningHourType> createOpeningHourType(@Valid @RequestBody OpeningHourType openingHourType) {
        return ResponseEntity.ok(openingHourTypeRepository.save(openingHourType));
    }

    @GetMapping("/openinghourtype/")
    public ResponseEntity<List<OpeningHourType>> getAllOpeningHourType() {
        return ResponseEntity.ok().body(openingHourTypeRepository.findAll());
    }


    // ------------------------------------------------------------------------
    // Region
    // ------------------------------------------------------------------------

    private final RegionRepository regionRepository;

    @PostMapping(value = "/region/", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Region> createRegion(@Valid @RequestBody Region region) {
        return ResponseEntity.ok(regionRepository.save(region));
    }

    @GetMapping("/region/")
    public ResponseEntity<List<Region>> getAllRegion() {
        return ResponseEntity.ok().body(regionRepository.findAll());
    }


    // ------------------------------------------------------------------------
    // ServiceArea
    // ------------------------------------------------------------------------

    private final ServiceAreaRepository serviceAreaRepository;

    @PostMapping(value = "/servicearea/", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ServiceArea> createServiceArea(@Valid @RequestBody ServiceArea serviceArea) {
        return ResponseEntity.ok(serviceAreaRepository.save(serviceArea));
    }

    @GetMapping("/servicearea/")
    public ResponseEntity<List<ServiceArea>> getAllServiceArea() {
        return ResponseEntity.ok().body(serviceAreaRepository.findAll());
    }


    // ------------------------------------------------------------------------
    // User
    // ------------------------------------------------------------------------

    private final UserRepository userRepository;

    @PostMapping(value = "/user/", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<User> createUser(@Valid @RequestBody User user) {
        return ResponseEntity.ok(userRepository.save(user));
    }

    @GetMapping("/user/")
    public ResponseEntity<List<User>> getAllUser() {
        return ResponseEntity.ok().body(userRepository.findAll());
    }

}
