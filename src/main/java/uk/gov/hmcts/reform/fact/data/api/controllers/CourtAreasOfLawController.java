package uk.gov.hmcts.reform.fact.data.api.controllers;

import uk.gov.hmcts.reform.fact.data.api.entities.AreaOfLawType;
import uk.gov.hmcts.reform.fact.data.api.entities.CourtAreasOfLaw;
import uk.gov.hmcts.reform.fact.data.api.security.SecuredFactRestController;
import uk.gov.hmcts.reform.fact.data.api.services.CourtAreasOfLawService;
import uk.gov.hmcts.reform.fact.data.api.validation.annotations.ValidUUID;

import java.util.Map;
import java.util.UUID;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

@SecuredFactRestController(
    name = "Court Areas Of Law",
    description = "Operations related to Court Areas Of Law"
)
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
    @PreAuthorize("@authService.isAdmin()")
    public ResponseEntity<CourtAreasOfLaw> setAreasOfLawServices(
        @Parameter(description = "UUID of the court", required = true) @ValidUUID @PathVariable String courtId,
        @Parameter(description = "AreasOfLaw object to create or update", required = true)
        @Valid @RequestBody CourtAreasOfLaw courtAreasOfLaw) {
        return ResponseEntity
            .status(HttpStatus.CREATED)
            .body(courtAreasOfLawService.setCourtAreasOfLaw(UUID.fromString(courtId), courtAreasOfLaw));
    }
}
