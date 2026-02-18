package uk.gov.hmcts.reform.fact.data.api.controllers.search;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import uk.gov.hmcts.reform.fact.data.api.os.OsData;
import uk.gov.hmcts.reform.fact.data.api.services.OsService;
import uk.gov.hmcts.reform.fact.data.api.validation.annotations.ValidPostcode;

@Tag(name = "Search Address", description = "Operations related to the searching of addresses")
@RestController
@Validated
@RequestMapping("/search/address")
public class SearchAddressController {

    private final OsService osService;

    public SearchAddressController(OsService osService) {
        this.osService = osService;
    }

    @GetMapping("/v1/postcode/{postcode}")
    @Operation(
        summary = "Search for and return all services.",
        description = "Retrieve all services."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200",
            description = "Successfully retrieved address(es) based on provided postcode."),
        @ApiResponse(responseCode = "400", description = "Postcode is missing or is not valid."),
        @ApiResponse(responseCode = "500", description = "OS returned an error when attempting to "
            + "retrieve information about the provided postcode.")
    })
    public ResponseEntity<OsData> getAddressByPostcode(
        @Parameter(description = "A provided postcode", required = true)
        @ValidPostcode
        @NotBlank
        @PathVariable String postcode
    ) {
        return ResponseEntity.ok(osService.getOsAddressByFullPostcode(postcode));
    }
}
