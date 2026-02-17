package uk.gov.hmcts.reform.fact.data.api.controllers;

import uk.gov.hmcts.reform.fact.data.api.models.CourtLocalAuthorityDto;
import uk.gov.hmcts.reform.fact.data.api.security.SecuredFactRestController;
import uk.gov.hmcts.reform.fact.data.api.services.CourtLocalAuthoritiesService;
import uk.gov.hmcts.reform.fact.data.api.validation.annotations.ValidUUID;

import java.util.List;
import java.util.UUID;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

@SecuredFactRestController(
    name = "Local Authorities",
    description = "Operations related to court local authorities"
)
@RequestMapping("/courts/{courtId}")
public class CourtLocalAuthoritiesController {

    private final CourtLocalAuthoritiesService courtLocalAuthoritiesService;

    public CourtLocalAuthoritiesController(CourtLocalAuthoritiesService courtLocalAuthoritiesService) {
        this.courtLocalAuthoritiesService = courtLocalAuthoritiesService;
    }

    @GetMapping(value = "/v1/local-authorities", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(
        summary = "Get local authorities for a court",
        description = "Fetch allowed areas of law enabled for the court and the local authorities."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Successfully retrieved court local authorities"),
        @ApiResponse(responseCode = "204", description = "No local authority configuration found for the court"),
        @ApiResponse(responseCode = "400", description = "Invalid court ID supplied"),
        @ApiResponse(responseCode = "404", description = "Court not found")
    })
    public ResponseEntity<List<CourtLocalAuthorityDto>> getCourtLocalAuthorities(
        @Parameter(description = "UUID of the court", required = true) @ValidUUID @PathVariable String courtId) {
        return ResponseEntity.ok(courtLocalAuthoritiesService.getCourtLocalAuthorities(UUID.fromString(courtId)));
    }

    @PutMapping(
        value = "/v1/local-authorities",
        consumes = MediaType.APPLICATION_JSON_VALUE,
        produces = MediaType.APPLICATION_JSON_VALUE
    )
    @Operation(
        summary = "Update local authorities for a court",
        description = "Update the local authorities mapped to the allowed areas of law (adoption, children, civil "
            + "partnership, divorce) for a given court."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Successfully updated local authorities"),
        @ApiResponse(responseCode = "204", description = "No local authority configuration found for the court"),
        @ApiResponse(responseCode = "400", description = "Invalid court ID supplied or request validation failed"),
        @ApiResponse(responseCode = "404", description = "Court or local authority not found")
    })
    @PreAuthorize("@authService.isAdmin()")
    public ResponseEntity<String> updateCourtLocalAuthorities(
        @Parameter(description = "UUID of the court", required = true) @ValidUUID @PathVariable String courtId,
        @Parameter(description = "Local authority mappings to update", required = true)
        @Valid @RequestBody List<CourtLocalAuthorityDto> updates) {
        courtLocalAuthoritiesService.setCourtLocalAuthorities(UUID.fromString(courtId), updates);
        return ResponseEntity.ok("Update successful for court ID " + courtId);
    }
}
