package uk.gov.hmcts.reform.fact.data.api.controllers;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import uk.gov.hmcts.reform.fact.data.api.security.SecuredFactRestController;
import uk.gov.hmcts.reform.fact.data.api.services.CsvService;

@SecuredFactRestController(
    name = "CSV",
    description = "Operations related to CSV file handling"
)
@RequestMapping("/csv")
public class CsvController {

    private final CsvService csvService;

    public CsvController(CsvService csvService) {
        this.csvService = csvService;
    }

    @PostMapping("/")
    @Operation(
        summary = "Create and upload CSV file",
        description = "Generates a CSV file with court data and uploads it to the configured storage service."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "CSV file created and uploaded successfully"),
        @ApiResponse(responseCode = "500", description = "Failed to create CSV file"),
        @ApiResponse(responseCode = "502", description = "Failed to upload CSV file to storage")
    })
    public void createAndUploadCsv() {
        csvService.createAndUploadCsv();
    }
}
