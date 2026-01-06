package uk.gov.hmcts.reform.fact.data.api.services;

import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.fact.data.api.entities.CourtServiceAreas;
import uk.gov.hmcts.reform.fact.data.api.repositories.CourtServiceAreasRepository;

import java.util.List;
import java.util.UUID;

@Service
public class CourtServiceAreaService {

    private final CourtServiceAreasRepository courtServiceAreasRepository;

    public CourtServiceAreaService(CourtServiceAreasRepository courtServiceAreasRepository) {
        this.courtServiceAreasRepository = courtServiceAreasRepository;
    }

    public List<CourtServiceAreas> findByServiceAreaId(UUID id) {
        return courtServiceAreasRepository.findByServiceAreaId(id);
    }
}
