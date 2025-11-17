package uk.gov.hmcts.reform.fact.data.api.migration.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import uk.gov.hmcts.reform.fact.data.api.migration.model.MigrationResponse;
import uk.gov.hmcts.reform.fact.data.api.migration.model.MigrationSummary;
import uk.gov.hmcts.reform.fact.data.api.migration.model.PhotoMigrationResponse;
import uk.gov.hmcts.reform.fact.data.api.migration.service.MigrationService;
import uk.gov.hmcts.reform.fact.data.api.migration.service.PhotoMigrationService;

/**
 * REST endpoint used to trigger a one-off import of data from the legacy FaCT service.
 * The endpoint is intentionally segregated from the public API surface and is expected
 * to be invoked by internal tooling only.
 */
@RestController
@RequestMapping("/migration")
@Tag(name = "Migration", description = "Endpoints supporting one-off migrations from the legacy FaCT system")
public class MigrationController {

    private final MigrationService migrationService;
    private final PhotoMigrationService photoMigrationService;

    public MigrationController(MigrationService migrationService, PhotoMigrationService photoMigrationService) {
        this.migrationService = migrationService;
        this.photoMigrationService = photoMigrationService;
    }

    /**
     * Executes the migration by fetching from the legacy export endpoint and persisting the
     * payload into the new schema. A POST verb is used rather than GET because the operation
     * is non-idempotent (it mutates state, can only run once, and has side-effects).
     *
     * @return summary of the records that were migrated.
     */
    @PostMapping("/import")
    @Operation(
        summary = "Execute legacy data migration",
        description = "Fetches data from the legacy FaCT private migration endpoint "
            + "and persists it into the new schema."
    )
    public ResponseEntity<MigrationResponse> importLegacyData() {
        MigrationSummary summary = migrationService.migrate();
        MigrationResponse response = new MigrationResponse(
            "Migration completed successfully",
            summary.result()
        );
        return ResponseEntity.ok(response);
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
