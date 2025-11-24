package uk.gov.hmcts.reform.fact.data.api.services;

import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.fact.data.api.entities.CourtAccessibilityOptions;
import uk.gov.hmcts.reform.fact.data.api.errorhandling.exceptions.CourtResourceNotFoundException;
import uk.gov.hmcts.reform.fact.data.api.errorhandling.exceptions.NotFoundException;
import uk.gov.hmcts.reform.fact.data.api.repositories.CourtAccessibilityOptionsRepository;

import java.util.UUID;

@Service
public class CourtAccessibilityOptionsService {

    private final CourtAccessibilityOptionsRepository courtAccessibilityOptionsRepository;
    private final CourtService courtService;

    public CourtAccessibilityOptionsService(
        CourtAccessibilityOptionsRepository courtAccessibilityOptionsRepository, CourtService courtService) {
        this.courtAccessibilityOptionsRepository = courtAccessibilityOptionsRepository;
        this.courtService = courtService;
    }

    /**
     * Get Accessibility Options by court id. A court will only ever have zero or one Accessibility Options records.
     *
     * @param courtId The court id to find the Accessibility Options for.
     * @return An Accessibility Options record or null if one doesn't exist for the court.
     * @throws NotFoundException if no Accessibility Options record exists for the court.
     */
    public CourtAccessibilityOptions getAccessibilityOptionsByCourtId(UUID courtId) {
        return courtAccessibilityOptionsRepository.findByCourtId(courtService.getCourtById(courtId).getId())
                .orElseThrow(() -> new
                    CourtResourceNotFoundException("No Accessibility Options found for court id: " + courtId)
                );
    }

    /**
     * Set an Accessibility Options record for a court.
     *
     * @param courtId The id of the court to set an Accessibility Options for.
     * @param courtAccessibilityOptions The court Accessibility Options entity to create.
     * @return The created Accessibility Options entity.
     */
    public CourtAccessibilityOptions setAccessibilityOptions(
        UUID courtId, CourtAccessibilityOptions courtAccessibilityOptions) {
        courtAccessibilityOptions.setCourt(courtService.getCourtById(courtId));
        courtAccessibilityOptions.setCourtId(courtId);

        courtAccessibilityOptionsRepository.findByCourtId(courtId).ifPresent(
                existing -> courtAccessibilityOptions.setId(existing.getId())
        );

        return courtAccessibilityOptionsRepository.save(courtAccessibilityOptions);
    }
}
