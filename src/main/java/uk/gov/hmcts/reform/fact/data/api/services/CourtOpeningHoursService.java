package uk.gov.hmcts.reform.fact.data.api.services;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.hmcts.reform.fact.data.api.entities.OpeningHourType;
import uk.gov.hmcts.reform.fact.data.api.entities.Court;
import uk.gov.hmcts.reform.fact.data.api.entities.CourtCounterServiceOpeningHours;
import uk.gov.hmcts.reform.fact.data.api.entities.CourtOpeningHours;
import uk.gov.hmcts.reform.fact.data.api.entities.types.DayOfTheWeek;
import uk.gov.hmcts.reform.fact.data.api.errorhandling.exceptions.CourtResourceNotFoundException;
import uk.gov.hmcts.reform.fact.data.api.repositories.CourtCounterServiceOpeningHoursRepository;
import uk.gov.hmcts.reform.fact.data.api.repositories.CourtOpeningHoursRepository;

import java.util.ArrayList;
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
     * @return The list of opening hours.
     * @throws CourtResourceNotFoundException if no opening hours record exists for the court or the list is empty.
     */
    public List<CourtOpeningHours> getOpeningHoursByCourtId(UUID courtId) {
        return courtOpeningHoursRepository
            .findByCourtId(courtService.getCourtById(courtId).getId())
            .filter(list -> !list.isEmpty())
            .orElseThrow(() -> new CourtResourceNotFoundException(
                "No opening hours found for court ID: " + courtId));
    }

    /**
     * Get opening hours type by court ID.
     *
     * @param courtId The court ID to find the opening hours type for.
     * @param openingHourTypeId The type ID for the opening hours type to find.
     * @return The opening hours record.
     * @throws CourtResourceNotFoundException if no opening hours record of this type exists for the court.
     */
    public List<CourtOpeningHours> getOpeningHoursByTypeId(UUID courtId, UUID openingHourTypeId) {
        return courtOpeningHoursRepository
            .findByCourtIdAndOpeningHourTypeId(
                courtService.getCourtById(courtId).getId(),
                openingHoursTypeService.getOpeningHourTypeById(openingHourTypeId).getId())
            .filter(list -> !list.isEmpty())
            .orElseThrow(
                () -> new CourtResourceNotFoundException(
                "No opening hour found for court ID: " + courtId + " with type ID: " + openingHourTypeId));
    }

    /**
     * Get counter-service opening hours by court ID.
     * A court will only ever have zero or one counter-service opening hours record.
     *
     * @param courtId The court ID to find the counter-service opening hours for.
     * @return The counter-service opening hours record.
     * @throws CourtResourceNotFoundException if no counter-service opening hours record exists for the court.
     */
    public List<CourtCounterServiceOpeningHours> getCounterServiceOpeningHoursByCourtId(UUID courtId) {
        return courtCounterServiceOpeningHoursRepository
            .findByCourtId(courtService.getCourtById(courtId).getId())
            .filter(list -> !list.isEmpty())
            .orElseThrow(
                () -> new CourtResourceNotFoundException(
                    "No counter service opening hours found for court ID: " + courtId));
    }

    /**
     * Set opening hours for a court.
     * @param courtId The ID of the court to set opening hours for.
     * @param openingHoursTypeId The ID of the opening hours type to set opening hours for.
     * @param courtOpeningHours The court opening hours entity to create or update.
     * @return The created or updated opening hour entity.
     */
    @Transactional
    public List<CourtOpeningHours> setOpeningHours(
        UUID courtId, UUID openingHoursTypeId, List<CourtOpeningHours> courtOpeningHours) {

        List<CourtOpeningHours> hoursToSave = new ArrayList<>(courtOpeningHours);

        // Check to ensure both options of everyday and specific days are not set together
        // and to ensure everyday takes preference if present.
        if (hoursToSave
            .stream()
            .anyMatch(hour -> hour.getDayOfWeek() == DayOfTheWeek.EVERYDAY)) {
            hoursToSave
                .removeIf(hour -> hour.getDayOfWeek() != DayOfTheWeek.EVERYDAY);
        }

        Court foundCourt = courtService.getCourtById(courtId);
        OpeningHourType foundOpeningHourType = openingHoursTypeService.getOpeningHourTypeById(openingHoursTypeId);

        courtOpeningHoursRepository
            .deleteByCourtIdAndOpeningHourTypeId(foundCourt.getId(), foundOpeningHourType.getId());

        for (CourtOpeningHours hour : hoursToSave) {
            hour.setCourt(foundCourt);
            hour.setCourtId(courtId);
            hour.setOpeningHourType(foundOpeningHourType);
            hour.setOpeningHourTypeId(openingHoursTypeId);
        }

        return courtOpeningHoursRepository.saveAll(hoursToSave);
    }

    /**
     * Set counter-service opening hours for a court.
     * @param courtId The ID of the court to set counter-service opening hours for.
     * @param courtCounterServiceOpeningHours The court counter-service opening hours entity to create or update.
     * @return The created or updated counter-service opening hour entity.
     */
    @Transactional
    public List<CourtCounterServiceOpeningHours> setCounterServiceOpeningHours(
        UUID courtId, List<CourtCounterServiceOpeningHours> courtCounterServiceOpeningHours) {

        List<CourtCounterServiceOpeningHours> hoursToSave
            = new ArrayList<>(courtCounterServiceOpeningHours);

        // Check to ensure both options of everyday and specific days are not set together
        // and to ensure everyday takes preference if present.
        if (hoursToSave
            .stream()
            .anyMatch(hour -> hour.getDayOfWeek() == DayOfTheWeek.EVERYDAY)) {
            hoursToSave
                .removeIf(hour -> hour.getDayOfWeek() != DayOfTheWeek.EVERYDAY);
        }

        Court foundCourt = courtService.getCourtById(courtId);
        courtCounterServiceOpeningHoursRepository.deleteByCourtId(foundCourt.getId());

        for (CourtCounterServiceOpeningHours hour : hoursToSave) {
            hour.setCourt(foundCourt);
            hour.setCourtId(courtId);
        }

        return courtCounterServiceOpeningHoursRepository.saveAll(hoursToSave);
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
