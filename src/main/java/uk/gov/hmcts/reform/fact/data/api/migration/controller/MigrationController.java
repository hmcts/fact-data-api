package uk.gov.hmcts.reform.fact.data.api.migration.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import java.util.UUID;
import uk.gov.hmcts.reform.fact.data.api.migration.model.MigrationResponse;
import uk.gov.hmcts.reform.fact.data.api.migration.model.MigrationReport;
import uk.gov.hmcts.reform.fact.data.api.migration.model.MigrationSummary;
import uk.gov.hmcts.reform.fact.data.api.migration.model.PhotoMigrationResponse;
import uk.gov.hmcts.reform.fact.data.api.migration.service.MigrationService;
import uk.gov.hmcts.reform.fact.data.api.migration.service.PhotoMigrationService;
import uk.gov.hmcts.reform.fact.data.api.security.SecuredFactRestController;

@SecuredFactRestController(
    name = "Migration",
    description = "Endpoints supporting one-off migrations from the legacy FaCT system",
    preAuthorize = "@authService.isAdmin()"
)
@RequestMapping("/migration")
@RequiredArgsConstructor
public class MigrationController {

    private final MigrationService migrationService;
    private final PhotoMigrationService photoMigrationService;

    @PostMapping("/import")
    @Operation(
        summary = "Execute legacy data migration",
        description = "Fetches data from the legacy FaCT private migration endpoint "
            + "and persists it into the new schema."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Migration completed successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid legacy payload or reference data missing"),
        @ApiResponse(responseCode = "409", description = "Migration already applied"),
        @ApiResponse(responseCode = "422", description = "Migration reconciliation rejected data loss"),
        @ApiResponse(responseCode = "500", description = "Unexpected server error")
    })
    public ResponseEntity<MigrationResponse> importLegacyData() {
        MigrationSummary summary = migrationService.migrate();
        return ResponseEntity.ok(new MigrationResponse(
            "Migration completed successfully",
            summary.getResult()
        ));
    }

    @GetMapping("/reports/{reportId}")
    @Operation(
        summary = "Retrieve a legacy migration reconciliation report",
        description = "Returns the durable source, count and finding manifest for a migration run."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Migration report found"),
        @ApiResponse(responseCode = "404", description = "Migration report not found")
    })
    public ResponseEntity<MigrationReport> getMigrationReport(@PathVariable UUID reportId) {
        return ResponseEntity.ok(migrationService.getReport(reportId));
    }

    @PostMapping("/photos")
    @Operation(
        summary = "Migrate court photos from legacy FaCT system",
        description = "Fetches court photos from the legacy FaCT private migration endpoint "
            + "and persists them into the new database and storage account."
    )
    public ResponseEntity<PhotoMigrationResponse> importCourtPhotos() {
        return ResponseEntity.ok(photoMigrationService.migratePhotos());
    }
}
