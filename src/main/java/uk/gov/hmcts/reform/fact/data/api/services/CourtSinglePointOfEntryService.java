package uk.gov.hmcts.reform.fact.data.api.services;

import org.springframework.stereotype.Service;

import uk.gov.hmcts.reform.fact.data.api.dto.CourtWithDistance;
import uk.gov.hmcts.reform.fact.data.api.repositories.CourtSinglePointsOfEntryRepository;

import java.util.List;
import java.util.UUID;

@Service
public class CourtSinglePointOfEntryService {

    private final CourtSinglePointsOfEntryRepository courtSinglePointsOfEntryRepository;

    public CourtSinglePointOfEntryService(CourtSinglePointsOfEntryRepository courtSinglePointsOfEntryRepository) {
        this.courtSinglePointsOfEntryRepository = courtSinglePointsOfEntryRepository;
    }

    /**
     * Finds the nearest SPOE court that lies within the given local authority for childcare arrangements.
     *
     * @param latitude  the latitude to search from
     * @param longitude the longitude to search from
     * @param areaOfLaw the area of law to filter by
     * @param localAuthorityId the local authority ID to filter by
     * @return matching courts with distance data
     */
    public List<CourtWithDistance> getCourtsSpoe(double latitude, double longitude, String areaOfLaw,
                                                 UUID localAuthorityId) {
        return courtSinglePointsOfEntryRepository.findNearestCourtBySpoeAndChildrenAreaOfLawAndLocalAuthorityId(
            latitude,
            longitude,
            areaOfLaw,
            localAuthorityId
        );
    }

    /**
     * Finds the nearest SPOE court for childcare arrangements.
     *
     * @param latitude the latitude to search from
     * @param longitude the longitude to search from
     * @param areaOfLaw the area of law to filter by
     * @return matching courts with distance data
     */
    public List<CourtWithDistance> getCourtsSpoe(double latitude, double longitude, String areaOfLaw) {
        return courtSinglePointsOfEntryRepository.findNearestCourtBySpoeAndChildrenAreaOfLaw(
            latitude,
            longitude,
            areaOfLaw
        );
    }
}
