package uk.gov.hmcts.reform.fact.data.api.controllers;

import com.fasterxml.jackson.annotation.JsonView;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import uk.gov.hmcts.reform.fact.data.api.entities.ServiceCentre;
import uk.gov.hmcts.reform.fact.data.api.entities.ServiceCentreDetails;
import uk.gov.hmcts.reform.fact.data.api.security.SecuredFactRestController;
import uk.gov.hmcts.reform.fact.data.api.services.ServiceCentreDetailsViewService;
import uk.gov.hmcts.reform.fact.data.api.services.ServiceCentreService;
import uk.gov.hmcts.reform.fact.data.api.validation.annotations.ValidUUID;

import java.util.UUID;

@SecuredFactRestController(
    name = "Service Centre",
    description = "Operations related to service centres"
)
@RequestMapping("/service-centres")
@RequiredArgsConstructor
@SuppressWarnings("java:S4684")
public class ServiceCentreController {

    /**
     * JsonView marker for expanded service centre details responses.
     */
    public interface ServiceCentreDetailsView {
    }

    private final ServiceCentreService serviceCentreService;
    private final ServiceCentreDetailsViewService serviceCentreDetailsViewService;

    @GetMapping("/{serviceCentreId}/v1")
    @JsonView(ServiceCentreDetailsView.class)
    @Operation(
        summary = "Get service centre details by ID",
        description = "Fetch detailed service centre information for a given service centre ID."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Successfully retrieved service centre details"),
        @ApiResponse(responseCode = "400", description = "Invalid service centre ID supplied"),
        @ApiResponse(responseCode = "404", description = "Service centre not found")
    })
    public ResponseEntity<ServiceCentreDetails> getServiceCentreDetailsById(
        @Parameter(description = "UUID of the service centre", required = true)
        @ValidUUID @PathVariable String serviceCentreId) {
        return ResponseEntity.ok(
            serviceCentreDetailsViewService.prepareDetailsView(
                serviceCentreService.getServiceCentreDetailsById(UUID.fromString(serviceCentreId))
            )
        );
    }

    @GetMapping("/{serviceCentreId}/entity/v1")
    @Operation(
        summary = "Get service centre entity by ID",
        description = "Fetch the service centre entity for a given service centre ID."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Successfully retrieved service centre entity"),
        @ApiResponse(responseCode = "400", description = "Invalid service centre ID supplied"),
        @ApiResponse(responseCode = "404", description = "Service centre not found")
    })
    public ResponseEntity<ServiceCentre> getServiceCentreById(
        @Parameter(description = "UUID of the service centre", required = true)
        @ValidUUID @PathVariable String serviceCentreId) {
        return ResponseEntity.ok(serviceCentreService.getServiceCentreById(UUID.fromString(serviceCentreId)));
    }

    @GetMapping("/name/v1")
    @Operation(
        summary = "Get service centre by exact name",
        description = "Fetch the service centre for a given exact service centre name."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Successfully retrieved service centre"),
        @ApiResponse(responseCode = "400", description = "Invalid service centre name supplied"),
        @ApiResponse(responseCode = "404", description = "Service centre not found")
    })
    public ResponseEntity<ServiceCentre> getServiceCentreByName(
        @Parameter(description = "Exact name of the service centre", required = true)
        @NotBlank(message = "name must not be blank")
        @Size(max = 250, message = "name must be less than 250 characters")
        @RequestParam(name = "name") String name) {
        return ResponseEntity.ok(serviceCentreService.getServiceCentreByName(name));
    }

    @PostMapping(value = "/v1",
        consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(
        summary = "Create a new service centre",
        description = "Creates a new service centre record."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "201", description = "Successfully created service centre"),
        @ApiResponse(responseCode = "400", description = "Invalid service centre data supplied")
    })
    @PreAuthorize("@authService.isAdmin()")
    public ResponseEntity<ServiceCentre> createServiceCentre(@Valid @RequestBody ServiceCentre serviceCentre) {
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(serviceCentreService.createServiceCentre(serviceCentre));
    }

    @PutMapping(value = "/{serviceCentreId}/v1",
        consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(
        summary = "Update an existing service centre",
        description = "Updates the details of an existing service centre."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Successfully updated service centre"),
        @ApiResponse(responseCode = "400", description = "Invalid service centre data supplied"),
        @ApiResponse(responseCode = "404", description = "Service centre not found")
    })
    @PreAuthorize("@authService.isAdmin()")
    public ResponseEntity<ServiceCentre> updateServiceCentre(
        @ValidUUID @PathVariable String serviceCentreId,
        @Valid @RequestBody ServiceCentre serviceCentre) {
        return ResponseEntity.ok(
            serviceCentreService.updateServiceCentre(UUID.fromString(serviceCentreId), serviceCentre)
        );
    }
}
