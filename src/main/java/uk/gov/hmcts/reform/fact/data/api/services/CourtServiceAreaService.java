package uk.gov.hmcts.reform.fact.data.api.services;

import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.fact.data.api.entities.CourtServiceAreas;
import uk.gov.hmcts.reform.fact.data.api.repositories.CourtServiceAreasRepository;

import java.util.List;
import java.util.UUID;

@Service
public class CourtServiceAreaService {

    private final CourtServiceAreasRepository courtServiceAreasRepository;
    private final ServiceAreaService serviceAreaService;

    public CourtServiceAreaService(CourtServiceAreasRepository courtServiceAreasRepository,
                                   ServiceAreaService serviceAreaService) {
        this.courtServiceAreasRepository = courtServiceAreasRepository;
        this.serviceAreaService = serviceAreaService;
    }

    public List<CourtServiceAreas> findByServiceAreaId(UUID id) {
        return courtServiceAreasRepository.findByServiceAreaId(id);
    }

    /**
     * Return the court service area details which includes the court_id on the frontend.
     * We can then call a service centre endpoint from the frontend if the
     * catchment type is NATIONAL.
     *
     * @param serviceAreaName The name of the service area.
     * @return A list of courts that link to the service area.
     */
    public List<CourtServiceAreas> findByServiceAreaName(String serviceAreaName) {
        return courtServiceAreasRepository.findByServiceAreaId(
            serviceAreaService.getServiceAreaByName(serviceAreaName).getId());
    }
}
