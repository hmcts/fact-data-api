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

    public ServiceCentreAreasOfLaw getServiceCentreAreasOfLawByServiceCentreId(UUID serviceCentreId) {
        return serviceCentreAreasOfLawRepository.findByServiceCentreId(
            serviceCentreService.getServiceCentreById(serviceCentreId).getId()
        ).orElseThrow(() -> new NotFoundException(
            "No service centre areas of law found for service centre id: " + serviceCentreId
        ));
    }

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
