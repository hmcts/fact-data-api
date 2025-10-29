package uk.gov.hmcts.reform.fact.data.api.services;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.fact.data.api.entities.Court;
import uk.gov.hmcts.reform.fact.data.api.entities.Region;
import uk.gov.hmcts.reform.fact.data.api.errorhandling.exceptions.NotFoundException;
import uk.gov.hmcts.reform.fact.data.api.repositories.CourtRepository;

import java.util.List;
import java.util.UUID;

@Service
public class CourtService {

    private final CourtRepository courtRepository;
    private final RegionService regionService;

    public CourtService(CourtRepository courtRepository, RegionService regionService) {
        this.courtRepository = courtRepository;
        this.regionService = regionService;
    }

    /**
     * Get a court by id.
     *
     * @param courtId The ID of the court to get.
     * @return The court entity.
     * @throws NotFoundException if the court is not found.
     */
    public Court getCourtById(UUID courtId) {
        return courtRepository.findById(courtId)
            .orElseThrow(() -> new NotFoundException("Court not found, ID: " + courtId)
        );
    }

    /**
     * Get a paginated list of courts with optional filters.
     *
     * @param pageable The pagination information.
     * @param includeClosed Whether to include closed courts.
     * @param regionId The region ID to filter by.
     * @param partialCourtName A partial court name to filter by.
     * @return A paginated list of courts.
     */
    public Page<Court> getFilteredAndPaginatedCourts(Pageable pageable, Boolean includeClosed,
                                                     String regionId, String partialCourtName) {

        String nameFilter = partialCourtName != null ? partialCourtName : "";

        List<UUID> regionIds = (regionId != null && !regionId.isBlank())
            ? List.of(regionService.getRegionById(UUID.fromString(regionId)).getId())
            : regionService.getAllRegions().stream()
            .map(Region::getId)
            .toList();

        return (includeClosed != null && includeClosed)
            ? courtRepository.findByRegionIdInAndNameContainingIgnoreCase(regionIds, nameFilter, pageable)
            : courtRepository.findByRegionIdInAndOpenTrueAndNameContainingIgnoreCase(regionIds, nameFilter, pageable);
    }

    /**
     * Creates a new court or service centre.
     *
     * @param court The court to create.
     * @return The created court.
     * @throws NotFoundException if the region is not found.
     */
    public Court createCourt(Court court) {
        Region foundRegion = regionService.getRegionById(court.getRegionId());
        court.setRegion(foundRegion);
        court.setSlug(toUniqueSlug(court.getName()));

        // A court is closed on creation until an address is added.
        court.setOpen(false);

        return courtRepository.save(court);
    }

    /**
     * Updates an existing court.
     *
     * @param courtId The id of the court to update.
     * @param court The court entity with updated values.
     * @return The updated court.
     * @throws NotFoundException if the court is not found.
     */
    public Court updateCourt(UUID courtId, Court court) {
        Court existingCourt = getCourtById(courtId);
        Region foundRegion = regionService.getRegionById(court.getRegionId());

        if (!existingCourt.getName().equalsIgnoreCase(court.getName())) {
            existingCourt.setName(court.getName());
            existingCourt.setSlug(toUniqueSlug(court.getName()));
        }

        existingCourt.setOpen(court.getOpen());
        existingCourt.setRegion(foundRegion);
        existingCourt.setRegionId(foundRegion.getId());
        existingCourt.setWarningNotice(court.getWarningNotice());

        return courtRepository.save(existingCourt);
    }


    /**
     * Converts a court name to a unique slug.
     *
     * @param name The court name.
     * @return A unique slug.
     */
    private String toUniqueSlug(String name) {
        String baseSlug = name.toLowerCase()
            .replaceAll("[^a-z\\s-]", "")
            .replaceAll("[\\s-]+", "-")
            .replaceAll("(^-)|(-$)", "");

        String slug = baseSlug;
        int counter = 1;

        while (courtRepository.existsBySlug(slug)) {
            slug = baseSlug + "-" + counter++;
        }

        return slug;
    }
}
