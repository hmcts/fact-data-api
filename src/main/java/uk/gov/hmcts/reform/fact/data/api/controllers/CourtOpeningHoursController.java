package uk.gov.hmcts.reform.fact.data.api.controllers;

import uk.gov.hmcts.reform.fact.data.api.entities.CourtCounterServiceOpeningHours;
import uk.gov.hmcts.reform.fact.data.api.entities.CourtOpeningHours;
import uk.gov.hmcts.reform.fact.data.api.security.SecuredFactRestController;
import uk.gov.hmcts.reform.fact.data.api.services.CourtOpeningHoursService;
import uk.gov.hmcts.reform.fact.data.api.validation.annotations.ValidUUID;

import java.util.List;
import java.util.UUID;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

@SuppressWarnings("java:S4684")
@SecuredFactRestController(
    name = "Court Opening Hours",
    description = "Operations related to opening hours for courts"
)
@RequestMapping("/courts/{courtId}/")
public class CourtOpeningHoursController {

    private final CourtOpeningHoursService courtOpeningHoursService;

    public CourtOpeningHoursController(CourtOpeningHoursService courtOpeningHoursService) {
        this.courtOpeningHoursService = courtOpeningHoursService;
    }

    @GetMapping("/v1/opening-hours")
    @Operation(
        summary = "Get court opening hours by court ID",
        description = "Fetch opening hours for a given court."
            + "Returns 204 if no opening hours exist for the court."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Successfully retrieved court opening hours"),
        @ApiResponse(responseCode = "204", description = "No opening hours found for the court"),
        @ApiResponse(responseCode = "400", description = "Invalid court ID supplied"),
        @ApiResponse(responseCode = "404", description = "Court not found")
    })
    public ResponseEntity<List<CourtOpeningHours>> getOpeningHoursByCourtId(
        @Parameter(description = "UUID of the court", required = true) @ValidUUID @PathVariable String courtId) {
        return ResponseEntity.ok(courtOpeningHoursService.getOpeningHoursByCourtId(UUID.fromString(courtId)));
    }

    @GetMapping("/v1/opening-hours/{openingHoursId}")
    @Operation(
        summary = "Get court opening hours by court ID and opening hours ID",
        description = "Fetch opening hours for a given court."
            + "Returns 204 if no opening hours exist for the court with this ID."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Successfully retrieved court opening hours"),
        @ApiResponse(responseCode = "204", description = "No opening hours found for the court with this ID"),
        @ApiResponse(responseCode = "400", description = "Invalid court ID or opening hours ID supplied"),
        @ApiResponse(responseCode = "404", description = "Court or opening hours not found")
    })
    public ResponseEntity<CourtOpeningHours> getOpeningHoursById(
        @Parameter(description = "UUID of the court", required = true) @ValidUUID @PathVariable String courtId,
        @Parameter(description = "UUID of the opening hours", required = true)
        @ValidUUID @PathVariable String openingHoursId) {
        return ResponseEntity.ok(
            courtOpeningHoursService
                .getOpeningHoursById(UUID.fromString(courtId), UUID.fromString(openingHoursId)));
    }

    @GetMapping("/v1/opening-hours/counter-service")
    @Operation(
        summary = "Get court counter service opening hours by court ID",
        description = "Fetch counter service opening hours for a given court."
            + "Returns 204 if no counter service opening hours exist for the court."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Successfully retrieved court counter service opening hours"),
        @ApiResponse(responseCode = "204", description = "No counter service opening hours found for the court"),
        @ApiResponse(responseCode = "400", description = "Invalid court ID supplied"),
        @ApiResponse(responseCode = "404", description = "Court not found")
    })
    public ResponseEntity<CourtCounterServiceOpeningHours> getCounterServiceOpeningHoursByCourtId(
        @Parameter(description = "UUID of the court", required = true) @ValidUUID @PathVariable String courtId) {
        return ResponseEntity.ok(
            courtOpeningHoursService.getCounterServiceOpeningHoursByCourtId(UUID.fromString(courtId)));
    }

    @PutMapping("/v1/opening-hours")
    @Operation(
        summary = "Set opening hours for a court",
        description = "Creates a opening hours for a court or updates the existing one."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Successfully created/updated opening hours"),
        @ApiResponse(responseCode = "204", description = "No Content"),
        @ApiResponse(responseCode = "400", description = "Invalid court ID, or request body"),
        @ApiResponse(responseCode = "404", description = "Court not found")
    })
    @PreAuthorize("@authService.isAdmin()")
    public ResponseEntity<CourtOpeningHours> setOpeningHours(
        @Parameter(description = "UUID of the court", required = true) @ValidUUID @PathVariable String courtId,
        @Valid @RequestBody CourtOpeningHours courtOpeningHours) {
        return ResponseEntity.ok(
            courtOpeningHoursService.setOpeningHours(
                UUID.fromString(courtId), courtOpeningHours)
        );
    }

    @PutMapping("/v1/opening-hours/counter-service")
    @Operation(
        summary = "Set counter service opening hours for a court",
        description = "Creates a opening hours for a court or updates the existing one."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Successfully created/updated opening hours"),
        @ApiResponse(responseCode = "204", description = "No Content"),
        @ApiResponse(responseCode = "400", description = "Invalid court ID or request body"),
        @ApiResponse(responseCode = "404", description = "Court or court type not found")
    })
    @PreAuthorize("@authService.isAdmin()")
    public ResponseEntity<CourtCounterServiceOpeningHours> setCounterServiceOpeningHours(
        @Parameter(description = "UUID of the court", required = true) @ValidUUID @PathVariable String courtId,
        @Valid @RequestBody CourtCounterServiceOpeningHours courtCounterServiceOpeningHours) {
        return ResponseEntity.ok(
            courtOpeningHoursService.setCounterServiceOpeningHours(
                UUID.fromString(courtId), courtCounterServiceOpeningHours)
        );
    }

    @DeleteMapping("/v1/opening-hours/{openingHoursId}")
    @Operation(
        summary = "Delete opening hours for a court",
        description = "Deletes opening hours for a court."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Successfully deleted opening hours"),
        @ApiResponse(responseCode = "400", description = "Invalid court ID or opening hours ID"),
        @ApiResponse(responseCode = "404", description = "Court not found")
    })
    @PreAuthorize("@authService.isAdmin()")
    public ResponseEntity<Void> deleteOpeningHours(
        @Parameter(description = "UUID of the court", required = true) @ValidUUID @PathVariable String courtId,
        @Parameter(description = "UUID of the opening hours", required = true)
        @ValidUUID @PathVariable String openingHoursId) {
        courtOpeningHoursService.deleteCourtOpeningHours(UUID.fromString(courtId), UUID.fromString(openingHoursId));
        return ResponseEntity.ok().body(null);
    }

    @DeleteMapping("/v1/opening-hours/counter-service")
    @Operation(
        summary = "Delete counter service opening hours for a court",
        description = "Deletes counter service opening hours for a court."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Successfully deleted counter service opening hours"),
        @ApiResponse(responseCode = "400", description = "Invalid court ID"),
        @ApiResponse(responseCode = "404", description = "Court not found")
    })
    @PreAuthorize("@authService.isAdmin()")
    public ResponseEntity<Void> deleteCounterServiceOpeningHours(
        @Parameter(description = "UUID of the court", required = true) @ValidUUID @PathVariable String courtId) {
        courtOpeningHoursService.deleteCourtCounterServiceOpeningHours(UUID.fromString(courtId));
        return ResponseEntity.ok().body(null);
    }
}
