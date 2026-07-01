package uk.gov.hmcts.reform.fact.data.api.controllers;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import uk.gov.hmcts.reform.fact.data.api.entities.Court;
import uk.gov.hmcts.reform.fact.data.api.entities.CourtDetails;
import uk.gov.hmcts.reform.fact.data.api.security.SecuredFactRestController;
import uk.gov.hmcts.reform.fact.data.api.services.CourtDetailsViewService;
import uk.gov.hmcts.reform.fact.data.api.services.CourtService;
import uk.gov.hmcts.reform.fact.data.api.validation.annotations.ValidCourtSlug;
import uk.gov.hmcts.reform.fact.data.api.validation.annotations.ValidUUID;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonView;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

@SecuredFactRestController(
    name = "Court",
    description = "Operations related to courts"
)
@RequestMapping("/courts")
@RequiredArgsConstructor
@SuppressWarnings("java:S4684")
public class CourtController {

    /**
     * JsonView needs a marker type so we can expand the payload only on the details endpoint,
     * without changing list/search responses.
     */
    public interface CourtDetailsView {
    }

    private final CourtService courtService;
    private final CourtDetailsViewService courtDetailsViewService;

    @GetMapping(value = {"/{courtId}/v1", "/{courtId}.json"})
    @Operation(
        summary = "Get court details by ID",
        description = "Fetch detailed court information for a given court ID."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Successfully retrieved court details"),
        @ApiResponse(responseCode = "400", description = "Invalid court ID supplied"),
        @ApiResponse(responseCode = "404", description = "Court not found")
    })
    public ResponseEntity<CourtDetails> getCourtDetailsById(
        @Parameter(description = "UUID of the court", required = true)
        @ValidUUID @PathVariable String courtId) {
        return ResponseEntity.ok(courtService.getCourtDetailsById(UUID.fromString(courtId)));
    }

    @GetMapping(value = "/{courtId}/entity/v1")
    @Operation(
        summary = "Get court entity by ID",
        description = "Fetch the court entity for a given court ID."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Successfully retrieved court entity"),
        @ApiResponse(responseCode = "400", description = "Invalid court ID supplied"),
        @ApiResponse(responseCode = "404", description = "Court not found")
    })
    public ResponseEntity<Court> getCourtById(
        @Parameter(description = "UUID of the court", required = true)
        @ValidUUID @PathVariable String courtId) {
        return ResponseEntity.ok(courtService.getCourtById(UUID.fromString(courtId)));
    }

    @GetMapping(value = "/name/v1")
    @Operation(
        summary = "Get court entity by exact name",
        description = "Fetch the court entity for a given exact court name."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Successfully retrieved court entity"),
        @ApiResponse(responseCode = "400", description = "Invalid court name supplied"),
        @ApiResponse(responseCode = "404", description = "Court not found")
    })
    public ResponseEntity<Court> getCourtByName(
        @Parameter(description = "Exact name of the court", required = true)
        @NotBlank(message = "name must not be blank")
        @Size(max = 250, message = "name must be less than 250 characters")
        @RequestParam(name = "name") String name) {
        return ResponseEntity.ok(courtService.getCourtByName(name));
    }

    @GetMapping(value = {"/slug/{courtSlug}/v1", "/slug/{courtSlug}.json"})
    @JsonView(CourtDetailsView.class)
    @Operation(
        summary = "Get court details by slug",
        description = "Fetch detailed court information for a given court slug."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Successfully retrieved court details"),
        @ApiResponse(responseCode = "400", description = "Invalid court slug supplied"),
        @ApiResponse(responseCode = "404", description = "Court not found")
    })
    public ResponseEntity<CourtDetails> getCourtDetailsBySlug(
        @Parameter(description = "Slug of the court", required = true)
        @ValidCourtSlug
        @PathVariable String courtSlug) {
        return ResponseEntity.ok(
            courtDetailsViewService.prepareDetailsView(courtService.getCourtDetailsBySlug(courtSlug))
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

    @PostMapping("/v1/link")
    @Operation(
        summary = "Link CaTH courts to FaCT",
        description = "Associates courts in CaTH with FACT data."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Response of matched and unmatched courts"),
        @ApiResponse(responseCode = "400", description = "Invalid linking data supplied")
    })
    @PreAuthorize("@authService.isAdmin()") //TODO: CaTH role
    public ResponseEntity<Map<String, Object>> linkCaTHCourtsToFaCT(
        @RequestBody @NotEmpty(message = "mrdIds cannot be empty")
        List<@NotBlank(message = "mrdId cannot be blank") String> mrdIds) {
        return ResponseEntity.ok(courtService.linkCathCourtsToFact(mrdIds));
    }

    @PutMapping("/v1/link/{mrdId}")
    @Operation(
        summary = "Called when a court has been deleted on CaTH",
        description = "Handles the deletion of the link between CaTH and FACT for a deleted court."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "204", description = "Successfully handled link deletion"),
        @ApiResponse(responseCode = "400", description = "Invalid MRD ID supplied"),
        @ApiResponse(responseCode = "404", description = "Court with given MRD ID not found")
    })
    @PreAuthorize("@authService.isAdmin()") //TODO: CaTH role
    public ResponseEntity<Void> handleCaTHCourtDeletion(
        @Parameter(description = "MRD ID of the deleted court", required = true)
        @NotBlank(message = "mrdId cannot be blank") @PathVariable String mrdId
    ) {
        courtService.handleCathCourtDeletion(mrdId);
        return ResponseEntity.noContent().build();
    }
}
