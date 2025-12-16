package uk.gov.hmcts.reform.fact.data.api.controllers;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import uk.gov.hmcts.reform.fact.data.api.config.OpenAPIConfiguration;
import uk.gov.hmcts.reform.fact.data.api.entities.CourtTranslation;
import uk.gov.hmcts.reform.fact.data.api.services.CourtTranslationService;
import uk.gov.hmcts.reform.fact.data.api.validation.annotations.ValidUUID;

import java.util.UUID;

@Tag(name = "Court Translation", description = "Operations related to translation services available for courts")
@RestController
@Validated
@RequestMapping("/courts/{courtId}")
@SecurityRequirement(name = OpenAPIConfiguration.BEARER_AUTH_SECURITY_SCHEME)
public class CourtTranslationController {

    private final CourtTranslationService courtTranslationService;

    public CourtTranslationController(CourtTranslationService courtTranslationService) {
        this.courtTranslationService = courtTranslationService;
    }

    @GetMapping("/v1/translation-services")
    @Operation(
        summary = "Get translation services by court ID",
        description = "Fetch translation services for a given court."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Successfully retrieved translation services"),
        @ApiResponse(responseCode = "204", description = "No translation services found for the court"),
        @ApiResponse(responseCode = "400", description = "Invalid court ID supplied"),
        @ApiResponse(responseCode = "404", description = "Court not found")
    })
    public ResponseEntity<CourtTranslation> getTranslationServicesByCourtId(
        @Parameter(description = "UUID of the court", required = true) @ValidUUID @PathVariable String courtId) {
        return ResponseEntity.ok(courtTranslationService.getTranslationByCourtId(UUID.fromString(courtId)));
    }

    @PostMapping(value = "/v1/translation-services",
        consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(
        summary = "Create or update translation services for a court",
        description = "Creates a new translation service for a court or updates the existing one."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "201", description = "Successfully created/updated translation service"),
        @ApiResponse(responseCode = "400", description = "Invalid court ID supplied or invalid request body"),
        @ApiResponse(responseCode = "404", description = "Court not found")
    })
    public ResponseEntity<CourtTranslation> setTranslationServices(
        @Parameter(description = "UUID of the court", required = true) @ValidUUID @PathVariable String courtId,
        @Parameter(description = "Translation object to create or update", required = true)
        @Valid @RequestBody CourtTranslation courtTranslation) {
        return ResponseEntity
            .status(HttpStatus.CREATED)
            .body(courtTranslationService.setTranslation(UUID.fromString(courtId), courtTranslation));
    }
}
