package uk.gov.hmcts.reform.fact.data.api.services;

import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.fact.data.api.dto.CourtWithDistance;
import uk.gov.hmcts.reform.fact.data.api.repositories.CourtSinglePointsOfEntryRepository;

import java.util.List;

@Service
public class CourtSinglePointOfEntryService {

    private final CourtSinglePointsOfEntryRepository courtSinglePointsOfEntryRepository;

    public CourtSinglePointOfEntryService(CourtSinglePointsOfEntryRepository courtSinglePointsOfEntryRepository) {
        this.courtSinglePointsOfEntryRepository = courtSinglePointsOfEntryRepository;
    }

    public List<CourtWithDistance> getChildcareCourtsSpoe(double latitude, double longitude) {
        return courtSinglePointsOfEntryRepository.findNearestCourtBySpoeAndChildrenAreaOfLaw(latitude, longitude);
    }
}
