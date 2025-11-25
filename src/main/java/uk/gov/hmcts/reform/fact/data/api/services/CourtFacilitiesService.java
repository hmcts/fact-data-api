package uk.gov.hmcts.reform.fact.data.api.services;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.fact.data.api.entities.Court;
import uk.gov.hmcts.reform.fact.data.api.entities.CourtFacilities;
import uk.gov.hmcts.reform.fact.data.api.errorhandling.exceptions.CourtResourceNotFoundException;
import uk.gov.hmcts.reform.fact.data.api.repositories.CourtFacilitiesRepository;

import java.util.UUID;

@Service
@Slf4j
public class CourtFacilitiesService {

    private final CourtFacilitiesRepository courtFacilitiesRepository;
    private final CourtService courtService;

    public CourtFacilitiesService(CourtFacilitiesRepository courtFacilitiesRepository, CourtService courtService) {
        this.courtFacilitiesRepository = courtFacilitiesRepository;
        this.courtService = courtService;
    }

    /**
     * Get facilities by court id. A court will only ever have zero or one facilities record.
     *
     * @param courtId The court id to find the facilities for.
     * @return The building facilities record or null if one doesn't exist for the court.
     * @throws CourtResourceNotFoundException  if no facilities record exists for the court.
     */
    public CourtFacilities getFacilitiesByCourtId(UUID courtId) {
        return courtFacilitiesRepository.findByCourtId(courtService.getCourtById(courtId).getId())
                .orElseThrow(() -> new
                    CourtResourceNotFoundException("No facilities found for court id: " + courtId)
                );
    }

    /**
     * Set a facilities record for a court.
     *
     * @param courtId The id of the court to set facilities for.
     * @param courtFacilities The court facilities entity to create.
     * @return The created facilities entity.
     */
    public CourtFacilities setFacilities(UUID courtId, CourtFacilities courtFacilities) {
        log.info("Setting facilities for court id: {}", courtId);
        Court foundCourt = courtService.getCourtById(courtId);
        courtFacilities.setCourt(foundCourt);
        courtFacilities.setCourtId(courtId);
        courtFacilitiesRepository.findByCourtId(courtId).ifPresent(
                existing -> courtFacilities.setId(existing.getId())
        );

        return courtFacilitiesRepository.save(courtFacilities);
    }
}
