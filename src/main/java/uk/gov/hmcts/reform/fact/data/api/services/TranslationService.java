package uk.gov.hmcts.reform.fact.data.api.services;

import uk.gov.hmcts.reform.fact.data.api.entities.Translation;
import uk.gov.hmcts.reform.fact.data.api.errorhandling.exceptions.NotFoundException;
import uk.gov.hmcts.reform.fact.data.api.repositories.CourtRepository;
import uk.gov.hmcts.reform.fact.data.api.repositories.TranslationRepository;

import java.util.List;
import java.util.UUID;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class TranslationService {

    private final TranslationRepository translationRepository;
    private final CourtRepository courtRepository;


    public Translation create(@NonNull Translation translation) {
        return translationRepository.save(translation);
    }

    public Translation update(@NonNull Translation translation) throws NotFoundException {
        if (!translationRepository.existsById(translation.getId())) {
            throw new NotFoundException("No Translation found with id " + translation.getId());
        }
        return translationRepository.save(translation);
    }

    public Translation retrieve(@NonNull UUID id) throws NotFoundException {
        return translationRepository.findById(id).orElseThrow(
            () -> new NotFoundException("No Translation found for id " + id));
    }

    public List<Translation> retrieveAll() {
        return translationRepository.findAll();
    }

    public void delete(@NonNull UUID id) {
        translationRepository.deleteById(id);
    }

}
