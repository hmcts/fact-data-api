package uk.gov.hmcts.reform.fact.data.api.services;

import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.fact.data.api.entities.Region;
import uk.gov.hmcts.reform.fact.data.api.errorhandling.exceptions.NotFoundException;
import uk.gov.hmcts.reform.fact.data.api.repositories.RegionRepository;

import java.util.List;
import java.util.UUID;

@Service
public class RegionService {

    private final RegionRepository regionRepository;

    public RegionService(RegionRepository regionRepository) {
        this.regionRepository = regionRepository;
    }

    /**
     * Get all regions.
     *
     * @return A list of all regions.
     */
    public List<Region> getAllRegions() {
        return regionRepository.findAll();
    }

    /**
     * Get a region by its ID.
     *
     * @param id The ID of the region.
     * @return The region entity.
     * @throws NotFoundException if the region is not found.
     */
    public Region getRegionById(UUID id) {
        return regionRepository.findById(id)
            .orElseThrow(() -> new NotFoundException("Region not found, ID: " + id));
    }
}
