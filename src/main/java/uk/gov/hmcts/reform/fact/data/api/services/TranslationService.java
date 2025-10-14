package uk.gov.hmcts.reform.fact.data.api.services;

import uk.gov.hmcts.reform.fact.data.api.entities.CourtTranslation;
import uk.gov.hmcts.reform.fact.data.api.errorhandling.exceptions.NotFoundException;
import uk.gov.hmcts.reform.fact.data.api.repositories.CourtRepository;
import uk.gov.hmcts.reform.fact.data.api.repositories.CourtTranslationRepository;

import java.util.List;
import java.util.UUID;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class TranslationService {

    private final CourtTranslationRepository courtTranslationRepository;
    private final CourtRepository courtRepository;


    public CourtTranslation create(@NonNull CourtTranslation courtTranslation) {
        return courtTranslationRepository.save(courtTranslation);
    }

    public CourtTranslation update(@NonNull CourtTranslation courtTranslation) throws NotFoundException {
        if (!courtTranslationRepository.existsById(courtTranslation.getId())) {
            throw new NotFoundException("No Translation found with id " + courtTranslation.getId());
        }
        return courtTranslationRepository.save(courtTranslation);
    }

    public CourtTranslation retrieve(@NonNull UUID id) throws NotFoundException {
        return courtTranslationRepository.findById(id).orElseThrow(
            () -> new NotFoundException("No Translation found for id " + id));
    }

    public List<CourtTranslation> retrieveAll() {
        return courtTranslationRepository.findAll();
    }

    public void delete(@NonNull UUID id) {
        courtTranslationRepository.deleteById(id);
    }

}
