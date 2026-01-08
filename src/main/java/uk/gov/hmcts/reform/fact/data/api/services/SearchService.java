package uk.gov.hmcts.reform.fact.data.api.services;

import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.fact.data.api.repositories.ServiceRepository;

import java.util.List;

@Service
public class SearchService {

    private final ServiceRepository serviceRepository;

    public SearchService(ServiceRepository serviceRepository) {
        this.serviceRepository = serviceRepository;
    }

    /**
     * Return all services.
     *
     * @return A list of services.
     */
    public List<uk.gov.hmcts.reform.fact.data.api.entities.Service> getAllServices() {
        return serviceRepository.findAll();
    }
}
