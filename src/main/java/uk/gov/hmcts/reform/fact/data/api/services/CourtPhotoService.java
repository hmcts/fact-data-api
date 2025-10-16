package uk.gov.hmcts.reform.fact.data.api.services;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import uk.gov.hmcts.reform.fact.data.api.entities.CourtPhoto;
import uk.gov.hmcts.reform.fact.data.api.errorhandling.exceptions.NotFoundException;
import uk.gov.hmcts.reform.fact.data.api.repositories.CourtPhotoRepository;

import java.util.UUID;

@Slf4j
@Service
public class CourtPhotoService {

    private final CourtPhotoRepository courtPhotoRepository;
    private final CourtService courtService;
    private final AzureBlobService azureBlobService;

    public CourtPhotoService(CourtPhotoRepository courtPhotoRepository,
                             CourtService courtService, AzureBlobService azureBlobService) {
        this.courtPhotoRepository = courtPhotoRepository;
        this.courtService = courtService;
        this.azureBlobService = azureBlobService;
    }

    /**
     * Get a court photo by court id.
     *
     * @param courtId The id of the court.
     * @return The court photo entity.
     * @throws NotFoundException if no court found with the given id or no photo found for the court.
     */
    public CourtPhoto getCourtPhotoByCourtId(UUID courtId) {
        courtService.getCourtById(courtId);
        return courtPhotoRepository.findCourtPhotoByCourtId(courtId).orElseThrow(
            () -> new NotFoundException("Court photo not found for court ID: " + courtId)
        );
    }

    /**
     * Set or update a court photo.
     *
     * @param courtId The id of the court.
     * @param courtPhoto The court photo entity to save.
     * @return The saved court photo.
     */
    public CourtPhoto setCourtPhoto(UUID courtId, MultipartFile file) {
        courtService.getCourtById(courtId);

        CourtPhoto courtPhoto = courtPhotoRepository.findCourtPhotoByCourtId(courtId)
            .orElse(new CourtPhoto());

        courtPhoto.setCourtId(courtId);

        // Set file name / link


        //TODO: Set real user ID when implemented.
        courtPhoto.setUpdatedByUserId(UUID.randomUUID());




        return courtPhotoRepository.save(courtPhoto);
    }

    /**
     * Delete a court photo by court id.
     *
     * @param courtId The id of the court.
     * @throws NotFoundException if no court found with the given id or no photo found for the court.
     */
    public void deleteCourtPhotoByCourtId(UUID courtId) {
        log.info("Deleting court photo for court ID: {}", courtId);
        courtService.getCourtById(courtId);
        CourtPhoto courtPhoto = getCourtPhotoByCourtId(courtId);

        azureBlobService.deleteBlob(courtPhoto.getFileLink());
        courtPhotoRepository.deleteById(courtPhoto.getId());
    }
}
