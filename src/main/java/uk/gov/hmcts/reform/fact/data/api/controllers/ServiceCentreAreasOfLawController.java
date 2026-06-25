package uk.gov.hmcts.reform.fact.data.api.controllers;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import uk.gov.hmcts.reform.fact.data.api.entities.AreaOfLawType;
import uk.gov.hmcts.reform.fact.data.api.entities.ServiceCentreAreasOfLaw;
import uk.gov.hmcts.reform.fact.data.api.security.SecuredFactRestController;
import uk.gov.hmcts.reform.fact.data.api.services.ServiceCentreAreasOfLawService;
import uk.gov.hmcts.reform.fact.data.api.validation.annotations.ValidUUID;

import java.util.Map;
import java.util.UUID;

@SecuredFactRestController(
    name = "Service Centre Areas Of Law",
    description = "Operations related to Service Centre Areas Of Law"
)
@RequestMapping("/service-centres/{serviceCentreId}")
@RequiredArgsConstructor
public class ServiceCentreAreasOfLawController {

    private final ServiceCentreAreasOfLawService serviceCentreAreasOfLawService;

    @GetMapping("/v1/areas-of-law")
    @Operation(
        summary = "Get Areas Of Law map with boolean by service centre ID",
        description = "Fetch Areas Of Law with availability boolean for a given service centre."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Successfully retrieved Areas Of Law"),
        @ApiResponse(responseCode = "400", description = "Invalid service centre ID supplied"),
        @ApiResponse(responseCode = "404", description = "Service centre not found")
    })
    public ResponseEntity<Map<AreaOfLawType, Boolean>> getAreasOfLawByServiceCentreId(
        @Parameter(description = "UUID of the service centre", required = true)
        @ValidUUID @PathVariable String serviceCentreId) {
        return ResponseEntity.ok(
            serviceCentreAreasOfLawService.getAreasOfLawStatusByServiceCentreId(UUID.fromString(serviceCentreId))
        );
    }

    @PutMapping("/v1/areas-of-law")
    @Operation(
        summary = "Create or update Service Centre Areas Of Law for a service centre",
        description = "Creates new Service Centre Areas Of Law for a service centre or updates the existing one."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "201", description = "Successfully created/updated Service Centre Areas Of Law"),
        @ApiResponse(responseCode = "400", description = "Invalid service centre ID supplied or invalid request body"),
        @ApiResponse(responseCode = "404", description = "Service centre not found")
    })
    @PreAuthorize("@authService.isAdmin()")
    public ResponseEntity<ServiceCentreAreasOfLaw> setAreasOfLawServices(
        @Parameter(description = "UUID of the service centre", required = true)
        @ValidUUID @PathVariable String serviceCentreId,
        @Parameter(description = "AreasOfLaw object to create or update", required = true)
        @Valid @RequestBody ServiceCentreAreasOfLaw serviceCentreAreasOfLaw) {
        return ResponseEntity
            .status(HttpStatus.CREATED)
            .body(serviceCentreAreasOfLawService.setServiceCentreAreasOfLaw(
                UUID.fromString(serviceCentreId),
                serviceCentreAreasOfLaw
            ));
    }
}
