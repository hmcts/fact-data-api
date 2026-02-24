package uk.gov.hmcts.reform.fact.data.api.controllers;

import uk.gov.hmcts.reform.fact.data.api.services.CourtService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Size;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Testing Support", description = "Utility endpoints for cleaning automated test data")
@RestController
@Validated
@RequestMapping("/testing-support")
@ConditionalOnProperty(prefix = "testingSupport", name = "enableApi", havingValue = "true")
public class TestingSupportController {

    private final CourtService courtService;

    public TestingSupportController(CourtService courtService) {
        this.courtService = courtService;
    }

    @DeleteMapping("/courts/name-prefix/{courtNamePrefix}")
    @Operation(
        summary = "Bulk delete courts by name prefix",
        description = "Deletes all courts whose names start with the supplied prefix (case-insensitive)."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Successfully deleted matching courts"),
        @ApiResponse(responseCode = "400", description = "Invalid court name prefix supplied")
    })
    public ResponseEntity<String> deleteCourtsByNamePrefix(
        @PathVariable
        @Size(min = 1, max = 200, message = "Court name prefix must be between 1 and 200 characters")
        String courtNamePrefix
    ) {
        return ResponseEntity.status(HttpStatus.OK)
            .body(courtService.deleteCourtsByNamePrefix(courtNamePrefix)
                      + " court(s) with prefix " + courtNamePrefix + " deleted successfully");
    }
}
