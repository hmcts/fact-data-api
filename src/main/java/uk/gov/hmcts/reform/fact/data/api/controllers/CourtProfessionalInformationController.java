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
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import uk.gov.hmcts.reform.fact.data.api.entities.CourtProfessionalInformation;
import uk.gov.hmcts.reform.fact.data.api.services.CourtProfessionalInformationService;
import uk.gov.hmcts.reform.fact.data.api.validation.annotations.ValidUUID;

import java.util.UUID;

@Tag(
    name = "Court Professional Information",
    description = "Operations related to professional information available for courts"
)
@RestController
@Validated
@RequestMapping("/courts/{courtId}")
public class CourtProfessionalInformationController {

    private final CourtProfessionalInformationService professionalInformationService;

    public CourtProfessionalInformationController(
        CourtProfessionalInformationService professionalInformationService
    ) {
        this.professionalInformationService = professionalInformationService;
    }

    @GetMapping("/professional-information")
    @Operation(
        summary = "Get professional information by court ID",
        description = "Fetch professional information for a given court."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Successfully retrieved professional information"),
        @ApiResponse(responseCode = "204", description = "No professional information found for the court"),
        @ApiResponse(responseCode = "400", description = "Invalid court ID supplied"),
        @ApiResponse(responseCode = "404", description = "Court not found")
    })
    public ResponseEntity<CourtProfessionalInformation> getProfessionalInformation(
        @Parameter(description = "UUID of the court", required = true) @ValidUUID @PathVariable String courtId
    ) {
        return ResponseEntity.ok(
            professionalInformationService.getProfessionalInformation(UUID.fromString(courtId))
        );
    }

    @PostMapping("/professional-information")
    @Operation(
        summary = "Create or update professional information for a court",
        description = "Creates a new professional information record for a court or updates the existing one."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "201", description = "Successfully created/updated professional information"),
        @ApiResponse(responseCode = "400", description = "Invalid court ID supplied or invalid request body"),
        @ApiResponse(responseCode = "404", description = "Court not found")
    })
    public ResponseEntity<CourtProfessionalInformation> setProfessionalInformation(
        @Parameter(description = "UUID of the court", required = true) @ValidUUID @PathVariable String courtId,
        @Parameter(description = "Professional information object to create or update", required = true)
        @Valid @RequestBody CourtProfessionalInformation professionalInformation
    ) {
        return ResponseEntity
            .status(HttpStatus.CREATED)
            .body(
                professionalInformationService.setProfessionalInformation(
                    UUID.fromString(courtId),
                    professionalInformation
                )
            );
    }
}
