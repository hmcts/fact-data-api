package uk.gov.hmcts.reform.fact.data.api.services;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.hmcts.reform.fact.data.api.entities.ServiceArea;
import uk.gov.hmcts.reform.fact.data.api.entities.ServiceCentre;
import uk.gov.hmcts.reform.fact.data.api.entities.types.CatchmentType;
import uk.gov.hmcts.reform.fact.data.api.errorhandling.exceptions.NotFoundException;
import uk.gov.hmcts.reform.fact.data.api.repositories.ServiceAreaRepository;
import uk.gov.hmcts.reform.fact.data.api.repositories.ServiceCentreRepository;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ServiceCentreService {

    private final ServiceCentreRepository serviceCentreRepository;
    private final ServiceAreaRepository serviceAreaRepository;

    public ServiceCentre getServiceCentreById(UUID serviceCentreId) {
        return serviceCentreRepository.findById(serviceCentreId)
            .orElseThrow(() -> new NotFoundException("Service centre not found, ID: " + serviceCentreId));
    }

    public ServiceCentre getServiceCentreByName(String serviceCentreName) {
        return serviceCentreRepository.findByName(serviceCentreName)
            .orElseThrow(() -> new NotFoundException("Service centre not found, name: " + serviceCentreName));
    }

    public ServiceCentre createServiceCentre(ServiceCentre serviceCentre) {
        serviceCentre.setId(null);
        serviceCentre.setSlug(toUniqueSlug(serviceCentre.getName()));
        serviceCentre.setOpen(false);
        serviceCentre.setServiceAreaIds(getValidatedServiceAreaIds(serviceCentre.getServiceAreaIds()));
        serviceCentre.setCatchmentType(CatchmentType.NATIONAL);

        return serviceCentreRepository.save(serviceCentre);
    }

    public ServiceCentre updateServiceCentre(UUID serviceCentreId, ServiceCentre serviceCentre) {
        ServiceCentre existingServiceCentre = getServiceCentreById(serviceCentreId);

        if (!existingServiceCentre.getName().equalsIgnoreCase(serviceCentre.getName())) {
            existingServiceCentre.setName(serviceCentre.getName());
            existingServiceCentre.setSlug(toUniqueSlug(serviceCentre.getName()));
        }

        existingServiceCentre.setOpen(serviceCentre.getOpen());
        existingServiceCentre.setWarningNotice(serviceCentre.getWarningNotice());
        existingServiceCentre.setServiceAreaIds(getValidatedServiceAreaIds(serviceCentre.getServiceAreaIds()));
        existingServiceCentre.setCatchmentType(CatchmentType.NATIONAL);

        return serviceCentreRepository.save(existingServiceCentre);
    }

    @Transactional
    public long deleteServiceCentresByNamePrefix(String serviceCentreNamePrefix) {
        List<ServiceCentre> serviceCentresToDelete =
            serviceCentreRepository.findByNameStartingWithIgnoreCase(serviceCentreNamePrefix.trim());

        if (serviceCentresToDelete.isEmpty()) {
            return 0;
        }

        serviceCentreRepository.deleteAllInBatch(serviceCentresToDelete);
        return serviceCentresToDelete.size();
    }

    public String toSlugFormat(String name) {
        return name.toLowerCase()
            .replaceAll("[^a-z\\s-]", "")
            .replaceAll("[\\s-]+", "-")
            .replaceAll("(^-)|(-$)", "");
    }

    private String toUniqueSlug(String name) {
        String baseSlug = toSlugFormat(name);
        String slug = baseSlug;
        int counter = 1;

        while (serviceCentreRepository.existsBySlug(slug)) {
            slug = baseSlug + "-" + counter++;
        }

        return slug;
    }

    private List<UUID> getValidatedServiceAreaIds(List<UUID> serviceAreaIds) {
        if (serviceAreaIds == null) {
            return List.of();
        }

        return serviceAreaRepository.findAllById(serviceAreaIds)
            .stream()
            .map(ServiceArea::getId)
            .toList();
    }

}
