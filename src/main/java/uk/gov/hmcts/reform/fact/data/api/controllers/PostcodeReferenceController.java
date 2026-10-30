package uk.gov.hmcts.reform.fact.data.api.controllers;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import uk.gov.hmcts.reform.fact.data.api.models.PostcodeReferenceImportResult;
import uk.gov.hmcts.reform.fact.data.api.models.PostcodeReferenceSynchronisationResult;
import uk.gov.hmcts.reform.fact.data.api.security.SecuredFactRestController;
import uk.gov.hmcts.reform.fact.data.api.services.PostcodeReferenceImportService;
import uk.gov.hmcts.reform.fact.data.api.services.PostcodeReferenceSynchronisationService;

@SecuredFactRestController(
    name = "Postcode reference",
    description = "Operations related to ONSPD postcode reference data",
    preAuthorize = "@authService.isAdmin()"
)
@RequestMapping("/postcode-reference/v1")
@RequiredArgsConstructor
public class PostcodeReferenceController {

    private final PostcodeReferenceImportService importService;
    private final PostcodeReferenceSynchronisationService synchronisationService;

    @PostMapping("/import")
    @Operation(
        summary = "Import ONSPD postcode reference data",
        description = "Replaces postcode reference data from the configured ONSPD blob when its source version is new."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Import completed or the source version was already imported"),
        @ApiResponse(responseCode = "404", description = "The configured ONSPD blob was not found"),
        @ApiResponse(responseCode = "500", description = "The postcode reference import failed")
    })
    public PostcodeReferenceImportResult importPostcodeReference() {
        return importService.importLatest();
    }

    @PostMapping("/synchronise")
    @Operation(
        summary = "Synchronise ONSPD postcode reference data",
        description = "Downloads a new ONSPD release when available and imports the stored reference data."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Synchronisation completed"),
        @ApiResponse(responseCode = "500", description = "The postcode reference synchronisation failed")
    })
    public PostcodeReferenceSynchronisationResult synchronisePostcodeReference() {
        return synchronisationService.synchronise();
    }
}
