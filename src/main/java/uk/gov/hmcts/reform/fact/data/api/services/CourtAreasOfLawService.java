package uk.gov.hmcts.reform.fact.data.api.services;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.fact.data.api.entities.AreaOfLawType;
import uk.gov.hmcts.reform.fact.data.api.entities.Court;
import uk.gov.hmcts.reform.fact.data.api.entities.CourtAreasOfLaw;
import uk.gov.hmcts.reform.fact.data.api.errorhandling.exceptions.NotFoundException;
import uk.gov.hmcts.reform.fact.data.api.repositories.CourtAreasOfLawRepository;

import java.util.UUID;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Slf4j
public class CourtAreasOfLawService {

    private final CourtAreasOfLawRepository courtAreasOfLawRepository;
    private final CourtService courtService;
    private final TypesService typesService;

    public CourtAreasOfLawService(
        CourtAreasOfLawRepository courtAreasOfLawRepository,
        CourtService courtService,
        TypesService typesService) {
        this.courtAreasOfLawRepository = courtAreasOfLawRepository;
        this.courtService = courtService;
        this.typesService = typesService;
    }

    /**
     * Get areas of law by court id.
     *
     * @param courtId The court id to find the areas of law for.
     * @return Areas of law records or null if one doesn't exist for the court.
     * @throws NotFoundException if no areas of law record exists for the court.
     */
    public CourtAreasOfLaw getCourtAreasOfLawByCourtId(UUID courtId) {
        return courtAreasOfLawRepository.findByCourtId(courtService.getCourtById(courtId).getId())
            .orElseThrow(() -> new
                NotFoundException("No court areas of law found for court id: " + courtId)
            );
    }


    /**
     * Get a map of areas of law types and their status for a court.
     *
     * @param courtId The court id to find the areas of law for.
     * @return Map of area of law types with boolean values indicating if they are available at the court.
     * @throws NotFoundException if no areas of law record exists for the court.
     */
    public Map<AreaOfLawType, Boolean> getAreasOfLawStatusByCourtId(UUID courtId) {
        CourtAreasOfLaw courtAreasOfLaw = getCourtAreasOfLawByCourtId(courtId);
        List<AreaOfLawType> allAreasOfLawTypes = typesService.getAreaOfLawTypes();
        List<UUID> courtAreasOfLawIds = courtAreasOfLaw.getAreasOfLaw();

        return allAreasOfLawTypes.stream()
            .collect(Collectors.toMap(
                areaOfLawType -> areaOfLawType,
                areaOfLawType -> courtAreasOfLawIds.contains(areaOfLawType.getId())
            ));
    }


    /**
     * Set an areas of law record for a court.
     *
     * @param courtId The id of the court to set an areas of law for.
     * @param courtAreasOfLaw The court areas of law entity to create.
     * @return The created areas of law entity.
     */
    public CourtAreasOfLaw setCourtAreasOfLaw(UUID courtId, CourtAreasOfLaw courtAreasOfLaw) {
        log.info("Setting areas of law for court id: {}", courtId);
        Court foundCourt = courtService.getCourtById(courtId);

        courtAreasOfLaw.setCourt(foundCourt);
        courtAreasOfLaw.setCourtId(courtId);

        courtAreasOfLawRepository.findByCourtId(courtId).ifPresent(
            existing -> courtAreasOfLaw.setId(existing.getId())
        );

        return courtAreasOfLawRepository.save(courtAreasOfLaw);
    }
}
