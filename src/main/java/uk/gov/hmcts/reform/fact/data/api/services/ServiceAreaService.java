package uk.gov.hmcts.reform.fact.data.api.services;

import uk.gov.hmcts.reform.fact.data.api.entities.ServiceArea;
import uk.gov.hmcts.reform.fact.data.api.entities.types.CatchmentType;
import uk.gov.hmcts.reform.fact.data.api.errorhandling.exceptions.NotFoundException;
import uk.gov.hmcts.reform.fact.data.api.repositories.CourtServiceAreasRepository;
import uk.gov.hmcts.reform.fact.data.api.repositories.ServiceAreaRepository;

import java.text.MessageFormat;
import java.util.List;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ServiceAreaService {

    private final ServiceAreaRepository serviceAreaRepository;
    private final CourtServiceAreasRepository courtServiceAreasRepository;

    /**
     * Retrieves a service area by name.
     *
     * @param serviceArea the service area name
     * @return the matching service area
     */
    public ServiceArea getServiceAreaByName(String serviceArea) {
        return serviceAreaRepository.findByNameIgnoreCase(serviceArea.trim())
            .map(this::enrichServiceArea)
            .orElseThrow(() -> new NotFoundException(
                MessageFormat.format(
                    "Service area {0} not found", serviceArea
                )));
    }

    /**
     * Retrieves all service areas for a service name.
     *
     * @param serviceName the service name
     * @return the matching service areas
     */
    public List<ServiceArea> getAllServiceAreasForService(String serviceName) {
        List<ServiceArea> areas =
            serviceAreaRepository.findAllByServiceName(serviceName.trim()).stream()
                .map(this::enrichServiceArea)
                .toList();

        if (areas.isEmpty()) {
            throw new NotFoundException("No service areas found for service " + serviceName);
        }
        return areas;
    }

    /**
     * Add some useful data to the DTO so that the frontend can make informed decisions.
     *
     * @param serviceArea the service area
     * @return the service area with additional data
     */
    private ServiceArea enrichServiceArea(ServiceArea serviceArea) {
        serviceArea.setHasLocal(
            courtServiceAreasRepository.existsByServiceAreaIdAndCatchmentTypeIn(
                serviceArea.getId(), List.of(CatchmentType.LOCAL))
        );
        serviceArea.setHasNational(
            courtServiceAreasRepository.existsByServiceAreaIdAndCatchmentTypeIn(
                serviceArea.getId(), List.of(CatchmentType.NATIONAL))
        );
        serviceArea.setHasRegional(
            courtServiceAreasRepository.existsByServiceAreaIdAndCatchmentTypeIn(
                serviceArea.getId(), List.of(CatchmentType.REGIONAL))
        );
        return serviceArea;
    }
}
