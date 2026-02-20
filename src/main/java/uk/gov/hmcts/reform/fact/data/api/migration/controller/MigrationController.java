package uk.gov.hmcts.reform.fact.data.api.migration.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import uk.gov.hmcts.reform.fact.data.api.migration.model.MigrationResponse;
import uk.gov.hmcts.reform.fact.data.api.migration.model.MigrationSummary;
import uk.gov.hmcts.reform.fact.data.api.migration.service.MigrationService;

@RestController
@RequestMapping("/migration")
@Tag(name = "Migration", description = "Endpoints supporting one-off migrations from the legacy FaCT system")
public class MigrationController {

    private final MigrationService migrationService;

    public MigrationController(MigrationService migrationService) {
        this.migrationService = migrationService;
    }

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
        @ApiResponse(responseCode = "500", description = "Unexpected server error")
    })
    public ResponseEntity<MigrationResponse> importLegacyData() {
        MigrationSummary summary = migrationService.migrate();
        return ResponseEntity.ok(new MigrationResponse(
            "Migration completed successfully",
            summary.getResult()
        ));
    }
}
