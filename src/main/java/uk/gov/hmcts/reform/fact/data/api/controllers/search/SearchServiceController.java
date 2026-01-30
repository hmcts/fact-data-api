package uk.gov.hmcts.reform.fact.data.api.controllers.search;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import uk.gov.hmcts.reform.fact.data.api.entities.Service;
import uk.gov.hmcts.reform.fact.data.api.entities.ServiceArea;
import uk.gov.hmcts.reform.fact.data.api.repositories.ServiceRepository;
import uk.gov.hmcts.reform.fact.data.api.services.ServiceAreaService;

import java.util.List;

@Tag(name = "Search Service", description = "Operations related to the searching of services")
@RestController
@Validated
@RequestMapping("/search/services")
public class SearchServiceController {

    private final ServiceRepository serviceRepository;
    private final ServiceAreaService serviceAreaService;

    public SearchServiceController(ServiceRepository serviceRepository,
                                   ServiceAreaService serviceAreaService) {
        this.serviceRepository = serviceRepository;
        this.serviceAreaService = serviceAreaService;
    }

    @GetMapping("/v1")
    @Operation(
        summary = "Search for and return all services.",
        description = "Retrieve all services."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200",
            description = "Successfully retrieved service areas based on provided name.")
    })
    public ResponseEntity<List<Service>> getServices() {
        return ResponseEntity.ok(serviceRepository.findAll());
    }

    @GetMapping("/v1/{serviceName}/service-areas")
    @Operation(
        summary = "Search for service areas by service name.",
        description = "Retrieve Service Areas based on provided name."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200",
            description = "Successfully retrieved service areas based on provided name."),
        @ApiResponse(responseCode = "400", description = "Service name is missing or is not valid."),
        @ApiResponse(responseCode = "404", description = "Information not found for provided service name.")
    })
    public ResponseEntity<List<ServiceArea>> getServiceAreaByName(
        @Parameter(description = "Service name", required = true)
        @PathVariable String serviceName
    ) {
        return ResponseEntity.ok(serviceAreaService.getAllServiceAreasForService(serviceName));
    }
}
