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
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import uk.gov.hmcts.reform.fact.data.api.entities.AreaOfLawType;
import uk.gov.hmcts.reform.fact.data.api.entities.CourtAreasOfLaw;
import uk.gov.hmcts.reform.fact.data.api.services.CourtAreasOfLawService;
import uk.gov.hmcts.reform.fact.data.api.validation.annotations.ValidUUID;

import java.util.Map;
import java.util.UUID;

@Tag(name = "Court Areas Of Law", description = "Operations related to Court Areas Of Law")
@RestController
@Validated
@RequestMapping("/courts/{courtId}")
public class CourtAreasOfLawController {

    private final CourtAreasOfLawService courtAreasOfLawService;

    public CourtAreasOfLawController(CourtAreasOfLawService courtAreasOfLawService) {
        this.courtAreasOfLawService = courtAreasOfLawService;
    }

    @GetMapping("/v1/areas-of-law")
    @Operation(
        summary = "Get Areas Of Law map with boolean by court ID",
        description = "Fetch Areas Of Law with availability boolean for a given court."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Successfully retrieved Areas Of Law"),
        @ApiResponse(responseCode = "204", description = "No areas Of Law found for the court"),
        @ApiResponse(responseCode = "400", description = "Invalid court ID supplied"),
        @ApiResponse(responseCode = "404", description = "Court not found")
    })
    public ResponseEntity<Map<AreaOfLawType, Boolean>> getAreasOfLawByCourtId(
        @Parameter(description = "UUID of the court", required = true) @ValidUUID @PathVariable String courtId) {
        return ResponseEntity.ok(courtAreasOfLawService.getAreasOfLawStatusByCourtId(UUID.fromString(courtId)));
    }

    @PutMapping("/v1/areas-of-law")
    @Operation(
        summary = "Create or update Court Areas Of Law for a court",
        description = "Creates a new Court Areas Of Law for a court or updates the existing one."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "201", description = "Successfully created/updated Court Areas Of Law"),
        @ApiResponse(responseCode = "400", description = "Invalid court ID supplied or invalid request body"),
        @ApiResponse(responseCode = "404", description = "Court not found")
    })
    public ResponseEntity<CourtAreasOfLaw> setAreasOfLawServices(
        @Parameter(description = "UUID of the court", required = true) @ValidUUID @PathVariable String courtId,
        @Parameter(description = "AreasOfLaw object to create or update", required = true)
        @Valid @RequestBody CourtAreasOfLaw courtAreasOfLaw) {
        return ResponseEntity
            .status(HttpStatus.CREATED)
            .body(courtAreasOfLawService.setCourtAreasOfLaw(UUID.fromString(courtId), courtAreasOfLaw));
    }
}
