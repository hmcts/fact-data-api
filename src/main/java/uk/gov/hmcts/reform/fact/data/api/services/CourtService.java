package uk.gov.hmcts.reform.fact.data.api.services;

import feign.FeignException;
import lombok.extern.slf4j.Slf4j;
import uk.gov.hmcts.reform.fact.data.api.entities.Court;
import uk.gov.hmcts.reform.fact.data.api.entities.CourtDetails;
import uk.gov.hmcts.reform.fact.data.api.entities.Region;
import uk.gov.hmcts.reform.fact.data.api.errorhandling.exceptions.NotFoundException;
import uk.gov.hmcts.reform.fact.data.api.repositories.CourtDetailsRepository;
import uk.gov.hmcts.reform.fact.data.api.repositories.CourtRepository;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.hmcts.reform.fact.data.api.clients.CathClient;
import uk.gov.hmcts.reform.fact.data.api.clients.SlackClient;

import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class CourtService {

    private final CourtRepository courtRepository;
    private final CourtDetailsRepository courtDetailsRepository;
    private final RegionService regionService;
    private final CathClient cathClient;
    private final SlackClient slackClient;

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
     * Get multiple courts by their IDs.
     *
     * @param courtIds List of court IDs to retrieve.
     * @return List of courts matching the provided IDs.
     * @throws NotFoundException if a court is not found.
     */
    public List<Court> getAllCourtsByIds(List<UUID> courtIds) {
        return courtRepository.findAllById(courtIds);
    }

    /**
     * Get a paginated list of courts with optional filters.
     *
     * @param pageable         The pagination information.
     * @param includeClosed    Whether to include closed courts.
     * @param regionId         The region ID to filter by.
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
        court.setRegionId(foundRegion.getId());
        court.setSlug(toUniqueSlug(court.getName()));

        // A court is closed on creation until an address is added.
        court.setOpen(false);

        return courtRepository.save(court);
    }

    /**
     * Updates an existing court.
     * if the open status changes and the court is linked to CaTH, notifies CaTH of the change.
     *
     * @param courtId The id of the court to update.
     * @param court   The court entity with updated values.
     * @return The updated court.
     * @throws NotFoundException if the court is not found.
     */
    public Court updateCourt(UUID courtId, Court court) {
        Court existingCourt = getCourtById(courtId);
        final Boolean previousOpenStatus = existingCourt.getOpen();
        Region foundRegion = regionService.getRegionById(court.getRegionId());

        if (!existingCourt.getName().equalsIgnoreCase(court.getName())) {
            existingCourt.setName(court.getName());
            existingCourt.setSlug(toUniqueSlug(court.getName()));
        }

        existingCourt.setOpen(court.getOpen());
        existingCourt.setRegionId(foundRegion.getId());
        existingCourt.setWarningNotice(court.getWarningNotice());

        Court updatedCourt = courtRepository.save(existingCourt);

        handleCathNotification(previousOpenStatus, updatedCourt);

        return updatedCourt;
    }

    /**
     * Return a list of courts based on a provided prefix.
     *
     * @param prefix The prefix.
     * @return A list of courts based on the provided prefix.
     */
    public List<Court> getCourtsByPrefixAndActiveSearch(String prefix) {
        return new ArrayList<>(courtRepository.findCourtByNameStartingWithIgnoreCaseAndOpenOrderByNameAsc(
            prefix,
            true
        ));
    }

    /**
     * Search courts by a provided string query. Matches currently if the string
     * matches in part an address or court name.
     *
     * @param query The query to search by.
     * @return One or more courts that match.
     */
    public List<Court> searchOpenCourtsByNameOrAddress(String query) {
        return courtRepository.searchOpenByNameOrAddress(query.trim());
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


    // -- Court Details --

    /**
     * Get a court details by id.
     *
     * @param courtId The ID of the court details to get.
     * @return The court details entity.
     * @throws NotFoundException if the court details is not found.
     */
    public CourtDetails getCourtDetailsById(UUID courtId) {
        return courtDetailsRepository.findById(courtId)
            .orElseThrow(() -> new NotFoundException("Court not found, ID: " + courtId)
            );
    }

    /**
     * Get court details by slug.
     *
     * @param courtSlug The slug of the court details to get.
     * @return The court details entity.
     * @throws NotFoundException if the court details is not found.
     */
    public CourtDetails getCourtDetailsBySlug(String courtSlug) {
        return courtDetailsRepository.findBySlug(courtSlug)
            .orElseThrow(() -> new NotFoundException("Court not found, slug: " + courtSlug)
        );
    }

    /**
     * Get all court details.
     *
     * @return The list of court details entities.
     */
    public List<CourtDetails> getAllCourtDetails() {
        return courtDetailsRepository.findAll();
    }

    // -- Utilities --

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
        String baseSlug = toSlugFormat(name);

        String slug = baseSlug;
        int counter = 1;

        while (courtRepository.existsBySlug(slug)) {
            slug = baseSlug + "-" + counter++;
        }

        return slug;
    }

    /**
     * Handles notification to CaTH when a court's open status changes and it is linked to CaTH.
     *
     * @param previousOpen the previous open status of the court.
     * @param court the up-to-date court entity.
     */
    private void handleCathNotification(Boolean previousOpen, Court court) {
        boolean shouldNotify =
            Boolean.TRUE.equals(previousOpen) != Boolean.TRUE.equals(court.getOpen())
                && court.getMrdId() != null
                && !court.getMrdId().isBlank()
                && Boolean.TRUE.equals(court.getOpenOnCath());

        if (shouldNotify) {
            try {
                log.info("Notifying CaTH {}", court.getMrdId());
                cathClient.notifyCourtStatusChange(
                    court.getMrdId(),
                    Map.of("isOpen", Boolean.TRUE.equals(court.getOpen()))
                );
            } catch (FeignException ex) {
                slackClient.sendSlackMessage(String.format(
                    """
                        :rotating_light: *FaCT notification to CaTH failed* :rotating_light:
                        *Court name:* %s
                        *Court ID:* `%s`
                        *MRD ID:* `%s`
                        *Status code:* %s
                        *Error message:* `%s`

                        <!subteam^S09TS4ASQ7R|@fact-bsp-devs> please investigate""",
                    court.getName(),
                    court.getId(),
                    court.getMrdId(),
                    HttpStatus.resolve(ex.status()),
                    ex.getMessage()
                ));
                log.error("Error notifying CaTH. MRD ID: {}, Error: {}", court.getMrdId(), ex.getMessage());
            }
        }
    }

    /**
     * Util method to convert Court name to slug format.
     *
     * <p>
     * Further checks need to be made to ensure the value is unique before assigning to a court entity.
     *
     * @param name the court name
     * @return the slug representation of the name
     */
    public String toSlugFormat(String name) {
        return name.toLowerCase()
            .replaceAll("[^a-z\\s-]", "")
            .replaceAll("[\\s-]+", "-")
            .replaceAll("(^-)|(-$)", "");
    }
}
