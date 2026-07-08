package uk.gov.hmcts.reform.fact.data.api.services;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.fact.data.api.entities.AreaOfLawType;
import uk.gov.hmcts.reform.fact.data.api.entities.CourtAreasOfLaw;
import uk.gov.hmcts.reform.fact.data.api.errorhandling.exceptions.NotFoundException;
import uk.gov.hmcts.reform.fact.data.api.repositories.CourtAreasOfLawRepository;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CourtAreasOfLawService {

    private final CourtAreasOfLawRepository courtAreasOfLawRepository;
    private final CourtService courtService;
    private final TypesService typesService;
    private final CourtLocalAuthoritiesService courtLocalAuthoritiesService;

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
     * @throws NotFoundException if the court does not exist.
     */
    public Map<AreaOfLawType, Boolean> getAreasOfLawStatusByCourtId(UUID courtId) {
        courtService.getCourtById(courtId);

        List<UUID> courtAreasOfLawIds = courtAreasOfLawRepository.findByCourtId(courtId)
            .map(CourtAreasOfLaw::getAreasOfLaw)
            .map(areasOfLaw -> areasOfLaw == null ? List.<UUID>of() : areasOfLaw)
            .orElse(List.of());
        List<AreaOfLawType> allAreasOfLawTypes = typesService.getAreaOfLawTypes();

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
        courtAreasOfLaw.setCourt(courtService.getCourtById(courtId));
        courtAreasOfLaw.setCourtId(courtId);

        courtAreasOfLaw.setAreasOfLaw(typesService.getAllAreasOfLawTypesByIds(courtAreasOfLaw.getAreasOfLaw())
                                          .stream().map(AreaOfLawType::getId)
                                          .toList());

        courtAreasOfLawRepository.findByCourtId(courtId)
            .map(CourtAreasOfLaw::getId)
            .ifPresent(courtAreasOfLaw::setId);

        CourtAreasOfLaw response = courtAreasOfLawRepository.save(courtAreasOfLaw);
        courtLocalAuthoritiesService.performHousekeeping(courtId);
        return response;
    }
}
