package uk.gov.hmcts.reform.fact.data.api.controllers;

import uk.gov.hmcts.reform.fact.data.api.entities.CourtPostcode;
import uk.gov.hmcts.reform.fact.data.api.models.PostcodeListDto;
import uk.gov.hmcts.reform.fact.data.api.models.PostcodeMoveDto;
import uk.gov.hmcts.reform.fact.data.api.services.CourtPostcodeService;
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
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Court Postcodes", description = "Operations related to postcodes for courts")
@RestController
@Validated
@RequestMapping("/courts/{courtId}")
@RequiredArgsConstructor
public class CourtPostcodeController {

    private final CourtPostcodeService courtPostcodeService;

    @GetMapping("/v1/postcodes")
    @Operation(
        summary = "Get all postcodes by court ID",
        description = "Fetch all postcodes associated to a given court."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Successfully retrieved postcodes for the court"),
        @ApiResponse(responseCode = "204", description = "No Postcodes found for the court"),
        @ApiResponse(responseCode = "400", description = "Invalid court ID supplied"),
        @ApiResponse(responseCode = "404", description = "Court not found")
    })
    public ResponseEntity<List<CourtPostcode>> getCourtPostcodes(
        @Parameter(description = "UUID of the court", required = true) @ValidUUID @PathVariable String courtId) {
        List<CourtPostcode> postcodes = courtPostcodeService.getPostcodesByCourtId(UUID.fromString(courtId));
        if (postcodes != null && !postcodes.isEmpty()) {
            return ResponseEntity.ok(postcodes);
        }
        return ResponseEntity.noContent().build();
    }

    @PostMapping(value = "/v1/postcodes",
        consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(
        summary = "Adds new postcodes to a court",
        description = "Adds a set of new postcodes to a  given court."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "204", description = "Successfully add the list of postcodes to the court"),
        @ApiResponse(responseCode = "400", description = "Invalid court ID supplied or invalid request body"),
        @ApiResponse(responseCode = "404", description = "Court not found")
    })
    public ResponseEntity<Void> addCourtPostcodes(
        @Parameter(description = "UUID of the court", required = true) @ValidUUID @PathVariable String courtId,
        @Parameter(description = "List of Postcode objects to update", required = true)
        @Valid @RequestBody PostcodeListDto courtPostcodes) {
        courtPostcodeService.addPostcodesToCourt(courtPostcodes, UUID.fromString(courtId));
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping(value = "/v1/postcodes",
        consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(
        summary = "Delete existing postcodes from a court",
        description = "Deletes a set of existing postcodes from the given court."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "204", description = "Successfully removed the list of postcodes from the court"),
        @ApiResponse(responseCode = "400", description = "Invalid court ID supplied or invalid request body"),
        @ApiResponse(responseCode = "404", description = "Court not found")
    })
    public ResponseEntity<Void> removeCourtPostcodes(
        @Parameter(description = "UUID of the court", required = true) @ValidUUID @PathVariable String courtId,
        @Parameter(description = "Facilities object to create or update", required = true)
        @Valid @RequestBody PostcodeListDto courtPostcodes) {
        courtPostcodeService.removePostcodesFromCourt(courtPostcodes, UUID.fromString(courtId));
        return ResponseEntity.noContent().build();
    }

    @PostMapping(value = "/v1/postcodes/move",
        consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(
        summary = "Migrates existing postcodes between courts",
        description = "Moves the assigned postcodes from the source court to the destination court."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "204", description = "Successfully removed the list of postcodes from the court"),
        @ApiResponse(responseCode = "400", description = "Invalid court ID supplied or invalid request body"),
        @ApiResponse(responseCode = "404", description = "Court not found")
    })
    public ResponseEntity<Void> migrateCourtPostcodes(
        @Parameter(description = "Postcodes to migrate", required = true)
        @Valid @RequestBody PostcodeMoveDto migrationData) {
        courtPostcodeService.migratePostcodes(migrationData);
        return ResponseEntity.noContent().build();
    }

}
