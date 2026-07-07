package uk.gov.hmcts.reform.fact.data.api.controllers;

import uk.gov.hmcts.reform.fact.data.api.aspect.annotations.LockCleanupCheck;
import uk.gov.hmcts.reform.fact.data.api.entities.Lock;
import uk.gov.hmcts.reform.fact.data.api.entities.types.SubjectType;
import uk.gov.hmcts.reform.fact.data.api.entities.types.Page;
import uk.gov.hmcts.reform.fact.data.api.security.SecuredFactRestController;
import uk.gov.hmcts.reform.fact.data.api.services.LockService;
import uk.gov.hmcts.reform.fact.data.api.aspect.annotations.LockTimeoutCheck;
import uk.gov.hmcts.reform.fact.data.api.validation.annotations.ValidUUID;

import java.util.List;
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
    name = "Lock",
    description = "Operations related to lock services"
)
@RequestMapping("/locks")
@SuppressWarnings("java:S4684")
public class LockController {

    private final LockService lockService;

    public LockController(LockService lockService) {
        this.lockService = lockService;
    }

    @LockCleanupCheck
    @GetMapping("/{subjectType}/{subjectId}/v1")
    @Operation(summary = "Get all active locks for a subject")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Successfully retrieved subject locks"),
        @ApiResponse(responseCode = "400", description = "Invalid subject ID supplied"),
        @ApiResponse(responseCode = "404", description = "Subject not found")
    })
    public ResponseEntity<List<Lock>> getSubjectLocks(
        @Parameter(description = "The subject type", required = true) @PathVariable SubjectType subjectType,
        @Parameter(description = "UUID of the subject", required = true) @ValidUUID @PathVariable String subjectId) {
        return ResponseEntity.ok(lockService.getAllSubjectLocks(subjectType, UUID.fromString(subjectId)));
    }

    @LockCleanupCheck
    @GetMapping("/{subjectType}/{subjectId}/v1/{page}")
    @Operation(summary = "Get lock status for a specific page of a subject")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Successfully retrieved lock status"),
        @ApiResponse(responseCode = "204", description = "No lock data found for the specified page"),
        @ApiResponse(responseCode = "400", description = "Invalid subject ID or page supplied"),
        @ApiResponse(responseCode = "404", description = "Subject not found")
    })
    public ResponseEntity<Lock> getSubjectLockStatus(
        @Parameter(description = "The subject type", required = true) @PathVariable SubjectType subjectType,
        @Parameter(description = "UUID of the subject", required = true) @ValidUUID @PathVariable String subjectId,
        @Parameter(description = "Page to check lock status", required = true) @PathVariable Page page) {
        return lockService.getPageLock(subjectType, UUID.fromString(subjectId), page)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.noContent().build());
    }

    @LockTimeoutCheck
    @PostMapping("/{subjectType}/{subjectId}/v1/{page}")
    @Operation(summary = "Create or update subject lock for a specific page")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "201", description = "Successfully created or updated subject lock"),
        @ApiResponse(responseCode = "400", description = "Invalid subject ID, page or user ID supplied"),
        @ApiResponse(responseCode = "404", description = "Subject or user not found"),
        @ApiResponse(responseCode = "409", description = "Conflict with existing subject lock")
    })
    @PreAuthorize("@authService.isAdmin()")
    public ResponseEntity<Lock> createOrUpdateSubjectLock(
        @Parameter(description = "The subject type", required = true) @PathVariable SubjectType subjectType,
        @Parameter(description = "UUID of the subject", required = true) @ValidUUID @PathVariable String subjectId,
        @Parameter(description = "Page to lock", required = true) @PathVariable Page page,
        @Parameter(description = "User ID creating the lock", required = true) @Valid @RequestBody UUID userId) {
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(lockService.createOrUpdateLock(subjectType, UUID.fromString(subjectId), page, userId));
    }

    @DeleteMapping("/{subjectType}/{subjectId}/v1/{page}")
    @Operation(summary = "Delete a subject lock for a specific page")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "204", description = "Successfully deleted subject lock"),
        @ApiResponse(responseCode = "400", description = "Invalid subject ID or page supplied"),
        @ApiResponse(responseCode = "404", description = "Subject or lock not found")
    })
    @PreAuthorize("@authService.isAdmin()")
    public ResponseEntity<Void> deleteSubjectLock(
        @Parameter(description = "The subject type", required = true) @PathVariable SubjectType subjectType,
        @Parameter(description = "UUID of the subject", required = true) @ValidUUID @PathVariable String subjectId,
        @Parameter(description = "Page to delete lock", required = true) @PathVariable Page page) {
        lockService.deleteLock(subjectType, UUID.fromString(subjectId), page);
        return ResponseEntity.noContent().build();
    }
}
