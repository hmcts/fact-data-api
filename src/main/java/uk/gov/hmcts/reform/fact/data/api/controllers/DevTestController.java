package uk.gov.hmcts.reform.fact.data.api.controllers;

import uk.gov.hmcts.reform.fact.data.api.entities.Court;
import uk.gov.hmcts.reform.fact.data.api.entities.Region;
import uk.gov.hmcts.reform.fact.data.api.entities.Translation;
import uk.gov.hmcts.reform.fact.data.api.errorhandling.exceptions.NotFoundException;
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
@Generated
public class DevTestController {

    private final CourtService courtService;
    private final RegionService regionService;
    private final TranslationService translationService;

    // ------------------------------------------------------------------------
    // Court
    // ------------------------------------------------------------------------

    @PostMapping(value = "/court/", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<UUID> createCourt(@Valid @RequestBody Court court) throws NotFoundException {
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
    public ResponseEntity<UUID> createTranslation(@Valid @RequestBody Translation translation)
        throws NotFoundException {
        // save and return the location
        var result = translationService.create(translation);
        var location = ServletUriComponentsBuilder.fromCurrentRequest().path("/{id}")
            .buildAndExpand(result).toUri();
        return ResponseEntity.created(location).body(result.getId());
    }

    @GetMapping("/translation/")
    public ResponseEntity<List<Translation>> getAllTranslations() {
        return ResponseEntity.ok().body(translationService.retrieveAll());
    }

    @GetMapping("/translation/{id}")
    public ResponseEntity<Translation> getTranslationById(@PathVariable UUID id) throws NotFoundException {
        return ResponseEntity.ok().body(translationService.retrieve(id));
    }

}
