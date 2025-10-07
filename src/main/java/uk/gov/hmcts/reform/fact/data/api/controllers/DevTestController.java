package uk.gov.hmcts.reform.fact.data.api.controllers;

import uk.gov.hmcts.reform.fact.data.api.entities.Court;
import uk.gov.hmcts.reform.fact.data.api.entities.Region;
import uk.gov.hmcts.reform.fact.data.api.entities.Translation;
import uk.gov.hmcts.reform.fact.data.api.repositories.CourtRepository;
import uk.gov.hmcts.reform.fact.data.api.repositories.RegionRepository;
import uk.gov.hmcts.reform.fact.data.api.repositories.TranslationRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

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
@Generated
public class DevTestController {

    private final CourtRepository courtRepository;
    private final RegionRepository regionRepository;
    private final TranslationRepository translationRepository;

    // ------------------------------------------------------------------------
    // Court
    // ------------------------------------------------------------------------

    @PostMapping(value = "/court/", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<UUID> createCourt(@RequestBody Court court) {
        // NOTE: this is required because the dto wants to just use the id and
        // the entity dao likes to have the related record
        regionRepository.findById(court.getRegionId()).ifPresent(court::setRegion);

        // save it and return the location
        var result = courtRepository.save(court);
        var location = ServletUriComponentsBuilder.fromCurrentRequest().path("/{id}")
            .buildAndExpand(result).toUri();
        return ResponseEntity.created(location).body(result.getId());
    }

    @GetMapping("/court/")
    public ResponseEntity<List<Court>> getAllCourts() {
        return ResponseEntity.ok().body(courtRepository.findAll());
    }

    @GetMapping("/court/{id}")
    public ResponseEntity<Court> getCourtById(@PathVariable UUID id) {
        Optional<Court> court = courtRepository.findById(id);
        return court.map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.notFound().build());
    }

    // ------------------------------------------------------------------------
    // Region
    // ------------------------------------------------------------------------

    @PostMapping(value = "/region/", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<UUID> createRegion(@RequestBody Region region) {
        // save it and return location
        var result = regionRepository.save(region);
        var location = ServletUriComponentsBuilder.fromCurrentRequest().path("/{id}")
            .buildAndExpand(result).toUri();
        return ResponseEntity.created(location).body(result.getId());
    }

    @GetMapping("/region/")
    public ResponseEntity<List<Region>> getAllRegions() {
        return ResponseEntity.ok().body(regionRepository.findAll());
    }

    @GetMapping("/region/{id}")
    public ResponseEntity<Region> getRegionById(@PathVariable UUID id) {
        Optional<Region> region = regionRepository.findById(id);
        return region.map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.notFound().build());
    }

    // ------------------------------------------------------------------------
    // Translation
    // ------------------------------------------------------------------------

    @PostMapping(value = "/translation/", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<UUID> createTranslation(@RequestBody Translation translation) {
        // required due o DTO/DAO differences
        courtRepository.findById(translation.getCourtId()).ifPresent(translation::setCourt);
        // save and return the location
        var result = translationRepository.save(translation);
        var location = ServletUriComponentsBuilder.fromCurrentRequest().path("/{id}")
            .buildAndExpand(result).toUri();
        return ResponseEntity.created(location).body(result.getId());
    }

    @GetMapping("/translation/")
    public ResponseEntity<List<Translation>> getAllTranslations() {
        return ResponseEntity.ok().body(translationRepository.findAll());
    }

    @GetMapping("/translation/{id}")
    public ResponseEntity<Translation> getTranslationById(@PathVariable UUID id) {
        Optional<Translation> translation = translationRepository.findById(id);
        return translation.map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.notFound().build());
    }

}
