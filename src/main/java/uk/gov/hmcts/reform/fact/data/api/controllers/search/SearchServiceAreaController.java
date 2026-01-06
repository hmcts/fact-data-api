package uk.gov.hmcts.reform.fact.data.api.controllers.search;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.NotNull;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import uk.gov.hmcts.reform.fact.data.api.entities.CourtServiceAreas;
import uk.gov.hmcts.reform.fact.data.api.entities.ServiceArea;
import uk.gov.hmcts.reform.fact.data.api.services.CourtServiceAreaService;
import uk.gov.hmcts.reform.fact.data.api.services.ServiceAreaService;
import uk.gov.hmcts.reform.fact.data.api.validation.annotations.ValidUUID;

import java.util.List;

@Tag(name = "Search Service Area", description = "Operations related to the searching of service areas")
@RestController
@Validated
@RequestMapping("/search/service-area")
public class SearchServiceAreaController {

    private final CourtServiceAreaService courtServiceAreaService;

    public SearchServiceAreaController(CourtServiceAreaService courtServiceAreaService) {
        this.courtServiceAreaService = courtServiceAreaService;
    }

    @GetMapping("/v1/{serviceAreaName}")
    @Operation(
        summary = "Search for service area by name.",
        description = "Retrieve Service Area based on provided name."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Successfully retrieved service area based on provided name."),
        @ApiResponse(responseCode = "400", description = "Service name is missing or is not valid."),
        @ApiResponse(responseCode = "404", description = "Information not found for provided service name.")
    })
    public ResponseEntity<List<CourtServiceAreas>> getServiceAreaByName(
        @Parameter(description = "Service area name", required = true)
        @PathVariable String serviceAreaName
    ) {
        return ResponseEntity.ok(courtServiceAreaService.findByServiceAreaName(serviceAreaName));
    }
}
