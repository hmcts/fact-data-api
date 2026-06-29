package uk.gov.hmcts.reform.fact.data.api.controllers;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import uk.gov.hmcts.reform.fact.data.api.dto.AllLocation;
import uk.gov.hmcts.reform.fact.data.api.dto.AllLocationDetails;
import uk.gov.hmcts.reform.fact.data.api.security.SecuredFactRestController;
import uk.gov.hmcts.reform.fact.data.api.services.AllLocationService;
import uk.gov.hmcts.reform.fact.data.api.validation.annotations.ValidUUID;

import java.util.List;

@SecuredFactRestController(
    name = "All Locations",
    description = "Operations related to combined courts and service centres"
)
@RequiredArgsConstructor
@SuppressWarnings("java:S4684")
public class AllLocationController {

    private final AllLocationService allLocationService;

    @GetMapping("/all/v1")
    @Operation(summary = "Get filtered and paginated courts and service centres")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Successfully retrieved locations"),
        @ApiResponse(responseCode = "400", description = "Invalid request parameters supplied")
    })
    public ResponseEntity<Page<AllLocation>> getFilteredAndPaginatedLocations(
        @RequestParam(name = "pageNumber", defaultValue = "0")
        @PositiveOrZero(message = "pageNumber must be greater than or equal to 0") int pageNumber,
        @RequestParam(name = "pageSize", defaultValue = "25")
        @Positive(message = "pageSize must be greater than 0") int pageSize,
        @RequestParam(name = "includeClosed", required = false) Boolean includeClosed,
        @RequestParam(name = "onlyServiceCentres", required = false) Boolean onlyServiceCentres,
        @RequestParam(name = "regionId", required = false) @ValidUUID(allowNull = true) String regionId,
        @RequestParam(name = "partialCourtName", required = false)
        @Size(max = 250, message = "Partial court name must be less than 250 characters")
        @Pattern(
            regexp = "^[A-Za-z&'()\\- ]*$",
            message = "Partial court name may only contain letters, spaces, apostrophes, hyphens, ampersands, "
                + "and parentheses"
        )
        String partialCourtName,
        @RequestParam(name = "sortBy", required = false) String sortBy,
        @RequestParam(name = "sortOrder", required = false) String sortOrder) {
        return ResponseEntity.ok(allLocationService.getFilteredAndPaginatedLocations(
            pageNumber,
            pageSize,
            includeClosed,
            onlyServiceCentres,
            regionId,
            partialCourtName,
            sortBy,
            sortOrder
        ));
    }

    @GetMapping(value = {"/all/details/v1", "/all/details.json"})
    @Operation(summary = "Get all court and service centre details")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Successfully retrieved location details")
    })
    public ResponseEntity<List<AllLocationDetails>> getAllLocationDetails() {
        return ResponseEntity.ok(allLocationService.getAllLocationDetails());
    }

}
