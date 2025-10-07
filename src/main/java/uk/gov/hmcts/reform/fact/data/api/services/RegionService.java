package uk.gov.hmcts.reform.fact.data.api.services;

import uk.gov.hmcts.reform.fact.data.api.entities.Region;
import uk.gov.hmcts.reform.fact.data.api.errorhandling.exceptions.NotFoundException;
import uk.gov.hmcts.reform.fact.data.api.repositories.RegionRepository;

import java.util.List;
import java.util.UUID;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class RegionService {

    private final RegionRepository regionRepository;

    public Region create(@NonNull Region region) {
        return regionRepository.save(region);
    }

    public Region update(@NonNull Region region) throws NotFoundException {
        if (!regionRepository.existsById(region.getId())) {
            throw new NotFoundException("No Region found for id " + region.getId());
        }
        return regionRepository.save(region);
    }

    public Region retrieve(@NonNull UUID id) throws NotFoundException {
        return regionRepository.findById(id).orElseThrow(() -> new NotFoundException("No Region found for id " + id));
    }

    public List<Region> retrieveAll() {
        return regionRepository.findAll();
    }

    public void delete(@NonNull UUID id) {
        regionRepository.deleteById(id);
    }
}
