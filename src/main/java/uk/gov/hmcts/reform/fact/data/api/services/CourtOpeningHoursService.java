package uk.gov.hmcts.reform.fact.data.api.services;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.hmcts.reform.fact.data.api.entities.OpeningHourType;
import uk.gov.hmcts.reform.fact.data.api.entities.Court;
import uk.gov.hmcts.reform.fact.data.api.entities.CourtCounterServiceOpeningHours;
import uk.gov.hmcts.reform.fact.data.api.entities.CourtOpeningHours;
import uk.gov.hmcts.reform.fact.data.api.errorhandling.exceptions.CourtResourceNotFoundException;
import uk.gov.hmcts.reform.fact.data.api.repositories.CourtCounterServiceOpeningHoursRepository;
import uk.gov.hmcts.reform.fact.data.api.repositories.CourtOpeningHoursRepository;

import java.util.List;
import java.util.UUID;

@Service
@Slf4j
public class CourtOpeningHoursService {

    private final CourtOpeningHoursRepository courtOpeningHoursRepository;
    private final CourtCounterServiceOpeningHoursRepository courtCounterServiceOpeningHoursRepository;
    private final CourtService courtService;
    private final OpeningHoursTypeService openingHoursTypeService;

    public CourtOpeningHoursService(
        CourtOpeningHoursRepository courtOpeningHoursRepository,
        CourtCounterServiceOpeningHoursRepository courtCounterServiceOpeningHoursRepository,
        CourtService courtService,
        OpeningHoursTypeService openingHoursTypeService) {
        this.courtOpeningHoursRepository = courtOpeningHoursRepository;
        this.courtCounterServiceOpeningHoursRepository = courtCounterServiceOpeningHoursRepository;
        this.courtService = courtService;
        this.openingHoursTypeService = openingHoursTypeService;
    }

    /**
     * Get a list of opening hours by court ID.
     *
     * @param courtId The court ID to find the opening hours for.
     * @return The list of opening hours record or 204 if the court has no opening hours.
     * @throws CourtResourceNotFoundException  if no opening hours record exists for the court.
     */
    public List<CourtOpeningHours> getOpeningHoursByCourtId(UUID courtId) {
        var openingHours = courtOpeningHoursRepository.findByCourtId(courtService.getCourtById(courtId).getId());
        if (openingHours.isEmpty()) {
            throw new CourtResourceNotFoundException("No opening hours found for court ID: " + courtId);
        }
        return openingHours;
    }

    /**
     * Get opening hours type by court ID.
     *
     * @param courtId The court ID to find the opening hours type for.
     * @param openingHourTypeId The type ID for the opening hours type to find.
     * @return The opening hours record or null if one doesn't exist for the court.
     * @throws CourtResourceNotFoundException if no opening hours record of this type exists for the court.
     */
    public CourtOpeningHours getOpeningHourByTypeId(UUID courtId, UUID openingHourTypeId) {
        UUID validatedCourtId = courtService.getCourtById(courtId).getId();
        UUID validatedOpeningHourTypeId = openingHoursTypeService.getOpeningHourTypeById(openingHourTypeId).getId();

        return courtOpeningHoursRepository
                .findByCourtIdAndOpeningHourTypeId(validatedCourtId, validatedOpeningHourTypeId)
                .orElseThrow(() -> new CourtResourceNotFoundException(
                        "No opening hour found for court ID: " + courtId + " with type ID: " + openingHourTypeId
                ));
    }

    /**
     * Get counter-service opening hours by court ID.
     * A court will only ever have zero or one counter-service opening hours record.
     *
     * @param courtId The court ID to find the counter-service opening hours for.
     * @return The counter-service opening hours record or null if one doesn't exist for the court.
     * @throws CourtResourceNotFoundException if no counter-service opening hours record exists for the court.
     */
    public CourtCounterServiceOpeningHours getCounterServiceOpeningHoursByCourtId(UUID courtId) {
        return courtCounterServiceOpeningHoursRepository.findByCourtId(courtService.getCourtById(courtId).getId())
            .orElseThrow(() -> new
                CourtResourceNotFoundException("No counter service opening hours found for court ID: " + courtId)
            );
    }

    /**
     * Set opening hours for a court.
     * @param courtId The ID of the court to set opening hours for.
     * @param openingHoursTypeId The ID of the opening hours type to set opening hours for.
     * @param courtOpeningHours The court opening hours entity to create or update.
     * @return The created or updated opening hours entity.
     */
    public CourtOpeningHours setOpeningHours(
        UUID courtId, UUID openingHoursTypeId, CourtOpeningHours courtOpeningHours) {

        Court foundCourt = courtService.getCourtById(courtId);
        courtOpeningHours.setCourt(foundCourt);
        courtOpeningHours.setCourtId(courtId);

        OpeningHourType foundOpeningHourType = openingHoursTypeService.getOpeningHourTypeById(openingHoursTypeId);
        courtOpeningHours.setOpeningHourType(foundOpeningHourType);
        courtOpeningHours.setOpeningHourTypeId(openingHoursTypeId);

        courtOpeningHoursRepository.findByCourtIdAndOpeningHourTypeId(courtId, openingHoursTypeId).ifPresent(
            existing -> courtOpeningHours.setId(existing.getId())
        );

        return courtOpeningHoursRepository.save(courtOpeningHours);
    }

    /**
     * Set counter-service opening hours for a court.
     * @param courtId The ID of the court to set counter-service opening hours for.
     * @param courtCounterServiceOpeningHours The court counter-service opening hours entity to create or update.
     * @return The created or updated counter-service opening hours entity.
     */
    public CourtCounterServiceOpeningHours setCounterServiceOpeningHours(
        UUID courtId, CourtCounterServiceOpeningHours courtCounterServiceOpeningHours) {

        Court foundCourt = courtService.getCourtById(courtId);
        courtCounterServiceOpeningHours.setCourt(foundCourt);
        courtCounterServiceOpeningHours.setCourtId(courtId);

        courtCounterServiceOpeningHoursRepository.findByCourtId(courtId).ifPresent(
            existing -> courtCounterServiceOpeningHours.setId(existing.getId())
        );

        return courtCounterServiceOpeningHoursRepository.save(courtCounterServiceOpeningHours);
    }

    /**
     * Delete court opening hours.
     * @param courtId The ID of the court to delete opening hours for.
     * @param openingHoursTypeId The ID of the opening hours type to delete opening hours for.
     */
    @Transactional
    public void deleteCourtOpeningHours(UUID courtId, UUID openingHoursTypeId) {
        courtOpeningHoursRepository
            .deleteByCourtIdAndOpeningHourTypeId(
                courtService.getCourtById(courtId).getId(),
                openingHoursTypeService.getOpeningHourTypeById(openingHoursTypeId).getId());
    }
}
