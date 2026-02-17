package uk.gov.hmcts.reform.fact.data.api.controllers;

import uk.gov.hmcts.reform.fact.data.api.entities.CourtLock;
import uk.gov.hmcts.reform.fact.data.api.entities.types.Page;
import uk.gov.hmcts.reform.fact.data.api.security.SecuredFactRestController;
import uk.gov.hmcts.reform.fact.data.api.services.CourtLockService;
import uk.gov.hmcts.reform.fact.data.api.validation.annotations.CourtLockTimeoutCheck;
import uk.gov.hmcts.reform.fact.data.api.validation.annotations.ValidUUID;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

@SecuredFactRestController(
    name = "Court Lock",
    description = "Operations related to court lock services"
)
@RequestMapping("/courts/{courtId}")
public class CourtLockController {

    private final CourtLockService courtLockService;

    public CourtLockController(CourtLockService courtLockService) {
        this.courtLockService = courtLockService;
    }

    @GetMapping("/v1/locks")
    @Operation(summary = "Get all active court locks for a court")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Successfully retrieved court locks"),
        @ApiResponse(responseCode = "400", description = "Invalid court ID supplied"),
        @ApiResponse(responseCode = "404", description = "Court not found")
    })
    public ResponseEntity<List<CourtLock>> getCourtLocks(
        @Parameter(description = "UUID of the court", required = true) @ValidUUID @PathVariable String courtId) {
        return ResponseEntity.ok(courtLockService.getLocksByCourtId(UUID.fromString(courtId)));
    }

    @GetMapping("/v1/locks/{page}")
    @Operation(summary = "Get lock status for a specific page of a court")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Successfully retrieved lock status"),
        @ApiResponse(responseCode = "400", description = "Invalid court ID or page supplied"),
        @ApiResponse(responseCode = "404", description = "Court not found")
    })
    public ResponseEntity<Optional<CourtLock>> getCourtLockStatus(
        @Parameter(description = "UUID of the court", required = true) @ValidUUID @PathVariable String courtId,
        @Parameter(description = "Page to check lock status", required = true) @PathVariable Page page) {
        return ResponseEntity.ok(courtLockService.getPageLock(UUID.fromString(courtId), page));
    }

    @PostMapping("/v1/locks/{page}")
    @CourtLockTimeoutCheck
    @Operation(summary = "Create or update court lock for a specific page")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "201", description = "Successfully created or updated court lock"),
        @ApiResponse(responseCode = "400", description = "Invalid court ID, page or user ID supplied"),
        @ApiResponse(responseCode = "404", description = "Court or user not found"),
        @ApiResponse(responseCode = "409", description = "Conflict with existing court lock")
    })
    @PreAuthorize("@authService.isAdmin()")
    public ResponseEntity<CourtLock> createOrUpdateCourtLock(
        @Parameter(description = "UUID of the court", required = true) @ValidUUID @PathVariable String courtId,
        @Parameter(description = "Page to lock", required = true) @PathVariable Page page,
        @Parameter(description = "User ID creating the lock", required = true) @Valid @RequestBody UUID userId) {
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(courtLockService.createOrUpdateLock(UUID.fromString(courtId), page, userId));
    }

    @DeleteMapping("/v1/locks/{page}")
    @Operation(summary = "Delete a court lock for a specific page")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "204", description = "Successfully deleted court lock"),
        @ApiResponse(responseCode = "400", description = "Invalid court ID or page supplied"),
        @ApiResponse(responseCode = "404", description = "Court or lock not found")
    })
    @PreAuthorize("@authService.isAdmin()")
    public ResponseEntity<Void> deleteCourtLock(
        @Parameter(description = "UUID of the court", required = true) @ValidUUID @PathVariable String courtId,
        @Parameter(description = "Page to delete lock", required = true) @PathVariable Page page) {
        courtLockService.deleteLock(UUID.fromString(courtId), page);
        return ResponseEntity.noContent().build();
    }
}
