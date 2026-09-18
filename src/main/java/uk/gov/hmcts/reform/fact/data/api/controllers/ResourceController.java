package uk.gov.hmcts.reform.fact.data.api.controllers;

import uk.gov.hmcts.reform.fact.data.api.security.SecuredFactRestController;
import uk.gov.hmcts.reform.fact.data.api.services.CourtPhotoService;
import uk.gov.hmcts.reform.fact.data.api.services.CsvService;
import uk.gov.hmcts.reform.fact.data.api.validation.annotations.ValidUUID;

import java.util.UUID;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

@SecuredFactRestController(
    name = "Resource",
    description = "Operations related to resources")
@RequestMapping("/resources")
@RequiredArgsConstructor
public class ResourceController {

    private final CourtPhotoService courtPhotoService;
    private final CsvService csvService;

    @GetMapping(value = "/v1/court-photo/{courtId}", produces = {"img/jpg", "img/jpeg", "img/png"})
    @Operation(
        summary = "Download a stored photo for a court",
        description = "Downloads the stored photo for a court."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Photo successfully downloaded"),
        @ApiResponse(responseCode = "404", description = "Failed to locate a Photo")
    })
    public ResponseEntity<StreamingResponseBody> downloadPhoto(
        @Parameter(description = "UUID of the court", required = true) @ValidUUID @PathVariable String courtId) {
        CourtPhotoService.PhotoStreamDetails details = courtPhotoService.getPhotoStream(UUID.fromString(courtId));
        return ResponseEntity.ok()
            .contentType(MediaType.parseMediaType(details.contentType()))
            .body(details.body());
    }

    @GetMapping(value = "/v1/csv", produces = "text/csv")
    @Operation(
        summary = "Download the current CSV file",
        description = "Downloads the current CSV file from from the configured storage service."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "CSV file successfully downloaded"),
        @ApiResponse(responseCode = "404", description = "Failed to locate a CSV file")
    })
    public ResponseEntity<StreamingResponseBody> downloadCsv() {
        return ResponseEntity.ok()
            .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"courts-and-tribunals-data.csv\"")
            .contentType(MediaType.parseMediaType("text/csv"))
            .body(csvService.getCsvStreamInputStream());
    }
}
