package uk.gov.hmcts.reform.fact.data.api.services;

import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.fact.data.api.entities.Court;
import uk.gov.hmcts.reform.fact.data.api.entities.Translation;
import uk.gov.hmcts.reform.fact.data.api.errorhandling.exceptions.NotFoundException;
import uk.gov.hmcts.reform.fact.data.api.errorhandling.exceptions.TranslationNotFoundException;
import uk.gov.hmcts.reform.fact.data.api.repositories.TranslationRepository;

import java.util.UUID;

@Service
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
     * @throws NotFoundException if the court id is not found.
     */
    public Translation getTranslationByCourtId(UUID courtId) {
        return translationRepository.findByCourt_Id(courtService.getCourtById(courtId).getId())
            .orElseThrow(() -> new
                TranslationNotFoundException("No translation services found for court id: " + courtId)
            );
    }

    /**
     * Create a translation service record for a court.
     *
     * @param translation The translation entity to create.
     * @return The created translation entity.
     */
    public Translation createTranslation(UUID courtId, Translation translation) {
        Court court = courtService.getCourtById(courtId);
        translation.setCourt(court);
        return translationRepository.save(translation);
    }
}
