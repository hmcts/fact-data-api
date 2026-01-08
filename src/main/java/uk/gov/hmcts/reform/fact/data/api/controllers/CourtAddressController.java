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
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import uk.gov.hmcts.reform.fact.data.api.entities.CourtAddress;
import uk.gov.hmcts.reform.fact.data.api.services.CourtAddressService;
import uk.gov.hmcts.reform.fact.data.api.validation.annotations.ValidUUID;

import java.util.List;
import java.util.UUID;

@Tag(name = "Court Addresses", description = "Operations related to addresses available for courts")
@RestController
@Validated
@RequestMapping("/courts/{courtId}")
public class CourtAddressController {

    private final CourtAddressService courtAddressService;

    public CourtAddressController(CourtAddressService courtAddressService) {
        this.courtAddressService = courtAddressService;
    }

    @GetMapping("/v1/address")
    @Operation(
        summary = "Get addresses for a court",
        description = "Fetch all addresses associated with the supplied court ID."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Successfully retrieved addresses"),
        @ApiResponse(responseCode = "400", description = "Invalid court ID supplied"),
        @ApiResponse(responseCode = "404", description = "Court not found")
    })
    public ResponseEntity<List<CourtAddress>> getAddresses(
        @Parameter(description = "UUID of the court", required = true) @ValidUUID @PathVariable String courtId
    ) {
        return ResponseEntity.ok(
            courtAddressService.getAddresses(UUID.fromString(courtId))
        );
    }

    @GetMapping("/v1/address/{addressId}")
    @Operation(
        summary = "Get a specific address for a court",
        description = "Fetch a single address by court and address identifier."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Successfully retrieved the address"),
        @ApiResponse(responseCode = "400", description = "Invalid court ID or address ID supplied"),
        @ApiResponse(responseCode = "404", description = "Address or court not found")
    })
    public ResponseEntity<CourtAddress> getAddress(
        @Parameter(description = "UUID of the court", required = true) @ValidUUID @PathVariable String courtId,
        @Parameter(description = "UUID of the address", required = true) @ValidUUID @PathVariable String addressId
    ) {
        return ResponseEntity.ok(
            courtAddressService.getAddress(
                UUID.fromString(courtId),
                UUID.fromString(addressId)
            )
        );
    }

    @PostMapping("/v1/address")
    @Operation(
        summary = "Create address for a court",
        description = "Create a new address entry for the supplied court."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "201", description = "Successfully created the address"),
        @ApiResponse(responseCode = "400", description = "Invalid court ID supplied or invalid request body"),
        @ApiResponse(responseCode = "404", description = "Court not found")
    })
    public ResponseEntity<CourtAddress> createAddress(
        @Parameter(description = "UUID of the court", required = true) @ValidUUID @PathVariable String courtId,
        @Parameter(description = "Address to create", required = true)
        @Valid @RequestBody CourtAddress courtAddress
    ) {
        return ResponseEntity
            .status(HttpStatus.CREATED)
            .body(courtAddressService.createAddress(UUID.fromString(courtId), courtAddress));
    }

    @PutMapping("/v1/address/{addressId}")
    @Operation(
        summary = "Update address for a court",
        description = "Update an existing address entry for the supplied court."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Successfully updated the address"),
        @ApiResponse(responseCode = "400",
            description = "Invalid court ID or address ID supplied, or invalid request body"),
        @ApiResponse(responseCode = "404", description = "Address or court not found")
    })
    public ResponseEntity<CourtAddress> updateCourtAddress(
        @Parameter(description = "UUID of the court", required = true) @ValidUUID @PathVariable String courtId,
        @Parameter(description = "UUID of the address", required = true) @ValidUUID @PathVariable String addressId,
        @Parameter(description = "Updated address", required = true)
        @Valid @RequestBody CourtAddress courtAddress
    ) {
        return ResponseEntity.ok(
            courtAddressService.updateAddress(
                UUID.fromString(courtId),
                UUID.fromString(addressId),
                courtAddress
            )
        );
    }

    @DeleteMapping("/v1/address/{addressId}")
    @Operation(
        summary = "Delete address for a court",
        description = "Remove an existing address entry for the supplied court."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "204", description = "Successfully deleted the address"),
        @ApiResponse(responseCode = "400", description = "Invalid court ID or address ID supplied"),
        @ApiResponse(responseCode = "404", description = "Address or court not found")
    })
    public ResponseEntity<Void> deleteCourtAddress(
        @Parameter(description = "UUID of the court", required = true) @ValidUUID @PathVariable String courtId,
        @Parameter(description = "UUID of the address", required = true) @ValidUUID @PathVariable String addressId
    ) {
        courtAddressService.deleteAddress(UUID.fromString(courtId), UUID.fromString(addressId));
        return ResponseEntity.noContent().build();
    }
}
