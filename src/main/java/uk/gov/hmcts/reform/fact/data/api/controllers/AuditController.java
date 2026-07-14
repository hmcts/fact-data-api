package uk.gov.hmcts.reform.fact.data.api.controllers;

import uk.gov.hmcts.reform.fact.data.api.entities.Audit;
import uk.gov.hmcts.reform.fact.data.api.entities.types.SubjectType;
import uk.gov.hmcts.reform.fact.data.api.entities.types.NameAndId;
import uk.gov.hmcts.reform.fact.data.api.errorhandling.exceptions.InvalidDateRangeException;
import uk.gov.hmcts.reform.fact.data.api.errorhandling.exceptions.InvalidParameterCombinationException;
import uk.gov.hmcts.reform.fact.data.api.security.SecuredFactRestController;
import uk.gov.hmcts.reform.fact.data.api.services.AuditService;
import uk.gov.hmcts.reform.fact.data.api.validation.annotations.ValidUUID;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;


@SecuredFactRestController(
    name = "Audit",
    description = "Operations related to audits",
    preAuthorize = "@authService.isAdmin()"
)
@RequiredArgsConstructor
@RequestMapping("/audits")
@SuppressWarnings("java:S4684")
public class AuditController {

    private final AuditService auditService;

    @GetMapping("/v1")
    @Operation(
        summary = "Get filtered and paginated list of audits",
        description = "Fetch a paginated and optionally filtered, list of audit records"
            + " that relate to a specific Court or Service Centre"
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
        @RequestParam(name = "subjectType", required = false) SubjectType subjectType,
        @RequestParam(name = "courtId", required = false) @ValidUUID(allowNull = true) String courtId,
        @RequestParam(name = "serviceCentreId", required = false) @ValidUUID(allowNull = true) String serviceCentreId,
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

        if (toDate != null && toDate.isBefore(fromDate)) {
            throw new InvalidDateRangeException("toDate must not be before fromDate");
        }
        if (courtId != null && serviceCentreId != null) {
            throw new InvalidParameterCombinationException("Only one of courtId or serviceCentreId can be provided");
        }

        return ResponseEntity.ok(
            auditService.getFilteredAndPaginatedAudits(
                pageNumber,
                pageSize,
                fromDate,
                toDate,
                subjectType,
                courtId,
                serviceCentreId,
                emailMatch
            )
        );
    }

    @GetMapping("/{auditId}/v1")
    @Operation(
        summary = "Retrieve a single audit record",
        description = "Fetch the audit record that relates to the given id"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Successfully retrieved Audit record"),
        @ApiResponse(responseCode = "400", description = "Invalid request parameters supplied"),
        @ApiResponse(responseCode = "404", description = "Audit record with the given id was not found")
    })
    public ResponseEntity<Audit> getAuditById(
        @ValidUUID @PathVariable String auditId) {
        return ResponseEntity.ok(auditService.getAuditById(UUID.fromString(auditId)));
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

    @GetMapping("/subjectoptions/v1")
    @Operation(
        summary = "Retrieve the complete set of name->id value pairs for all supported audit subjects",
        description = "Fetches a Map of all subject names with their corresponding ids, mapped to their subject type"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Successfully retrieved map of subject-> name+id pairs")
    })
    public ResponseEntity<Map<SubjectType, List<NameAndId>>> getSubjectNameAndIdMap() {
        return ResponseEntity.ok(this.auditService.getSubjectNameAndIdMap());
    }
}
