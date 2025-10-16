package uk.gov.hmcts.reform.fact.data.api.services;

import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.fact.data.api.entities.Court;
import uk.gov.hmcts.reform.fact.data.api.errorhandling.exceptions.NotFoundException;
import uk.gov.hmcts.reform.fact.data.api.repositories.CourtRepository;

import java.util.UUID;

@Service
public class CourtService {

    private final CourtRepository courtRepository;

    public CourtService(CourtRepository courtRepository) {
        this.courtRepository = courtRepository;
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
     * Creates a new court or service centre.
     *
     * @param court
     * @return
     */
    public Court createCourt(Court court) {
        // Check and set region also.



        court.setSlug(toSlug(court.getName()));
        // A court is closed on creation by default until an address is added.
        court.setOpen(false);

        // A court cannot be marked open on CaTH from the API, this is handled by the CaTH sync process.
        // An MRD can be set??
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

        existingCourt.setName(court.getName());
        existingCourt.setSlug(toSlug(court.getName()));
        existingCourt.setOpen(court.getOpen());
        existingCourt.setTemporaryUrgentNotice(court.getTemporaryUrgentNotice());

        return courtRepository.save(existingCourt);
    }

    /**
     * GET	/courts	Params: includeClosed, region, court name (partial search). Returns paginated list of courts for the admin search page
     * GET	/courts/all	Returns all court data - used only by the fact-cron-trigger for spreadsheet creation on data.gov.uk
     * GET	/courts/{courtId}	Get all the data that is displayed on the FaCT public court page (single court)
     * PUT	/courts/{courtId}	Updates the court name and if the court is open/closed, and a temporary urgent notice
     */


         // * POST	/courts/{courtId}/service-areas	Adds service areas for a service centre for a new court


    /**
     * Converts a court name to a slug.
     */
    private static String toSlug(String name) {
        return name.toLowerCase()
            .replaceAll("[^a-z\\s-]", "")
            .replaceAll("[\\s-]+", "-")
            .replaceAll("^-|-$", "");
    }
}
