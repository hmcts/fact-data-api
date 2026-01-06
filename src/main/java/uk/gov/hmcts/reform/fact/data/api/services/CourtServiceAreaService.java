package uk.gov.hmcts.reform.fact.data.api.services;

import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.fact.data.api.entities.CourtServiceAreas;
import uk.gov.hmcts.reform.fact.data.api.entities.ServiceArea;
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

    public List<CourtServiceAreas> findByServiceAreaName(String serviceAreaName) {
        return courtServiceAreasRepository.findByServiceAreaId(
            serviceAreaService.getServiceAreaByName(serviceAreaName).getId());
    }
}
