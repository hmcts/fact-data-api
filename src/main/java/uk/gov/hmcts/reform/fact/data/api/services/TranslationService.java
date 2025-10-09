package uk.gov.hmcts.reform.fact.data.api.services;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.fact.data.api.entities.Translation;
import uk.gov.hmcts.reform.fact.data.api.errorhandling.exceptions.TranslationNotFoundException;
import uk.gov.hmcts.reform.fact.data.api.repositories.TranslationRepository;

import java.util.UUID;

@Service
@Slf4j
public class TranslationService {

    private final TranslationRepository translationRepository;
    private final CourtService courtService;

    public TranslationService(TranslationRepository translationRepository, CourtService courtService) {
        this.translationRepository = translationRepository;
        this.courtService = courtService;
    }

    /**
     * Get translation by court id. A court will only ever have zero or one translation service records.
     *
     * @param courtId The court id to find the translation for.
     * @return A translation service record or null if one doesn't exist for the court.
     * @throws TranslationNotFoundException if no translation service record exists for the court.
     */
    public Translation getTranslationByCourtId(UUID courtId) {
        return translationRepository.findByCourtId(courtService.getCourtById(courtId).getId())
            .orElseThrow(() -> new
                TranslationNotFoundException("No translation services found for court id: " + courtId)
            );
    }

    /**
     * Set a translation service record for a court.
     *
     * @param translation The translation entity to create.
     * @return The created translation entity.
     */
    public Translation setTranslation(UUID courtId, Translation translation) {
        log.info("Setting translation service for court id: {}", courtId);
        courtService.getCourtById(courtId);
        translation.setCourtId(courtId);
        translationRepository.findByCourtId(courtId).ifPresent(
                existing -> translation.setId(existing.getId())
        );

        return translationRepository.save(translation);
    }
}
