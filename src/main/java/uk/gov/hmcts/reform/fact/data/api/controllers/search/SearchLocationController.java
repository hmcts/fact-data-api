package uk.gov.hmcts.reform.fact.data.api.controllers.search;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import uk.gov.hmcts.reform.fact.data.api.dto.SearchResult;
import uk.gov.hmcts.reform.fact.data.api.entities.types.SearchAction;
import uk.gov.hmcts.reform.fact.data.api.security.SecuredFactRestController;
import uk.gov.hmcts.reform.fact.data.api.services.search.SearchLocationService;
import uk.gov.hmcts.reform.fact.data.api.validation.annotations.ValidPostcode;

import java.util.List;

@SecuredFactRestController(
    name = "Search Location",
    description = "Operations related to searching courts and service centres"
)
@RequestMapping("/search/locations")
public class SearchLocationController {

    private final SearchLocationService searchLocationService;

    public SearchLocationController(SearchLocationService searchLocationService) {
        this.searchLocationService = searchLocationService;
    }

    @GetMapping("/v1/postcode")
    @Operation(
        summary = "Search courts and service centres by postcode.",
        description = "Retrieve court and service-centre results based on postcode, service area and action."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Successfully retrieved matching locations."),
        @ApiResponse(responseCode = "400", description = "Postcode is missing or is not valid."),
        @ApiResponse(responseCode = "404", description = "Information not found.")
    })
    public ResponseEntity<List<SearchResult>> getLocationsByPostcode(
        @Parameter(description = "Postcode")
        @ValidPostcode
        @NotBlank
        @RequestParam(value = "postcode")
        final String postcode,

        @Parameter(description = "Service area name")
        @RequestParam(value = "serviceArea", required = false)
        final String serviceArea,

        @Parameter(description = "Action to perform")
        @RequestParam(value = "action", required = false)
        final SearchAction action,

        @Parameter(description = "Maximum number of results (default 10)")
        @RequestParam(value = "limit", required = false, defaultValue = "10")
        @Min(1)
        @Max(50)
        final Integer limit) {

        return ResponseEntity.ok(
            searchLocationService.getLocationsBySearchParameters(postcode, serviceArea, action, limit)
        );
    }
}
