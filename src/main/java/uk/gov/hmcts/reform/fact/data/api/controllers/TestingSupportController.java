package uk.gov.hmcts.reform.fact.data.api.controllers;

import uk.gov.hmcts.reform.fact.data.api.entities.CourtDetails;
import uk.gov.hmcts.reform.fact.data.api.services.CourtService;
import uk.gov.hmcts.reform.fact.data.api.services.TestingSupportService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;


@Tag(name = "Testing Support", description = "Utility endpoints for cleaning automated test data")
@RestController
@Validated
@RequestMapping("/testing-support")
@ConditionalOnProperty(prefix = "testingSupport", name = "enableApi", havingValue = "true")
@RequiredArgsConstructor
@SuppressWarnings("java:S4684")
public class TestingSupportController {

    private final CourtService courtService;
    private final TestingSupportService testingSupportService;

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

    @PostMapping("/courts")
    @Operation(
        summary = "Create sample court",
        description = "Creates a sample court with randomised data for testing purposes."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "201", description = "Successfully created sample court"),
    })
    public ResponseEntity<CourtDetails> createSampleCourt(
        @RequestParam(required = true) String courtName,
        @RequestParam(required = false) Long seed,
        @RequestParam(required = false, defaultValue = "false") boolean serviceCenter) {
        String courtSlug = testingSupportService.createCourt(courtName, seed, serviceCenter);
        CourtDetails details = courtService.getCourtDetailsBySlug(courtSlug);
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(details);
    }
}
