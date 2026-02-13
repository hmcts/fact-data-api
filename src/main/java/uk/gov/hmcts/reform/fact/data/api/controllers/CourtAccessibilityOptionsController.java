package uk.gov.hmcts.reform.fact.data.api.controllers;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
import uk.gov.hmcts.reform.fact.data.api.entities.CourtAccessibilityOptions;
import uk.gov.hmcts.reform.fact.data.api.services.CourtAccessibilityOptionsService;
import uk.gov.hmcts.reform.fact.data.api.validation.annotations.ValidUUID;

import java.util.UUID;

@Tag(
    name = "Court Accessibility Options",
    description = "Operations related to Accessibility Options available for courts"
)
@RestController
@Validated
@RequestMapping("/courts/{courtId}")
public class CourtAccessibilityOptionsController {

    private final CourtAccessibilityOptionsService courtAccessibilityOptionsService;

    public CourtAccessibilityOptionsController(CourtAccessibilityOptionsService courtAccessibilityOptionsService) {
        this.courtAccessibilityOptionsService = courtAccessibilityOptionsService;
    }

    @GetMapping("/v1/accessibility-options")
    @Operation(
        summary = "Get AccessibilityOptions services by court ID",
        description = "Fetch AccessibilityOptions services for a given court."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Successfully retrieved Accessibility Options"),
        @ApiResponse(responseCode = "204", description = "No Accessibility Options found for the court"),
        @ApiResponse(responseCode = "400", description = "Invalid court ID supplied"),
        @ApiResponse(responseCode = "404", description = "Court not found")
    })
    public ResponseEntity<CourtAccessibilityOptions> getAccessibilityOptionsServicesByCourtId(
        @Parameter(description = "UUID of the court", required = true) @ValidUUID @PathVariable String courtId) {
        return ResponseEntity.ok(
            courtAccessibilityOptionsService.getAccessibilityOptionsByCourtId(UUID.fromString(courtId)));
    }

    @PostMapping("/v1/accessibility-options")
    @Operation(
        summary = "Create or update Accessibility Options for a court",
        description = "Creates a new Accessibility Options for a court or updates the existing one."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "201", description = "Successfully created/updated Accessibility Options"),
        @ApiResponse(responseCode = "400", description = "Invalid court ID supplied or invalid request body"),
        @ApiResponse(responseCode = "404", description = "Court not found")
    })
    public ResponseEntity<CourtAccessibilityOptions> setAccessibilityOptionsServices(
        @Parameter(description = "UUID of the court", required = true) @ValidUUID @PathVariable String courtId,
        @Parameter(description = "AccessibilityOptions object to create or update", required = true)
        @Valid @RequestBody CourtAccessibilityOptions courtAccessibilityOptions) {
        return ResponseEntity
            .status(HttpStatus.CREATED)
            .body(courtAccessibilityOptionsService.setAccessibilityOptions(
                UUID.fromString(courtId), courtAccessibilityOptions));
    }
}
