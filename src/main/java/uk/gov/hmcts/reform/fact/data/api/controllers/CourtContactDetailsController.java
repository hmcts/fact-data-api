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
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import uk.gov.hmcts.reform.fact.data.api.entities.CourtContactDetails;
import uk.gov.hmcts.reform.fact.data.api.services.CourtContactDetailsService;
import uk.gov.hmcts.reform.fact.data.api.validation.annotations.ValidUUID;

import java.util.List;
import java.util.UUID;

@Tag(name = "Court Contact Details", description = "Operations related to contact details available for courts")
@RestController
@Validated
@RequestMapping("/courts/{courtId}")
public class CourtContactDetailsController {

    private final CourtContactDetailsService courtContactDetailsService;

    public CourtContactDetailsController(CourtContactDetailsService courtContactDetailsService) {
        this.courtContactDetailsService = courtContactDetailsService;
    }

    @GetMapping("/v1/contact-details")
    @Operation(
        summary = "Get contact details for a court",
        description = "Fetch all contact details associated with the supplied court ID."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Successfully retrieved contact details"),
        @ApiResponse(responseCode = "400", description = "Invalid court ID supplied"),
        @ApiResponse(responseCode = "404", description = "Court not found")
    })
    public ResponseEntity<List<CourtContactDetails>> getContactDetails(
        @Parameter(description = "UUID of the court", required = true) @ValidUUID @PathVariable String courtId
    ) {
        return ResponseEntity.ok(
            courtContactDetailsService.getContactDetails(UUID.fromString(courtId))
        );
    }

    @GetMapping("/v1/contact-details/{contactId}")
    @Operation(
        summary = "Get a specific contact detail for a court",
        description = "Fetch a single contact detail by court and contact identifier."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Successfully retrieved the contact detail"),
        @ApiResponse(responseCode = "400", description = "Invalid court ID or contact ID supplied"),
        @ApiResponse(responseCode = "404", description = "Contact detail or court not found")
    })
    public ResponseEntity<CourtContactDetails> getContactDetail(
        @Parameter(description = "UUID of the court", required = true) @ValidUUID @PathVariable String courtId,
        @Parameter(description = "UUID of the contact", required = true) @ValidUUID @PathVariable String contactId
    ) {
        return ResponseEntity.ok(
            courtContactDetailsService.getContactDetail(
                UUID.fromString(courtId),
                UUID.fromString(contactId)
            )
        );
    }

    @PostMapping("/v1/contact-details")
    @Operation(
        summary = "Create contact details for a court",
        description = "Create a new contact detail entry for the supplied court."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "201", description = "Successfully created the contact detail"),
        @ApiResponse(responseCode = "400", description = "Invalid court ID supplied or invalid request body"),
        @ApiResponse(responseCode = "404", description = "Court or contact description type not found")
    })
    public ResponseEntity<CourtContactDetails> createContactDetail(
        @Parameter(description = "UUID of the court", required = true) @ValidUUID @PathVariable String courtId,
        @Parameter(description = "Contact detail to create", required = true)
        @Valid @RequestBody CourtContactDetails courtContactDetails
    ) {
        return ResponseEntity
            .status(HttpStatus.CREATED)
            .body(courtContactDetailsService.createContactDetail(UUID.fromString(courtId), courtContactDetails));
    }

    @PutMapping("/v1/contact-details/{contactId}")
    @Operation(
        summary = "Update contact details for a court",
        description = "Update an existing contact detail entry for the supplied court."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Successfully updated the contact detail"),
        @ApiResponse(responseCode = "400",
            description = "Invalid court ID or contact ID supplied, or invalid request body"),
        @ApiResponse(responseCode = "404", description = "Contact detail, court, or contact description type not found")
    })
    public ResponseEntity<CourtContactDetails> updateContactDetail(
        @Parameter(description = "UUID of the court", required = true) @ValidUUID @PathVariable String courtId,
        @Parameter(description = "UUID of the contact", required = true) @ValidUUID @PathVariable String contactId,
        @Parameter(description = "Updated contact detail", required = true)
        @Valid @RequestBody CourtContactDetails courtContactDetails
    ) {
        return ResponseEntity.ok(
            courtContactDetailsService.updateContactDetail(
                UUID.fromString(courtId),
                UUID.fromString(contactId),
                courtContactDetails
            )
        );
    }

    @DeleteMapping("/v1/contact-details/{contactId}")
    @Operation(
        summary = "Delete contact details for a court",
        description = "Remove an existing contact detail entry for the supplied court."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "204", description = "Successfully deleted the contact detail"),
        @ApiResponse(responseCode = "400", description = "Invalid court ID or contact ID supplied"),
        @ApiResponse(responseCode = "404", description = "Contact detail or court not found")
    })
    public ResponseEntity<Void> deleteContactDetail(
        @Parameter(description = "UUID of the court", required = true) @ValidUUID @PathVariable String courtId,
        @Parameter(description = "UUID of the contact", required = true) @ValidUUID @PathVariable String contactId
    ) {
        courtContactDetailsService.deleteContactDetail(UUID.fromString(courtId), UUID.fromString(contactId));
        return ResponseEntity.noContent().build();
    }
}
