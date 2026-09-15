package uk.gov.hmcts.reform.fact.data.api.controllers;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import uk.gov.hmcts.reform.fact.data.api.security.SecuredFactRestController;
import uk.gov.hmcts.reform.fact.data.api.services.CsvService;

import java.io.InputStream;

@SecuredFactRestController(
    name = "CSV",
    description = "Operations related to CSV file handling",
    preAuthorize = "@authService.isAdmin()"
)
@RequestMapping("/csv")
@RequiredArgsConstructor
public class CsvController {

    private final CsvService csvService;

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

    @GetMapping(value = "/", produces = "text/csv")
    @Operation(
        summary = "Download the current CSV file",
        description = "Downloads the current CSV file from from the configured storage service."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "CSV file successfully downloaded"),
        @ApiResponse(responseCode = "404", description = "Failed to locate a CSV file")
    })
    @PreAuthorize("@authService.canView()")
    public ResponseEntity<StreamingResponseBody> downloadCsv() {
        InputStream csvInputStream = csvService.getCsvStreamInputStream();
        StreamingResponseBody body = outputStream -> {
            try (InputStream in = csvInputStream) {
                in.transferTo(outputStream);
            }
        };
        return ResponseEntity.ok()
            .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"courts-and-tribunals-data.csv\"")
            .contentType(MediaType.parseMediaType("text/csv"))
            .body(body);
    }
}
