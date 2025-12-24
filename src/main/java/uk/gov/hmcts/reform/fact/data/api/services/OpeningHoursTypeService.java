package uk.gov.hmcts.reform.fact.data.api.services;

import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.fact.data.api.entities.OpeningHourType;
import uk.gov.hmcts.reform.fact.data.api.errorhandling.exceptions.CourtResourceNotFoundException;
import uk.gov.hmcts.reform.fact.data.api.repositories.OpeningHoursTypeRepository;

import java.util.UUID;

@Service
public class OpeningHoursTypeService {

    private final OpeningHoursTypeRepository openingHoursTypeRepository;

    public OpeningHoursTypeService(OpeningHoursTypeRepository openingHoursTypeRepository) {
        this.openingHoursTypeRepository = openingHoursTypeRepository;
    }

    /**
     * Get an opening hour type by id.
     *
     * @param openingHourTypeId The ID of the opening hour type to get.
     * @return The opening hour type entity.
     * @throws CourtResourceNotFoundException if the opening hour type is not found.
     */
    public OpeningHourType getOpeningHourTypeById(UUID openingHourTypeId) {
        return openingHoursTypeRepository.findById(openingHourTypeId).orElseThrow(
            () -> new CourtResourceNotFoundException("Opening hour type not found, ID: " + openingHourTypeId)
        );
    }
}
