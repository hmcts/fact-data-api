package uk.gov.hmcts.reform.fact.data.api.services;

import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.fact.data.api.entities.ServiceArea;
import uk.gov.hmcts.reform.fact.data.api.errorhandling.exceptions.NotFoundException;
import uk.gov.hmcts.reform.fact.data.api.repositories.ServiceAreaRepository;

import java.text.MessageFormat;
import java.util.List;

@Service
public class ServiceAreaService {

    private final ServiceAreaRepository serviceAreaRepository;

    public ServiceAreaService(ServiceAreaRepository serviceAreaRepository) {
        this.serviceAreaRepository = serviceAreaRepository;
    }

    /**
     * Retrieves a service area by name.
     *
     * @param serviceArea the service area name
     * @return the matching service area
     */
    public ServiceArea getServiceAreaByName(String serviceArea) {
        return serviceAreaRepository.findByNameIgnoreCase(serviceArea.trim())
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
            serviceAreaRepository.findAllByServiceName(serviceName.trim());

        if (areas.isEmpty()) {
            throw new NotFoundException("No service areas found for service " + serviceName);
        }
        return areas;
    }
}
