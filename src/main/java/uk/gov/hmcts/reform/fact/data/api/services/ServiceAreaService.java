package uk.gov.hmcts.reform.fact.data.api.services;

import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.fact.data.api.entities.ServiceArea;
import uk.gov.hmcts.reform.fact.data.api.errorhandling.exceptions.NotFoundException;
import uk.gov.hmcts.reform.fact.data.api.repositories.ServiceAreaRepository;

import java.text.MessageFormat;

@Service
public class ServiceAreaService {

    private final ServiceAreaRepository serviceAreaRepository;

    public ServiceAreaService(ServiceAreaRepository serviceAreaRepository) {
        this.serviceAreaRepository = serviceAreaRepository;
    }

    public ServiceArea getServiceAreaByName(String serviceArea) {
        return serviceAreaRepository.findByNameIgnoreCase(serviceArea.trim())
            .orElseThrow(() -> new NotFoundException(
                MessageFormat.format(
                    "Service area {0} not found",
                    serviceArea
                )));
    }
}
