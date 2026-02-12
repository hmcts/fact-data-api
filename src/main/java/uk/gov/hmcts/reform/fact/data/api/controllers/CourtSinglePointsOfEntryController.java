package uk.gov.hmcts.reform.fact.data.api.controllers;

import uk.gov.hmcts.reform.fact.data.api.models.AreaOfLawSelectionDto;
import uk.gov.hmcts.reform.fact.data.api.services.CourtSinglePointsOfEntryService;
import uk.gov.hmcts.reform.fact.data.api.validation.annotations.ValidUUID;

import java.util.List;
import java.util.UUID;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Court Single Points of Entry", description = "Operations related to single points of entry for courts")
@RestController
@Validated
@RequestMapping("/courts/{courtId}")
@RequiredArgsConstructor
public class CourtSinglePointsOfEntryController {

    private final CourtSinglePointsOfEntryService courtSinglePointOfEntryService;

    @GetMapping("/v1/single-point-of-entry")
    @Operation(
        summary = "Get Single Points of Entry by court ID",
        description = "Fetch Single Points of Entry configuration for a given court."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Successfully retrieved Single Points of Entry"),
        @ApiResponse(responseCode = "400", description = "Invalid court ID supplied"),
        @ApiResponse(responseCode = "404", description = "Court not found")
    })
    public ResponseEntity<List<AreaOfLawSelectionDto>> getSinglePointsOfEntry(
        @Parameter(description = "UUID of the court", required = true)
        @PathVariable String courtId) {
        // Provided the court exists, the service code will initialise
        // the selection dto list to the allowable set, all unselected.
        return ResponseEntity.ok().body(
            courtSinglePointOfEntryService.getCourtSinglePointsOfEntry(UUID.fromString(courtId))
        );
    }

    @PutMapping("/v1/single-point-of-entry")
    @Operation(
        summary = "Update Single Points of Entry by Court ID",
        description = "Updates the Single Points of Entry configuration for a given court."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200",
            description = "Successfully updated the Single Point of Entry configuration for the Court"),
        @ApiResponse(responseCode = "400",
            description = "court ID and/or Single Point of Entry configuration data is invalid"),
        @ApiResponse(responseCode = "404", description = "Court not found")
    })
    public ResponseEntity<Void> updateSinglePointsOfEntry(
        @Parameter(description = "UUID of the court", required = true)
        @ValidUUID @PathVariable String courtId,
        @Parameter(description = "The updated Single Points of Entry for the court", required = true)
        @Valid @RequestBody List<AreaOfLawSelectionDto> courtSinglePointsOfEntry) {
        // Provided the court exists, this will update or initialise
        // Single Points of Entry configuration
        courtSinglePointOfEntryService.updateCourtSinglePointsOfEntry(
            UUID.fromString(courtId),
            courtSinglePointsOfEntry
        );
        return ResponseEntity.ok().build();
    }
}
