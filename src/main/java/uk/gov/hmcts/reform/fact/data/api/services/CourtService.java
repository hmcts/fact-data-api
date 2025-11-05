package uk.gov.hmcts.reform.fact.data.api.services;

import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.fact.data.api.entities.Court;
import uk.gov.hmcts.reform.fact.data.api.errorhandling.exceptions.NotFoundException;
import uk.gov.hmcts.reform.fact.data.api.repositories.CourtRepository;

import java.util.List;
import java.util.UUID;

@Service
public class CourtService {

    private final CourtRepository courtRepository;

    public CourtService(CourtRepository courtRepository) {
        this.courtRepository = courtRepository;
    }

    /**
     * Get a court by id.
     *
     * @param courtId The ID of the court to get.
     * @return The court entity.
     * @throws NotFoundException if the court is not found.
     */
    public Court getCourtById(UUID courtId) {
        return courtRepository.findById(courtId).orElseThrow(
            () -> new NotFoundException("Court not found, ID: " + courtId)
        );
    }

    /**
     * Get multiple courts by their IDs.
     *
     * @param courtIds List of court IDs to retrieve.
     * @return List of courts matching the provided IDs.
     * @throws NotFoundException if a court is not found.
     */
    public List<Court> getAllCourtsByIds(List<UUID> courtIds) {
        return courtRepository.findAllById(courtIds);
    }
}
