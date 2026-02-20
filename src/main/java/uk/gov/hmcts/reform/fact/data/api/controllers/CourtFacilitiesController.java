package uk.gov.hmcts.reform.fact.data.api.controllers;

import uk.gov.hmcts.reform.fact.data.api.entities.CourtFacilities;
import uk.gov.hmcts.reform.fact.data.api.security.SecuredFactRestController;
import uk.gov.hmcts.reform.fact.data.api.services.CourtFacilitiesService;
import uk.gov.hmcts.reform.fact.data.api.validation.annotations.ValidUUID;

import java.util.UUID;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

@SecuredFactRestController(
    name = "Court Facilities",
    description = "Operations related to facilities available at courts"
)
@RequestMapping("/courts/{courtId}")
public class CourtFacilitiesController {

    private final CourtFacilitiesService courtFacilitiesService;

    public CourtFacilitiesController(CourtFacilitiesService courtFacilitiesService) {
        this.courtFacilitiesService = courtFacilitiesService;
    }

    @GetMapping("/v1/building-facilities")
    @Operation(
        summary = "Get building facilities by court ID",
        description = "Fetch building facilities for a given court."
            + "Returns 204 if no building facilities exist for the court."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Successfully retrieved building facilities"),
        @ApiResponse(responseCode = "204", description = "No building facilities found for the court"),
        @ApiResponse(responseCode = "400", description = "Invalid court ID supplied"),
        @ApiResponse(responseCode = "404", description = "Court not found")
    })
    public ResponseEntity<CourtFacilities> getBuildingFacilitiesByCourtId(
        @Parameter(description = "UUID of the court", required = true) @ValidUUID @PathVariable String courtId) {
        return ResponseEntity.ok(courtFacilitiesService.getFacilitiesByCourtId(UUID.fromString(courtId)));
    }

    @PostMapping(value = "/v1/building-facilities",
        consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(
        summary = "Create or update building facilities for a court",
        description = "Creates a new building facilities for a court or updates the existing one."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "201", description = "Successfully created/updated building facilities"),
        @ApiResponse(responseCode = "400", description = "Invalid court ID supplied or invalid request body"),
        @ApiResponse(responseCode = "404", description = "Court not found")
    })
    @PreAuthorize("@authService.isAdmin()")
    public ResponseEntity<CourtFacilities> setBuildingFacilities(
        @Parameter(description = "UUID of the court", required = true) @ValidUUID @PathVariable String courtId,
        @Parameter(description = "Facilities object to create or update", required = true)
        @Valid @RequestBody CourtFacilities courtFacilities) {
        return ResponseEntity
            .status(HttpStatus.CREATED)
            .body(courtFacilitiesService.setFacilities(UUID.fromString(courtId), courtFacilities));
    }
}
