package uk.gov.hmcts.reform.fact.data.api.services;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.fact.data.api.entities.AreaOfLawType;
import uk.gov.hmcts.reform.fact.data.api.entities.ContactDescriptionType;
import uk.gov.hmcts.reform.fact.data.api.entities.ServiceArea;
import uk.gov.hmcts.reform.fact.data.api.entities.ServiceCentreAreasOfLaw;
import uk.gov.hmcts.reform.fact.data.api.entities.ServiceCentreContactDetails;
import uk.gov.hmcts.reform.fact.data.api.entities.ServiceCentreDetails;
import uk.gov.hmcts.reform.fact.data.api.repositories.ServiceAreaRepository;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ServiceCentreDetailsViewService {

    private final TypesService typesService;
    private final ServiceAreaRepository serviceAreaRepository;

    /**
     * Prepares the service centre details response by expanding UUID-backed fields into reference objects.
     *
     * @param serviceCentreDetails the service centre details entity to prepare.
     * @return the same entity after enrichment, or null if nothing was provided.
     */
    public ServiceCentreDetails prepareDetailsView(ServiceCentreDetails serviceCentreDetails) {
        if (serviceCentreDetails == null) {
            return null;
        }

        enrichServiceAreas(serviceCentreDetails);
        enrichContactDescriptions(serviceCentreDetails.getServiceCentreContactDetails());
        enrichServiceCentreAreasOfLaw(serviceCentreDetails.getServiceCentreAreasOfLaw());
        return serviceCentreDetails;
    }

    private void enrichServiceAreas(ServiceCentreDetails serviceCentreDetails) {
        List<UUID> serviceAreaIds = safeList(serviceCentreDetails.getServiceAreaIds());
        if (serviceAreaIds.isEmpty()) {
            return;
        }

        Map<UUID, ServiceArea> serviceAreasById = serviceAreaRepository.findAllById(serviceAreaIds).stream()
            .filter(Objects::nonNull)
            .collect(Collectors.toMap(ServiceArea::getId, Function.identity()));

        serviceCentreDetails.setServiceAreaDetails(
            serviceAreaIds.stream()
                .map(id -> serviceAreasById.getOrDefault(id, createServiceAreaStub(id)))
                .toList()
        );
    }

    private void enrichContactDescriptions(List<ServiceCentreContactDetails> contactDetails) {
        if (contactDetails == null || contactDetails.isEmpty()) {
            return;
        }

        List<UUID> descriptionTypeIds = contactDetails.stream()
            .map(ServiceCentreContactDetails::getServiceCentreContactDescriptionId)
            .filter(Objects::nonNull)
            .distinct()
            .toList();

        Map<UUID, ContactDescriptionType> descriptionTypesById = descriptionTypeIds.isEmpty()
            ? Collections.emptyMap()
            : typesService.getContactDescriptionTypesByIds(descriptionTypeIds).stream()
                .filter(Objects::nonNull)
                .collect(Collectors.toMap(ContactDescriptionType::getId, Function.identity()));

        contactDetails.forEach(contactDetail -> {
            UUID descriptionTypeId = contactDetail.getServiceCentreContactDescriptionId();
            ContactDescriptionType descriptionType = descriptionTypesById.get(descriptionTypeId);
            if (descriptionType == null && descriptionTypeId != null) {
                ContactDescriptionType fallback = new ContactDescriptionType();
                fallback.setId(descriptionTypeId);
                descriptionType = fallback;
            }
            contactDetail.setServiceCentreContactDescriptionDetails(descriptionType);
        });
    }

    private void enrichServiceCentreAreasOfLaw(List<ServiceCentreAreasOfLaw> serviceCentreAreasOfLaw) {
        if (serviceCentreAreasOfLaw == null || serviceCentreAreasOfLaw.isEmpty()) {
            return;
        }

        List<UUID> areaOfLawIds = serviceCentreAreasOfLaw.stream()
            .flatMap(area -> safeList(area.getAreasOfLaw()).stream())
            .distinct()
            .toList();

        Map<UUID, AreaOfLawType> areasOfLawById = areaOfLawIds.isEmpty()
            ? Collections.emptyMap()
            : typesService.getAllAreasOfLawTypesByIds(areaOfLawIds).stream()
                .filter(Objects::nonNull)
                .collect(Collectors.toMap(AreaOfLawType::getId, Function.identity()));

        serviceCentreAreasOfLaw.forEach(area -> area.setAreasOfLawDetails(
            safeList(area.getAreasOfLaw()).stream()
                .map(id -> areasOfLawById.getOrDefault(id, createAreaOfLawStub(id)))
                .toList()
        ));
    }

    private AreaOfLawType createAreaOfLawStub(UUID id) {
        if (id == null) {
            return null;
        }
        AreaOfLawType fallback = new AreaOfLawType();
        fallback.setId(id);
        return fallback;
    }

    private ServiceArea createServiceAreaStub(UUID id) {
        if (id == null) {
            return null;
        }
        ServiceArea fallback = new ServiceArea();
        fallback.setId(id);
        return fallback;
    }

    private <T> List<T> safeList(List<T> list) {
        return list == null ? Collections.emptyList() : list;
    }
}
