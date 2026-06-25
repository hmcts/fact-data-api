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
import uk.gov.hmcts.reform.fact.data.api.entities.ServiceCentreAddress;
import uk.gov.hmcts.reform.fact.data.api.security.SecuredFactRestController;
import uk.gov.hmcts.reform.fact.data.api.services.ServiceCentreAddressService;
import uk.gov.hmcts.reform.fact.data.api.validation.annotations.ValidUUID;

import java.util.List;
import java.util.UUID;

@SecuredFactRestController(
    name = "Service Centre Addresses",
    description = "Operations related to addresses available for service centres"
)
@RequestMapping("/service-centres/{serviceCentreId}")
@RequiredArgsConstructor
@SuppressWarnings("java:S4684")
public class ServiceCentreAddressController {

    private final ServiceCentreAddressService serviceCentreAddressService;

    @GetMapping("/v1/address")
    @Operation(
        summary = "Get addresses for a service centre",
        description = "Fetch all addresses associated with the supplied service centre ID."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Successfully retrieved addresses"),
        @ApiResponse(responseCode = "400", description = "Invalid service centre ID supplied"),
        @ApiResponse(responseCode = "404", description = "Service centre not found")
    })
    public ResponseEntity<List<ServiceCentreAddress>> getAddresses(
        @Parameter(description = "UUID of the service centre", required = true)
        @ValidUUID @PathVariable String serviceCentreId
    ) {
        return ResponseEntity.ok(
            serviceCentreAddressService.getAddresses(UUID.fromString(serviceCentreId))
        );
    }

    @GetMapping("/v1/address/{addressId}")
    @Operation(
        summary = "Get a specific address for a service centre",
        description = "Fetch a single address by service centre and address identifier."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Successfully retrieved the address"),
        @ApiResponse(responseCode = "400", description = "Invalid service centre ID or address ID supplied"),
        @ApiResponse(responseCode = "404", description = "Address or service centre not found")
    })
    public ResponseEntity<ServiceCentreAddress> getAddress(
        @Parameter(description = "UUID of the service centre", required = true)
        @ValidUUID @PathVariable String serviceCentreId,
        @Parameter(description = "UUID of the address", required = true) @ValidUUID @PathVariable String addressId
    ) {
        return ResponseEntity.ok(
            serviceCentreAddressService.getAddress(
                UUID.fromString(serviceCentreId),
                UUID.fromString(addressId)
            )
        );
    }

    @PostMapping("/v1/address")
    @Operation(
        summary = "Create address for a service centre",
        description = "Create a new address entry for the supplied service centre."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "201", description = "Successfully created the address"),
        @ApiResponse(responseCode = "400", description = "Invalid service centre ID supplied or invalid request body"),
        @ApiResponse(responseCode = "404", description = "Service centre not found")
    })
    @PreAuthorize("@authService.isAdmin()")
    public ResponseEntity<ServiceCentreAddress> createAddress(
        @Parameter(description = "UUID of the service centre", required = true)
        @ValidUUID @PathVariable String serviceCentreId,
        @Parameter(description = "Address to create", required = true)
        @Valid @RequestBody ServiceCentreAddress serviceCentreAddress
    ) {
        return ResponseEntity
            .status(HttpStatus.CREATED)
            .body(serviceCentreAddressService.createAddress(UUID.fromString(serviceCentreId), serviceCentreAddress));
    }

    @PutMapping("/v1/address/{addressId}")
    @Operation(
        summary = "Update address for a service centre",
        description = "Update an existing address entry for the supplied service centre."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Successfully updated the address"),
        @ApiResponse(responseCode = "400",
            description = "Invalid service centre ID or address ID supplied, or invalid request body"),
        @ApiResponse(responseCode = "404", description = "Address or service centre not found")
    })
    @PreAuthorize("@authService.isAdmin()")
    public ResponseEntity<ServiceCentreAddress> updateServiceCentreAddress(
        @Parameter(description = "UUID of the service centre", required = true)
        @ValidUUID @PathVariable String serviceCentreId,
        @Parameter(description = "UUID of the address", required = true) @ValidUUID @PathVariable String addressId,
        @Parameter(description = "Updated address", required = true)
        @Valid @RequestBody ServiceCentreAddress serviceCentreAddress
    ) {
        return ResponseEntity.ok(
            serviceCentreAddressService.updateAddress(
                UUID.fromString(serviceCentreId),
                UUID.fromString(addressId),
                serviceCentreAddress
            )
        );
    }

    @DeleteMapping("/v1/address/{addressId}")
    @Operation(
        summary = "Delete address for a service centre",
        description = "Remove an existing address entry for the supplied service centre."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "204", description = "Successfully deleted the address"),
        @ApiResponse(responseCode = "400", description = "Invalid service centre ID or address ID supplied"),
        @ApiResponse(responseCode = "404", description = "Address or service centre not found")
    })
    @PreAuthorize("@authService.isAdmin()")
    public ResponseEntity<Void> deleteServiceCentreAddress(
        @Parameter(description = "UUID of the service centre", required = true)
        @ValidUUID @PathVariable String serviceCentreId,
        @Parameter(description = "UUID of the address", required = true) @ValidUUID @PathVariable String addressId
    ) {
        serviceCentreAddressService.deleteAddress(UUID.fromString(serviceCentreId), UUID.fromString(addressId));
        return ResponseEntity.noContent().build();
    }
}
