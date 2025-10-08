package uk.gov.hmcts.reform.fact.data.api.services;

import uk.gov.hmcts.reform.fact.data.api.entities.Court;
import uk.gov.hmcts.reform.fact.data.api.errorhandling.exceptions.NotFoundException;
import uk.gov.hmcts.reform.fact.data.api.repositories.CourtRepository;
import uk.gov.hmcts.reform.fact.data.api.repositories.RegionRepository;

import java.util.List;
import java.util.UUID;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CourtService {

    private final CourtRepository courtRepository;
    private final RegionRepository regionRepository;

    public Court create(@NonNull Court court) {
        return courtRepository.save(court);
    }

    public Court update(@NonNull Court court) throws NotFoundException {
        if (!courtRepository.existsById(court.getId())) {
            throw new NotFoundException("No Court found with id " + court.getId());
        }
        return courtRepository.save(court);
    }

    public Court retrieve(@NonNull UUID id) throws NotFoundException {
        return courtRepository.findById(id).orElseThrow(() -> new NotFoundException("No Court found for id " + id));
    }

    public List<Court> retrieveAll() {
        return courtRepository.findAll();
    }

    public void delete(@NonNull UUID id) {
        courtRepository.deleteById(id);
    }

}
