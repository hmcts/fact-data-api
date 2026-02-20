package uk.gov.hmcts.reform.fact.data.api.controllers;

import uk.gov.hmcts.reform.fact.data.api.entities.CourtCounterServiceOpeningHours;
import uk.gov.hmcts.reform.fact.data.api.entities.CourtOpeningHours;
import uk.gov.hmcts.reform.fact.data.api.security.SecuredFactRestController;
import uk.gov.hmcts.reform.fact.data.api.services.CourtOpeningHoursService;
import uk.gov.hmcts.reform.fact.data.api.validation.annotations.UniqueOpeningDays;
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

    @GetMapping("/v1/opening-hours/{openingHourTypeId}")
    @Operation(
        summary = "Get court opening hours by court ID and type ID",
        description = "Fetch opening hours of a given type for a given court."
            + "Returns 204 if no opening hours of type exists for the court."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Successfully retrieved court opening hours"),
        @ApiResponse(responseCode = "204", description = "No opening hours found for the court of this type"),
        @ApiResponse(responseCode = "400", description = "Invalid court ID or opening hour type ID supplied"),
        @ApiResponse(responseCode = "404", description = "Court not found")
    })
    public ResponseEntity<List<CourtOpeningHours>> getOpeningHoursByTypeId(
        @Parameter(description = "UUID of the court", required = true) @ValidUUID @PathVariable String courtId,
        @Parameter(description = "UUID of the opening hours type", required = true)
        @ValidUUID @PathVariable String openingHourTypeId) {
        return ResponseEntity.ok(
            courtOpeningHoursService
                .getOpeningHoursByTypeId(UUID.fromString(courtId), UUID.fromString(openingHourTypeId)));
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
    public ResponseEntity<List<CourtCounterServiceOpeningHours>> getCounterServiceOpeningHoursByCourtId(
        @Parameter(description = "UUID of the court", required = true) @ValidUUID @PathVariable String courtId) {
        return ResponseEntity.ok(
            courtOpeningHoursService.getCounterServiceOpeningHoursByCourtId(UUID.fromString(courtId)));
    }

    @PutMapping("/v1/opening-hours/{openingHourTypeId}")
    @Operation(
        summary = "Set opening hours of type for a court",
        description = "Creates a opening hours of type for a court or updates the existing one."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Successfully created/updated opening hours"),
        @ApiResponse(responseCode = "204", description = "No Content"),
        @ApiResponse(responseCode = "400", description = "Invalid court ID, opening hours type ID, or request body"),
        @ApiResponse(responseCode = "404", description = "Court not found")
    })
    @PreAuthorize("@authService.isAdmin()")
    public ResponseEntity<List<CourtOpeningHours>> setOpeningHours(
        @Parameter(description = "UUID of the court", required = true) @ValidUUID @PathVariable String courtId,
        @Parameter(description = "UUID of the opening hours type", required = true)
        @ValidUUID @PathVariable String openingHourTypeId,
        @UniqueOpeningDays @Valid @RequestBody List<CourtOpeningHours> courtOpeningHours) {
        return ResponseEntity.ok(
            courtOpeningHoursService.setOpeningHours(
                UUID.fromString(courtId), UUID.fromString(openingHourTypeId), courtOpeningHours)
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
        @ApiResponse(responseCode = "404", description = "Court not found")
    })
    @PreAuthorize("@authService.isAdmin()")
    public ResponseEntity<List<CourtCounterServiceOpeningHours>> setCounterServiceOpeningHours(
        @Parameter(description = "UUID of the court", required = true) @ValidUUID @PathVariable String courtId,
        @UniqueOpeningDays @Valid @RequestBody List<CourtCounterServiceOpeningHours> courtCounterServiceOpeningHours) {
        return ResponseEntity.ok(
            courtOpeningHoursService.setCounterServiceOpeningHours(
                UUID.fromString(courtId), courtCounterServiceOpeningHours)
        );
    }

    @DeleteMapping("/v1/opening-hours/{openingHourTypeId}")
    @Operation(
        summary = "Delete opening hours of given type for a court",
        description = "Deletes opening hours of given type for a court."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Successfully deleted opening hours"),
        @ApiResponse(responseCode = "400", description = "Invalid court ID or opening hours type ID"),
        @ApiResponse(responseCode = "404", description = "Court not found")
    })
    @PreAuthorize("@authService.isAdmin()")
    public ResponseEntity<Void> deleteOpeningHours(
        @Parameter(description = "UUID of the court", required = true) @ValidUUID @PathVariable String courtId,
        @Parameter(description = "UUID of the opening hours type", required = true)
        @ValidUUID @PathVariable String openingHourTypeId) {
        courtOpeningHoursService.deleteCourtOpeningHours(UUID.fromString(courtId), UUID.fromString(openingHourTypeId));
        return ResponseEntity.ok().body(null);
    }
}
