package uk.gov.hmcts.reform.fact.data.api.services;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.fact.data.api.entities.AreaOfLawType;
import uk.gov.hmcts.reform.fact.data.api.entities.ServiceCentreAreasOfLaw;
import uk.gov.hmcts.reform.fact.data.api.errorhandling.exceptions.NotFoundException;
import uk.gov.hmcts.reform.fact.data.api.repositories.ServiceCentreAreasOfLawRepository;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ServiceCentreAreasOfLawService {

    private final ServiceCentreAreasOfLawRepository serviceCentreAreasOfLawRepository;
    private final ServiceCentreService serviceCentreService;
    private final TypesService typesService;

    /**
     * Get areas of law by service centre id.
     *
     * @param serviceCentreId The service centre id to find the areas of law for.
     * @return Areas of law record for the service centre.
     * @throws NotFoundException if no areas of law record exists for the service centre.
     */
    public ServiceCentreAreasOfLaw getServiceCentreAreasOfLawByServiceCentreId(UUID serviceCentreId) {
        return serviceCentreAreasOfLawRepository.findByServiceCentreId(
            serviceCentreService.getServiceCentreById(serviceCentreId).getId()
        ).orElseThrow(() -> new NotFoundException(
            "No service centre areas of law found for service centre id: " + serviceCentreId
        ));
    }

    /**
     * Get a map of areas of law types and their status for a service centre.
     *
     * @param serviceCentreId The service centre id to find the areas of law for.
     * @return Map of area of law types with boolean values indicating if they are available at the service centre.
     * @throws NotFoundException if no areas of law record exists for the service centre.
     */
    public Map<AreaOfLawType, Boolean> getAreasOfLawStatusByServiceCentreId(UUID serviceCentreId) {
        List<UUID> serviceCentreAreasOfLawIds =
            getServiceCentreAreasOfLawByServiceCentreId(serviceCentreId).getAreasOfLaw();
        List<AreaOfLawType> allAreasOfLawTypes = typesService.getAreaOfLawTypes();

        return allAreasOfLawTypes.stream()
            .collect(Collectors.toMap(
                areaOfLawType -> areaOfLawType,
                areaOfLawType -> serviceCentreAreasOfLawIds.contains(areaOfLawType.getId())
            ));
    }

    /**
     * Set an areas of law record for a service centre.
     *
     * @param serviceCentreId The id of the service centre to set areas of law for.
     * @param serviceCentreAreasOfLaw The service centre areas of law entity to create or update.
     * @return The created or updated areas of law entity.
     * @throws NotFoundException if the service centre or supplied areas of law do not exist.
     */
    public ServiceCentreAreasOfLaw setServiceCentreAreasOfLaw(UUID serviceCentreId,
                                                              ServiceCentreAreasOfLaw serviceCentreAreasOfLaw) {
        serviceCentreAreasOfLaw.setServiceCentre(serviceCentreService.getServiceCentreById(serviceCentreId));
        serviceCentreAreasOfLaw.setServiceCentreId(serviceCentreId);

        serviceCentreAreasOfLaw.setAreasOfLaw(
            typesService.getAllAreasOfLawTypesByIds(serviceCentreAreasOfLaw.getAreasOfLaw())
                .stream()
                .map(AreaOfLawType::getId)
                .toList()
        );

        serviceCentreAreasOfLawRepository.findByServiceCentreId(serviceCentreId)
            .map(ServiceCentreAreasOfLaw::getId)
            .ifPresent(serviceCentreAreasOfLaw::setId);

        return serviceCentreAreasOfLawRepository.save(serviceCentreAreasOfLaw);
    }
}
