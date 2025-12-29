package uk.gov.hmcts.reform.fact.data.api.controllers;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import uk.gov.hmcts.reform.fact.data.api.entities.types.SearchAction;
import uk.gov.hmcts.reform.fact.data.api.validation.annotations.ValidPostcode;

@Tag(name = "Court Search", description = "Operations related to the searching of courts")
@RestController
@Validated
@RequestMapping("/search/courts")
public class SearchCourtController {

    @GetMapping("/postcode")
    @Operation(
        summary = "Search courts by postcode based on various business rules.",
        description = "Retrieve courts based on postcode, service area and action."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Successfully retrieved court(s) based on provided postcode."),
        @ApiResponse(responseCode = "400", description = "Postcode is missing or is not valid."),
        @ApiResponse(responseCode = "404", description = "Information not found. "
            + "For example, service area provided but not valid.")
    })
    public ResponseEntity<String> getCourtsByPostcode(
        @Parameter(description = "Postcode")
        @ValidPostcode
        @NotBlank
        @RequestParam(value = "postcode")
        String postcode,

        @Parameter(description = "Service area slug")
        @RequestParam(value = "serviceArea", required = false)
        String serviceArea,

        @Parameter(description = "Action to perform")
        @RequestParam(value = "action", required = false)
        SearchAction action,

        @Parameter(description = "Maximum number of results (default 10)")
        @RequestParam(value = "limit", required = false, defaultValue = "10")
        @Min(1)
        @Max(50)
        Integer limit) {

        if (action != null && (serviceArea == null || serviceArea.isBlank())) {
            return ResponseEntity.badRequest()
                .body("Action provided but serviceArea is missing");
        }

        // * Required: postcode
        // * Optional: serviceArea
        // * Optional: action ∈ {NEAREST, DOCUMENTS, UPDATE}
        // * Optional: limit (default 10, max MAX)

        System.out.println(postcode);
        System.out.println(serviceArea);
        System.out.println(action);
        System.out.println(limit);

        return ResponseEntity.ok("Testy test");
    }
}
