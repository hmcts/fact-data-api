package uk.gov.hmcts.reform.fact.data.api.controllers;

import uk.gov.hmcts.reform.fact.data.api.entities.Audit;
import uk.gov.hmcts.reform.fact.data.api.services.AuditService;
import uk.gov.hmcts.reform.fact.data.api.validation.annotations.ValidUUID;

import java.time.LocalDate;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RequiredArgsConstructor
@Tag(name = "Audit", description = "Operations related to audits")
@RestController
@Validated
@RequestMapping("/audits")
public class AuditController {

    private final AuditService auditService;

    @GetMapping("/v1")
    @Operation(
        summary = "Get filtered and paginated list of audits for a given Court",
        description = "Fetch a paginated and optionally filtered, list of audit records"
            + " that relate to a specific Court"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Successfully retrieved list of audits"),
        @ApiResponse(responseCode = "400", description = "Invalid request parameters supplied")
    })
    public ResponseEntity<Page<Audit>> getFilteredAndPaginatedAudits(
        @RequestParam(name = "pageNumber", defaultValue = "0")
        @PositiveOrZero(message = "pageNumber must be greater than or equal to 0") int pageNumber,
        @RequestParam(name = "pageSize", defaultValue = "25")
        @Positive(message = "pageSize must be greater than 0") int pageSize,
        @RequestParam(name = "courtId", required = false) @ValidUUID(allowNull = true) String courtId,
        @RequestParam(name = "email", required = false)
        @Pattern(
            regexp = "^[A-Za-z0-9._+-]*(|@[A-Za-z0-9._+-]*)$",
            message = "email match may only contain letters, hyphens, periods, plus/minus signs, "
                + "underscores, and a single 'at' (@) symbol")
        @Parameter(name = "email", description = "Full or partial email for result filtering")
        String emailMatch,
        @RequestParam(name = "fromDate")
        @Parameter(name = "fromDate", required = true, description = "'From' date (start of day) for result filtering")
        LocalDate fromDate,
        @Parameter(name = "toDate", description = "'To' date (end of day) for result filtering")
        @RequestParam(name = "toDate", required = false) LocalDate toDate) {
        return ResponseEntity.ok(
            auditService.getFilteredAndPaginatedAudits(
                pageNumber,
                pageSize,
                fromDate,
                toDate,
                courtId,
                emailMatch
            )
        );
    }

    @DeleteMapping("/v1")
    @Operation(
        summary = "Remove expired audits",
        description = "Starts an expired audit removal process."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "204", description = "The request to remove expired audits has completed")
    })
    public ResponseEntity<Void> removeExpiredAuditEntries() {
        auditService.removeExpiredAuditEntries();
        return ResponseEntity.noContent().build();
    }

}
