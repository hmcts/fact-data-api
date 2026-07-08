package uk.gov.hmcts.reform.fact.data.api.controllers;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import uk.gov.hmcts.reform.fact.data.api.entities.ServiceCentreContactDetails;
import uk.gov.hmcts.reform.fact.data.api.security.SecuredFactRestController;
import uk.gov.hmcts.reform.fact.data.api.services.ServiceCentreContactDetailsService;
import uk.gov.hmcts.reform.fact.data.api.validation.annotations.ValidUUID;

import java.util.List;
import java.util.UUID;

@SecuredFactRestController(
    name = "Service Centre Contact Details",
    description = "Operations related to contact details available for service centres"
)
@RequestMapping("/service-centres/{serviceCentreId}")
@RequiredArgsConstructor
public class ServiceCentreContactDetailsController {

    private final ServiceCentreContactDetailsService serviceCentreContactDetailsService;

    @GetMapping("/v1/contact-details")
    @Operation(
        summary = "Get contact details for a service centre",
        description = "Fetch all contact details associated with the supplied service centre ID."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Successfully retrieved contact details"),
        @ApiResponse(responseCode = "400", description = "Invalid service centre ID supplied"),
        @ApiResponse(responseCode = "404", description = "Service centre not found")
    })
    public ResponseEntity<List<ServiceCentreContactDetails>> getContactDetails(
        @Parameter(description = "UUID of the service centre", required = true)
        @ValidUUID @PathVariable String serviceCentreId
    ) {
        return ResponseEntity.ok(
            serviceCentreContactDetailsService.getContactDetails(UUID.fromString(serviceCentreId))
        );
    }

    @GetMapping("/v1/contact-details/{contactId}")
    @Operation(
        summary = "Get a specific contact detail for a service centre",
        description = "Fetch a single contact detail by service centre and contact identifier."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Successfully retrieved the contact detail"),
        @ApiResponse(responseCode = "400", description = "Invalid service centre ID or contact ID supplied"),
        @ApiResponse(responseCode = "404", description = "Contact detail or service centre not found")
    })
    public ResponseEntity<ServiceCentreContactDetails> getContactDetail(
        @Parameter(description = "UUID of the service centre", required = true)
        @ValidUUID @PathVariable String serviceCentreId,
        @Parameter(description = "UUID of the contact", required = true) @ValidUUID @PathVariable String contactId
    ) {
        return ResponseEntity.ok(
            serviceCentreContactDetailsService.getContactDetail(
                UUID.fromString(serviceCentreId),
                UUID.fromString(contactId)
            )
        );
    }

    @PostMapping("/v1/contact-details")
    @Operation(
        summary = "Create contact details for a service centre",
        description = "Create a new contact detail entry for the supplied service centre."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "201", description = "Successfully created the contact detail"),
        @ApiResponse(responseCode = "400", description = "Invalid service centre ID supplied or invalid request body"),
        @ApiResponse(responseCode = "404", description = "Service centre or contact description type not found")
    })
    @PreAuthorize("@authService.isAdmin()")
    public ResponseEntity<ServiceCentreContactDetails> createContactDetail(
        @Parameter(description = "UUID of the service centre", required = true)
        @ValidUUID @PathVariable String serviceCentreId,
        @Parameter(description = "Contact detail to create", required = true)
        @Valid @RequestBody ServiceCentreContactDetails serviceCentreContactDetails
    ) {
        return ResponseEntity
            .status(HttpStatus.CREATED)
            .body(serviceCentreContactDetailsService.createContactDetail(
                UUID.fromString(serviceCentreId),
                serviceCentreContactDetails
            ));
    }

    @PutMapping("/v1/contact-details/{contactId}")
    @Operation(
        summary = "Update contact details for a service centre",
        description = "Update an existing contact detail entry for the supplied service centre."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Successfully updated the contact detail"),
        @ApiResponse(responseCode = "400",
            description = "Invalid service centre ID or contact ID supplied, or invalid request body"),
        @ApiResponse(responseCode = "404",
            description = "Contact detail, service centre, or contact description type not found")
    })
    @PreAuthorize("@authService.isAdmin()")
    public ResponseEntity<ServiceCentreContactDetails> updateContactDetail(
        @Parameter(description = "UUID of the service centre", required = true)
        @ValidUUID @PathVariable String serviceCentreId,
        @Parameter(description = "UUID of the contact", required = true) @ValidUUID @PathVariable String contactId,
        @Parameter(description = "Updated contact detail", required = true)
        @Valid @RequestBody ServiceCentreContactDetails serviceCentreContactDetails
    ) {
        return ResponseEntity.ok(
            serviceCentreContactDetailsService.updateContactDetail(
                UUID.fromString(serviceCentreId),
                UUID.fromString(contactId),
                serviceCentreContactDetails
            )
        );
    }

    @DeleteMapping("/v1/contact-details/{contactId}")
    @Operation(
        summary = "Delete contact details for a service centre",
        description = "Remove an existing contact detail entry for the supplied service centre."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "204", description = "Successfully deleted the contact detail"),
        @ApiResponse(responseCode = "400", description = "Invalid service centre ID or contact ID supplied"),
        @ApiResponse(responseCode = "404", description = "Contact detail or service centre not found")
    })
    @PreAuthorize("@authService.isAdmin()")
    public ResponseEntity<Void> deleteContactDetail(
        @Parameter(description = "UUID of the service centre", required = true)
        @ValidUUID @PathVariable String serviceCentreId,
        @Parameter(description = "UUID of the contact", required = true) @ValidUUID @PathVariable String contactId
    ) {
        serviceCentreContactDetailsService.deleteContactDetail(
            UUID.fromString(serviceCentreId),
            UUID.fromString(contactId)
        );
        return ResponseEntity.noContent().build();
    }
}
