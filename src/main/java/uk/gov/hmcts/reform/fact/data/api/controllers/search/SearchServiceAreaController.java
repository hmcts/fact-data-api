package uk.gov.hmcts.reform.fact.data.api.controllers.search;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import uk.gov.hmcts.reform.fact.data.api.dto.ServiceAreaSearchResult;
import uk.gov.hmcts.reform.fact.data.api.security.SecuredFactRestController;
import uk.gov.hmcts.reform.fact.data.api.services.search.SearchServiceAreaService;

import java.util.List;

@SecuredFactRestController(
    name = "Search Service Area",
    description = "Operations related to the searching of service areas"
)
@RequestMapping("/search/service-area")
public class SearchServiceAreaController {

    private final SearchServiceAreaService searchServiceAreaService;

    public SearchServiceAreaController(SearchServiceAreaService searchServiceAreaService) {
        this.searchServiceAreaService = searchServiceAreaService;
    }

    @GetMapping("/v1/{serviceAreaName}")
    @Operation(
        summary = "Search for service area by name.",
        description = "Retrieve service-centre search results based on provided service area name."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Successfully retrieved results for service area."),
        @ApiResponse(responseCode = "400", description = "Service area name is missing or is not valid."),
        @ApiResponse(responseCode = "404", description = "Information not found for provided service area name.")
    })
    public ResponseEntity<List<ServiceAreaSearchResult>> getServiceAreaByName(
        @Parameter(description = "Service area name", required = true)
        @PathVariable String serviceAreaName
    ) {
        return ResponseEntity.ok(searchServiceAreaService.findByServiceAreaName(serviceAreaName));
    }
}
