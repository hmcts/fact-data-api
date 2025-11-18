package uk.gov.hmcts.reform.fact.data.api.services;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.hmcts.reform.fact.data.api.entities.Court;
import uk.gov.hmcts.reform.fact.data.api.entities.Region;
import uk.gov.hmcts.reform.fact.data.api.errorhandling.exceptions.NotFoundException;
import uk.gov.hmcts.reform.fact.data.api.repositories.CourtRepository;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
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
     * Deletes courts whose names start with the supplied prefix.
     *
     * @param courtNamePrefix the name prefix to match (case-insensitive).
     * @return the number of courts removed.
     */
    @Transactional
    public long deleteCourtsByNamePrefix(String courtNamePrefix) {
        List<Court> courtsToDelete = courtRepository.findByNameStartingWithIgnoreCase(courtNamePrefix.trim());

        if (courtsToDelete.isEmpty()) {
            return 0;
        }

        courtRepository.deleteAllInBatch(courtsToDelete);
        return courtsToDelete.size();
    }

    /**
     * Marks courts received from CaTH as open and records which MRD IDs could not be matched.
     *
     * @param mrdIds the MRD IDs supplied by CaTH.
     * @return the matched and unmatched MRD IDs.
     */
    public Map<String, Object> linkCathCourtsToFact(List<String> mrdIds) {
        List<Map<String, Object>> matchedLocations = new ArrayList<>();
        List<String> unmatchedIds = new ArrayList<>();

        mrdIds.forEach(mrdId -> {
            courtRepository.findByMrdId(mrdId).ifPresentOrElse(court -> {
                court.setOpenOnCath(true);
                courtRepository.save(court);
                matchedLocations.add(Map.of(
                    "mrdId", mrdId,
                    "isOpen", court.getOpen()
                ));
            }, () -> unmatchedIds.add(mrdId));
        });

        return Map.of(
            "matchedLocations", matchedLocations,
            "unmatchedLocations", unmatchedIds
        );
    }

    /**
     * Marks the court as closed on CaTH when its link has been deleted.
     *
     * @param mrdId the MRD ID supplied by CaTH.
     */
    @Transactional
    public void handleCathCourtDeletion(String mrdId) {
        Court court = courtRepository.findByMrdId(mrdId)
            .orElseThrow(() -> new NotFoundException("Court not found, MRD ID: " + mrdId));
        court.setOpenOnCath(Boolean.FALSE);
        courtRepository.save(court);
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
