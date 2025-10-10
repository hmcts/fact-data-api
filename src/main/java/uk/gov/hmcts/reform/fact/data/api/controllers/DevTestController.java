package uk.gov.hmcts.reform.fact.data.api.controllers;

import uk.gov.hmcts.reform.fact.data.api.entities.Audit;
import uk.gov.hmcts.reform.fact.data.api.entities.Court;
import uk.gov.hmcts.reform.fact.data.api.entities.CourtAddress;
import uk.gov.hmcts.reform.fact.data.api.entities.CourtTranslation;
import uk.gov.hmcts.reform.fact.data.api.entities.Region;
import uk.gov.hmcts.reform.fact.data.api.entities.User;
import uk.gov.hmcts.reform.fact.data.api.errorhandling.exceptions.NotFoundException;
import uk.gov.hmcts.reform.fact.data.api.repositories.AuditRepository;
import uk.gov.hmcts.reform.fact.data.api.repositories.CourtAddressRepository;
import uk.gov.hmcts.reform.fact.data.api.repositories.UserRepository;
import uk.gov.hmcts.reform.fact.data.api.services.CourtService;
import uk.gov.hmcts.reform.fact.data.api.services.RegionService;
import uk.gov.hmcts.reform.fact.data.api.services.TranslationService;

import java.util.List;
import java.util.UUID;

import jakarta.validation.Valid;
import lombok.Generated;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

/**
 * Test controller for entity testing during dev.
 */
@RestController
@RequestMapping(
    path = "/dev",
    produces = {MediaType.APPLICATION_JSON_VALUE}
)
@RequiredArgsConstructor
@Profile("dev")
@Validated
@Generated // tells jacoco/sonar/etc to look away
public class DevTestController {

    // services
    private final CourtService courtService;
    private final RegionService regionService;
    private final TranslationService translationService;

    // repos - shortcut during development
    private final UserRepository userRepository;

    // ------------------------------------------------------------------------
    // Court
    // ------------------------------------------------------------------------

    @PostMapping(value = "/court/", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<UUID> createCourt(@Valid @RequestBody Court court) {
        // save it and return the location
        var result = courtService.create(court);
        var location = ServletUriComponentsBuilder.fromCurrentRequest().path("/{id}")
            .buildAndExpand(result).toUri();
        return ResponseEntity.created(location).body(result.getId());
    }

    @GetMapping("/court/")
    public ResponseEntity<List<Court>> getAllCourts() {
        return ResponseEntity.ok().body(courtService.retrieveAll());
    }

    @GetMapping("/court/full")
    public ResponseEntity<List<Court>> getAllCourtsFullView() {
        return ResponseEntity.ok().body(courtService.retrieveAll());
    }

    @GetMapping("/court/{id}")
    public ResponseEntity<Court> getCourtById(@PathVariable UUID id) throws NotFoundException {
        return ResponseEntity.ok().body(courtService.retrieve(id));
    }

    // ------------------------------------------------------------------------
    // Region
    // ------------------------------------------------------------------------

    @PostMapping(value = "/region/", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<UUID> createRegion(@Valid @RequestBody Region region) {
        // save it and return location
        var result = regionService.create(region);
        var location = ServletUriComponentsBuilder.fromCurrentRequest().path("/{id}")
            .buildAndExpand(result).toUri();
        return ResponseEntity.created(location).body(result.getId());
    }

    @GetMapping("/region/")
    public ResponseEntity<List<Region>> getAllRegions() {
        return ResponseEntity.ok().body(regionService.retrieveAll());
    }

    @GetMapping("/region/{id}")
    public ResponseEntity<Region> getRegionById(@PathVariable UUID id) throws NotFoundException {
        return ResponseEntity.ok().body(regionService.retrieve(id));
    }

    // ------------------------------------------------------------------------
    // Translation
    // ------------------------------------------------------------------------

    @PostMapping(value = "/translation/", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<UUID> createTranslation(@Valid @RequestBody CourtTranslation courtTranslation) {
        // save and return the location
        var result = translationService.create(courtTranslation);
        var location = ServletUriComponentsBuilder.fromCurrentRequest().path("/{id}")
            .buildAndExpand(result).toUri();
        return ResponseEntity.created(location).body(result.getId());
    }

    @GetMapping("/translation/")
    public ResponseEntity<List<CourtTranslation>> getAllTranslations() {
        return ResponseEntity.ok().body(translationService.retrieveAll());
    }

    @GetMapping("/translation/{id}")
    public ResponseEntity<CourtTranslation> getTranslationById(@PathVariable UUID id) throws NotFoundException {
        return ResponseEntity.ok().body(translationService.retrieve(id));
    }

    // ------------------------------------------------------------------------
    // User
    // ------------------------------------------------------------------------

    @PostMapping(value = "/user/", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<UUID> createUser(@Valid @RequestBody User user) {
        // save it and return location
        var result = userRepository.save(user);
        var location = ServletUriComponentsBuilder.fromCurrentRequest().path("/{id}")
            .buildAndExpand(result).toUri();
        return ResponseEntity.created(location).body(result.getId());
    }

    @GetMapping("/user/")
    public ResponseEntity<List<User>> getAllUsers() {
        return ResponseEntity.ok().body(userRepository.findAll());
    }

    @GetMapping("/user/{id}")
    public ResponseEntity<User> getUserById(@PathVariable UUID id) {
        return ResponseEntity.of(userRepository.findById(id));
    }

    // ------------------------------------------------------------------------
    // LocalAuthorityType
    // ------------------------------------------------------------------------


    // ------------------------------------------------------------------------
    // CourtLocalAuthority
    // ------------------------------------------------------------------------


    // ------------------------------------------------------------------------
    // AreaOfLawType
    // ------------------------------------------------------------------------


    // ------------------------------------------------------------------------
    // Audit
    // ------------------------------------------------------------------------
    private final AuditRepository auditRepository;

    @PostMapping(value = "/audit/", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<UUID> createAudit(@Valid @RequestBody Audit audit) {
        var result = auditRepository.save(audit);
        var location = ServletUriComponentsBuilder.fromCurrentRequest().path("/{id}")
            .buildAndExpand(result).toUri();
        return ResponseEntity.created(location).body(result.getId());
    }

    @GetMapping("/audit/")
    public ResponseEntity<List<Audit>> getAllAudits() {
        return ResponseEntity.ok().body(auditRepository.findAll());
    }

    // ------------------------------------------------------------------------
    //  ContactDescriptionType
    // ------------------------------------------------------------------------


    // ------------------------------------------------------------------------
    // CourtContactDetail
    // ------------------------------------------------------------------------


    // ------------------------------------------------------------------------
    // CourtAccessibilityOption
    // ------------------------------------------------------------------------

    // ------------------------------------------------------------------------
    // CourtAddress
    // ------------------------------------------------------------------------

    private final CourtAddressRepository courtAddressRepository;

    @PostMapping(value = "/court-address/", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<UUID> createCourtAddress(@Valid @RequestBody CourtAddress courtAddress) {
        // save it and return location
        var result = courtAddressRepository.save(courtAddress);
        var location = ServletUriComponentsBuilder.fromCurrentRequest().path("/{id}")
            .buildAndExpand(result).toUri();
        return ResponseEntity.created(location).body(result.getId());
    }

    @GetMapping("/court-address/")
    public ResponseEntity<List<CourtAddress>> getAllCourtAddress() {
        return ResponseEntity.ok().body(courtAddressRepository.findAll());
    }

    @GetMapping("/court-address/{id}")
    public ResponseEntity<CourtAddress> getCourtAddressById(@PathVariable UUID id) {
        return ResponseEntity.of(courtAddressRepository.findById(id));
    }

    // ------------------------------------------------------------------------
    // CourtCode
    // ------------------------------------------------------------------------


    // ------------------------------------------------------------------------
    // CourtCounterServiceOpeningHours
    // ------------------------------------------------------------------------


    // ------------------------------------------------------------------------
    // CourtDxCode
    // ------------------------------------------------------------------------


    // ------------------------------------------------------------------------
    // CourtFacility
    // ------------------------------------------------------------------------


    // ------------------------------------------------------------------------
    // CourtFax
    // ------------------------------------------------------------------------


    // ------------------------------------------------------------------------
    // CourtLock
    // ------------------------------------------------------------------------


    // ------------------------------------------------------------------------
    // CourtOpeningTime
    // ------------------------------------------------------------------------


    // ------------------------------------------------------------------------
    // CourtPhoto
    // ------------------------------------------------------------------------


    // ------------------------------------------------------------------------
    // CourtPostcode
    // ------------------------------------------------------------------------


    // ------------------------------------------------------------------------
    // CourtProfessionalInformation
    // ------------------------------------------------------------------------


    // ------------------------------------------------------------------------
    // ServiceArea
    // ------------------------------------------------------------------------


    // ------------------------------------------------------------------------
    // CourtServiceArea
    // ------------------------------------------------------------------------


    // ------------------------------------------------------------------------
    // CourtSinglePointsOfEntry
    // ------------------------------------------------------------------------


    // ------------------------------------------------------------------------
    // CourtType
    // ------------------------------------------------------------------------


    // ------------------------------------------------------------------------
    // OpeningHourType
    // ------------------------------------------------------------------------

}
