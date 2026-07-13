package uk.gov.hmcts.reform.fact.data.api.controllers;

import uk.gov.hmcts.reform.fact.data.api.dto.ApprovalStatus;
import uk.gov.hmcts.reform.fact.data.api.entities.Approval;
import uk.gov.hmcts.reform.fact.data.api.security.SecuredFactRestController;
import uk.gov.hmcts.reform.fact.data.api.services.ApprovalService;
import uk.gov.hmcts.reform.fact.data.api.validation.annotations.ValidUUID;

import java.util.List;
import java.util.UUID;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

@SecuredFactRestController(
    name = "Approval",
    description = "Operations related to approvals",
    preAuthorize = "@authService.isAdmin()"
)
@RequiredArgsConstructor
@RequestMapping("/approvals")
@SuppressWarnings("java:S4684")
public class ApprovalController {

    private final ApprovalService approvalService;

    @GetMapping("/v1")
    @Operation(summary = "Get all approvals")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Successfully retrieved approvals")
    })
    public ResponseEntity<List<ApprovalStatus>> getAllApprovals() {
        return ResponseEntity.ok(approvalService.getAllApprovalStatuses());
    }

    @PostMapping("/v1")
    @Operation(summary = "Create an approval")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "201", description = "Successfully created approval"),
        @ApiResponse(responseCode = "400", description = "Invalid request"),
        @ApiResponse(responseCode = "404", description = "User or subject not found")
    })
    public ResponseEntity<Approval> createApproval(@Valid @RequestBody Approval approval) {
        return ResponseEntity.status(HttpStatus.CREATED).body(approvalService.createApproval(approval));
    }

    @DeleteMapping("/{approvalId}/v1")
    @Operation(summary = "Delete an approval by ID")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "204", description = "Successfully deleted approval"),
        @ApiResponse(responseCode = "400", description = "Invalid approval ID supplied"),
        @ApiResponse(responseCode = "404", description = "Approval not found")
    })
    public ResponseEntity<Void> deleteApproval(
        @Parameter(description = "UUID of the approval", required = true) @ValidUUID @PathVariable String approvalId) {
        approvalService.deleteApproval(UUID.fromString(approvalId));
        return ResponseEntity.noContent().build();
    }
}
