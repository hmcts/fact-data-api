package uk.gov.hmcts.reform.fact.data.api.controllers;

import uk.gov.hmcts.reform.fact.data.api.entities.AreaOfLawType;
import uk.gov.hmcts.reform.fact.data.api.entities.ContactDescriptionType;
import uk.gov.hmcts.reform.fact.data.api.entities.CourtType;
import uk.gov.hmcts.reform.fact.data.api.entities.OpeningHourType;
import uk.gov.hmcts.reform.fact.data.api.entities.Region;
import uk.gov.hmcts.reform.fact.data.api.entities.ServiceArea;
import uk.gov.hmcts.reform.fact.data.api.security.SecuredFactRestController;
import uk.gov.hmcts.reform.fact.data.api.services.TypesService;

import java.util.List;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;

@SecuredFactRestController(
    path = "/types",
    name = "Types",
    description = "Operations related to types required for the functionality of both the admin portal and frontend"
)
public class TypesController {

    private final TypesService typesService;

    public TypesController(TypesService typesService) {
        this.typesService = typesService;
    }

    @GetMapping("/v1/areas-of-law")
    @Operation(
        summary = "Get all areas of law types",
        description = "Fetch all areas of law types."
            + "Returns empty list if no areas of law types exist."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Successfully retrieved areas of law types"),
    })
    public ResponseEntity<List<AreaOfLawType>> getAreasOfLawTypes() {
        return ResponseEntity.ok(typesService.getAreaOfLawTypes());
    }

    @GetMapping("/v1/court-types")
    @Operation(
        summary = "Get all court types",
        description = "Fetch all court types."
            + "Returns empty list if no court types exist."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Successfully retrieved court types"),
    })
    public ResponseEntity<List<CourtType>> getCourtTypes() {
        return ResponseEntity.ok(typesService.getCourtTypes());
    }

    @GetMapping("/v1/opening-hours-types")
    @Operation(
        summary = "Get all opening hours types",
        description = "Fetch all opening hours types."
            + "Returns empty list if no opening hours types exist."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Successfully retrieved opening hours types"),
    })
    public ResponseEntity<List<OpeningHourType>> getOpeningHoursTypes() {
        return ResponseEntity.ok(typesService.getOpeningHoursTypes());
    }

    @GetMapping("/v1/contact-description-types")
    @Operation(
        summary = "Get all contact description types",
        description = "Fetch all contact description types."
            + "Returns empty list if no contact description types exist."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Successfully retrieved contact description types"),
    })
    public ResponseEntity<List<ContactDescriptionType>> getContactDescriptionTypes() {
        return ResponseEntity.ok(typesService.getContactDescriptionTypes());
    }

    @GetMapping("/v1/regions")
    @Operation(
        summary = "Get all regions",
        description = "Fetch all regions."
            + "Returns empty list if no regions exist."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Successfully retrieved regions"),
    })
    public ResponseEntity<List<Region>> getRegions() {
        return ResponseEntity.ok(typesService.getRegions());
    }

    @GetMapping("/v1/service-areas")
    @Operation(
        summary = "Get all service areas",
        description = "Fetch all service areas."
            + "Returns empty list if no service areas exist."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Successfully retrieved service areas"),
    })
    public ResponseEntity<List<ServiceArea>> getServiceAreas() {
        return ResponseEntity.ok(typesService.getServiceAreas());
    }
}
