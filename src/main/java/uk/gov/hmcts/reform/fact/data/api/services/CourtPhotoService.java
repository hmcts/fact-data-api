package uk.gov.hmcts.reform.fact.data.api.services;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.fact.data.api.entities.CourtPhoto;
import uk.gov.hmcts.reform.fact.data.api.repositories.CourtPhotoRepository;

import java.util.UUID;

@Slf4j
@Service
public class CourtPhotoService {

    private final CourtPhotoRepository courtPhotoRepository;
    private final CourtService courtService;

    public CourtPhotoService(CourtPhotoRepository courtPhotoRepository, CourtService courtService) {
        this.courtPhotoRepository = courtPhotoRepository;
        this.courtService = courtService;
    }

    /**
     * Get a court photo by court id.
     *
     * @param courtId The id of the court.
     * @return The court photo entity.
     * @throws NotFoundException if no court found with the given id or no photo found for the court.
     */
    public CourtPhoto getCourtPhotoByCourtId(UUID courtId) {
        // courtService/
        return courtPhotoRepository.findCourtPhotoByCourtId(courtId).;
    }

    /**
     * Set or update a court photo.
     *
     * @param courtId The id of the court.
     * @param courtPhoto The court photo entity to save.
     * @return The saved court photo.
     */
    public CourtPhoto setCourtPhoto(UUID courtId, CourtPhoto courtPhoto) {
        return courtPhotoRepository.save(courtPhoto);
    }

    /**
     * Delete a court photo by court id.
     *
     * @param courtId The id of the court.
     */
    public void deleteCourtPhotoByCourtId(UUID courtId) {
        courtPhotoRepository.deleteById(courtId);
    }
}
