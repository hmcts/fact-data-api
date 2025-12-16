package uk.gov.hmcts.reform.fact.data.api.controllers;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import uk.gov.hmcts.reform.fact.data.api.config.OpenAPIConfiguration;
import uk.gov.hmcts.reform.fact.data.api.entities.Court;
import uk.gov.hmcts.reform.fact.data.api.services.CourtService;
import uk.gov.hmcts.reform.fact.data.api.validation.annotations.ValidUUID;

import java.util.UUID;

@Tag(name = "Court", description = "Operations related to courts")
@RestController
@Validated
@RequestMapping("/courts")
@SecurityRequirement(name = OpenAPIConfiguration.BEARER_AUTH_SECURITY_SCHEME)
@PreAuthorize("@authService.isViewer()")
public class CourtController {

    private final CourtService courtService;

    public CourtController(CourtService courtService) {
        this.courtService = courtService;
    }

    @GetMapping("/{courtId}/v1")
    @Operation(
        summary = "Get court by ID",
        description = "Fetch court information for a given court ID."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Successfully retrieved court"),
        @ApiResponse(responseCode = "400", description = "Invalid court ID supplied"),
        @ApiResponse(responseCode = "404", description = "Court not found")
    })
    public ResponseEntity<Court> getCourtById(@Parameter(description = "UUID of the court", required = true)
                                                  @ValidUUID @PathVariable String courtId) {
        return ResponseEntity.ok(courtService.getCourtById(UUID.fromString(courtId)));
    }

    @GetMapping("/v1")
    @Operation(
        summary = "Get filtered and paginated list of courts",
        description = "Fetch a paginated list of courts with optional filters."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Successfully retrieved list of courts"),
        @ApiResponse(responseCode = "400", description = "Invalid request parameters supplied")
    })
    public ResponseEntity<Page<Court>> getFilteredAndPaginatedCourts(
        @RequestParam(name = "pageNumber", defaultValue = "0")
        @PositiveOrZero(message = "pageNumber must be greater than or equal to 0") int pageNumber,
        @RequestParam(name = "pageSize", defaultValue = "25")
        @Positive(message = "pageSize must be greater than 0") int pageSize,
        @RequestParam(name = "includeClosed", required = false) Boolean includeClosed,
        @RequestParam(name = "regionId", required = false) @ValidUUID(allowNull = true) String regionId,
        @RequestParam(name = "partialCourtName", required = false)
        @Size(max = 250, message = "Partial court name must be less than 250 characters")
        @Pattern(
            regexp = "^[A-Za-z&'()\\- ]*$",
            message = "Partial court name may "
                + "only contain letters, spaces, apostrophes, hyphens, ampersands, and parentheses"
        )
        String partialCourtName) {
        Pageable pageable = PageRequest.of(pageNumber, pageSize);
        return ResponseEntity.ok(
            courtService.getFilteredAndPaginatedCourts(
                pageable,
                includeClosed,
                regionId,
                partialCourtName
            )
        );
    }


    @PostMapping(value = "/v1",
        consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(
        summary = "Create a new court",
        description = "Creates a new court record."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "201", description = "Successfully created court"),
        @ApiResponse(responseCode = "400", description = "Invalid court data supplied"),
        @ApiResponse(responseCode = "404", description = "Associated region not found")
    })
    @PreAuthorize("@authService.isAdmin()")
    public ResponseEntity<Court> createCourt(@Valid @RequestBody Court court) {
        return ResponseEntity.status(HttpStatus.CREATED).body(courtService.createCourt(court));
    }

    @PutMapping(value = "/{courtId}/v1",
        consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(
        summary = "Update an existing court",
        description = "Updates the details of an existing court."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Successfully updated court"),
        @ApiResponse(responseCode = "400", description = "Invalid court data supplied"),
        @ApiResponse(responseCode = "404", description = "Court or associated region not found")
    })
    @PreAuthorize("@authService.isAdmin()")
    public ResponseEntity<Court> updateCourt(@ValidUUID @PathVariable String courtId, @Valid @RequestBody Court court) {
        return ResponseEntity.ok(courtService.updateCourt(UUID.fromString(courtId), court));
    }
}
