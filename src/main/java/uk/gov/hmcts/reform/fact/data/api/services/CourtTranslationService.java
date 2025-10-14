package uk.gov.hmcts.reform.fact.data.api.services;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.fact.data.api.entities.CourtTranslation;
import uk.gov.hmcts.reform.fact.data.api.errorhandling.exceptions.TranslationNotFoundException;
import uk.gov.hmcts.reform.fact.data.api.repositories.CourtTranslationRepository;

import java.util.UUID;

@Service
@Slf4j
public class CourtTranslationService {

    private final CourtTranslationRepository courtTranslationRepository;
    private final CourtService courtService;

    public CourtTranslationService(CourtTranslationRepository courtTranslationRepository, CourtService courtService) {
        this.courtTranslationRepository = courtTranslationRepository;
        this.courtService = courtService;
    }

    /**
     * Get translation by court id. A court will only ever have zero or one translation records.
     *
     * @param courtId The court id to find the translation for.
     * @return A translation  record or null if one doesn't exist for the court.
     * @throws TranslationNotFoundException if no translation record exists for the court.
     */
    public CourtTranslation getTranslationByCourtId(UUID courtId) {
        return courtTranslationRepository.findByCourtId(courtService.getCourtById(courtId).getId())
                .orElseThrow(() -> new
                        TranslationNotFoundException("No translation found for court id: " + courtId)
                );
    }

    /**
     * Set a translation record for a court.
     *
     * @param courtId The id of the court to set a translation for.
     * @param courtTranslation The court translation entity to create.
     * @return The created translation entity.
     */
    public CourtTranslation setTranslation(UUID courtId, CourtTranslation courtTranslation) {
        log.info("Setting translation for court id: {}", courtId);
        courtService.getCourtById(courtId);
        courtTranslation.setCourtId(courtId);
        courtTranslationRepository.findByCourtId(courtId).ifPresent(
                existing -> courtTranslation.setId(existing.getId())
        );

        return courtTranslationRepository.save(courtTranslation);
    }
}
