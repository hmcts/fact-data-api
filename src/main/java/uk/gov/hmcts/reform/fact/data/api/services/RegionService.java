package uk.gov.hmcts.reform.fact.data.api.services;

import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.fact.data.api.entities.Region;
import uk.gov.hmcts.reform.fact.data.api.errorhandling.exceptions.NotFoundException;
import uk.gov.hmcts.reform.fact.data.api.repositories.RegionRepository;

import java.util.UUID;

@Service
public class RegionService {

    private final RegionRepository regionRepository;

    public RegionService(RegionRepository regionRepository) {
        this.regionRepository = regionRepository;
    }

    public Region getRegionById(UUID id) {
        return regionRepository.findById(id)
            .orElseThrow(() -> new NotFoundException("Region not found, ID: " + id));
    }
}
