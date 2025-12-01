package uk.gov.hmcts.reform.fact.data.api.controllers;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import uk.gov.hmcts.reform.fact.data.api.os.OsData;
import uk.gov.hmcts.reform.fact.data.api.os.OsLocationData;
import uk.gov.hmcts.reform.fact.data.api.services.OsService;

@Tag(
    name = "Ordnance Survey",
    description = "Operations to retrieve data from Ordnance Survey for postcodes")
@RestController
@Validated
@RequestMapping("/os")
public class OsController {

    private final OsService osPostcodeService;

    public OsController(OsService osPostcodeService) {
        this.osPostcodeService = osPostcodeService;
    }

    @GetMapping("/v1/postcode")
    @Operation(
        summary = "Get OS data for a postcode",
        description = "Fetches Ordnance Survey data for the supplied postcode"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Successfully retrieved postcode data"),
    })
    public ResponseEntity<OsLocationData> getOsPostcodeData(
        @RequestParam("postcode")
        @NotBlank(message = "Postcode must be provided") String postcode
    ) {
        return ResponseEntity.ok(osPostcodeService.getOsLocationData(postcode));
    }

    @GetMapping("/v1/addresses/full")
    @Operation(
        summary = "Get OS data for a postcode",
        description = "Fetches Ordnance Survey data for the supplied postcode"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Successfully retrieved postcode data"),
    })
    public ResponseEntity<OsData> getOsAddressesFull(
        @RequestParam("postcode")
        @NotBlank(message = "Postcode must be provided") String postcode
    ) {
        return ResponseEntity.ok(osPostcodeService.getOsAddressData(postcode));
    }

    @GetMapping("/v1/addresses/partial")
    @Operation(
        summary = "Get OS data for a postcode",
        description = "Fetches Ordnance Survey data for the supplied postcode"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Successfully retrieved postcode data"),
    })
    public ResponseEntity<OsData> getOsAddressesPartial(
        @RequestParam("postcode")
        @NotBlank(message = "Postcode must be provided") String postcode
    ) {
        return ResponseEntity.ok(osPostcodeService.getOsAddressData(postcode));
    }
}
