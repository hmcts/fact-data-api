package uk.gov.hmcts.reform.fact.data.api.controllers;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import uk.gov.hmcts.reform.fact.data.api.entities.CourtPhoto;
import uk.gov.hmcts.reform.fact.data.api.services.CourtPhotoService;
import uk.gov.hmcts.reform.fact.data.api.validation.annotations.ValidImage;
import uk.gov.hmcts.reform.fact.data.api.validation.annotations.ValidUUID;

import java.util.UUID;

@Tag(name = "Court Photo", description = "Operations related to photos for courts")
@RestController
@Validated
@RequestMapping("/courts/{courtId}")
public class CourtPhotoController {

    private final CourtPhotoService courtPhotoService;

    public CourtPhotoController(CourtPhotoService courtPhotoService) {
        this.courtPhotoService = courtPhotoService;
    }

    @GetMapping("/v1/photo")
    @Operation(
        summary = "Get court photo by court ID",
        description = "Fetch photo information for a given court."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Successfully retrieved court photo"),
        @ApiResponse(responseCode = "400", description = "Invalid court ID supplied"),
        @ApiResponse(responseCode = "404", description = "Court or court photo not found")
    })
    public ResponseEntity<CourtPhoto> getCourtPhotoByCourtId(
        @Parameter(description = "UUID of the court", required = true) @ValidUUID @PathVariable String courtId) {
        return ResponseEntity.ok(courtPhotoService.getCourtPhotoByCourtId(UUID.fromString(courtId)));
    }

    @PostMapping("/v1/photo")
    @Operation(
        summary = "Creates the photo for a court (and replaces any existing photo)",
        description = "Creates photo for a court."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "201", description = "Successfully created court photo"),
        @ApiResponse(responseCode = "400", description = "Invalid court ID supplied or invalid file"),
        @ApiResponse(responseCode = "404", description = "Court not found")
    })
    public ResponseEntity<CourtPhoto> setCourtPhotoByCourtId(
        @Parameter(description = "UUID of the court", required = true)
        @ValidUUID @PathVariable String courtId, @ValidImage @RequestPart("file") MultipartFile file) {
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(courtPhotoService.setCourtPhoto(UUID.fromString(courtId), file));
    }

    @DeleteMapping("/v1/photo")
    @Operation(
        summary = "Delete court photo by court ID",
        description = "Deletes the court photo associated with the given court ID."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "204", description = "Successfully deleted court photo"),
        @ApiResponse(responseCode = "400", description = "Invalid court ID supplied"),
        @ApiResponse(responseCode = "404", description = "Court or court photo not found")
    })
    public ResponseEntity<Void> deleteCourtPhotoByCourtId(
        @Parameter(description = "UUID of the court", required = true) @ValidUUID @PathVariable String courtId) {
        courtPhotoService.deleteCourtPhotoByCourtId(UUID.fromString(courtId));
        return ResponseEntity.noContent().build();
    }
}
