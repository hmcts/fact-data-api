package uk.gov.hmcts.reform.fact.data.api.services;

import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.fact.data.api.dto.CourtWithDistance;
import uk.gov.hmcts.reform.fact.data.api.repositories.CourtAddressRepository;

import java.util.List;

@Service
public class CourtAddressService {
    private final CourtAddressRepository courtAddressRepository;

    public CourtAddressService(CourtAddressRepository courtAddressRepository) {
        this.courtAddressRepository = courtAddressRepository;
    }

    /**
     * Find Court Distances through the OS Data provided.
     * Uses the Court Address table to do this, but places the relevant rows into
     * a CourtWithDistance List already for us to use.
     * @param lat the lat
     * @param lng the lng
     * @param limit the limit of rows returned
     * @return A list of CourtWithDistance objects
     */
    public List<CourtWithDistance> findCourtWithDistanceByOsData(double lat, double lng, Integer limit) {
        return courtAddressRepository.findNearestCourts(lat, lng, limit);
    }
}
