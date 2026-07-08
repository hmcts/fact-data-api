package uk.gov.hmcts.reform.fact.data.api.services;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import uk.gov.hmcts.reform.fact.data.api.entities.ServiceArea;
import uk.gov.hmcts.reform.fact.data.api.entities.ServiceCentre;
import uk.gov.hmcts.reform.fact.data.api.entities.ServiceCentreDetails;
import uk.gov.hmcts.reform.fact.data.api.entities.types.CatchmentType;
import uk.gov.hmcts.reform.fact.data.api.entities.types.NameAndId;
import uk.gov.hmcts.reform.fact.data.api.errorhandling.exceptions.NotFoundException;
import uk.gov.hmcts.reform.fact.data.api.repositories.AuditRepository;
import uk.gov.hmcts.reform.fact.data.api.repositories.ServiceAreaRepository;
import uk.gov.hmcts.reform.fact.data.api.repositories.ServiceCentreDetailsRepository;
import uk.gov.hmcts.reform.fact.data.api.repositories.ServiceCentreRepository;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ServiceCentreService {

    private final ServiceCentreRepository serviceCentreRepository;
    private final AuditRepository auditRepository;
    private final ServiceCentreDetailsRepository serviceCentreDetailsRepository;
    private final ServiceAreaRepository serviceAreaRepository;
    private final RegionService regionService;

    /**
     * Get a service centre by id.
     *
     * @param serviceCentreId The ID of the service centre to get.
     * @return The service centre entity.
     * @throws NotFoundException if the service centre is not found.
     */
    public ServiceCentre getServiceCentreById(UUID serviceCentreId) {
        return serviceCentreRepository.findById(serviceCentreId)
            .orElseThrow(() -> new NotFoundException("Service centre not found, ID: " + serviceCentreId));
    }

    /**
     * Get service centre details by id.
     *
     * @param serviceCentreId The ID of the service centre details to get.
     * @return The service centre details entity.
     * @throws NotFoundException if the service centre details are not found.
     */
    public ServiceCentreDetails getServiceCentreDetailsById(UUID serviceCentreId) {
        return serviceCentreDetailsRepository.findById(serviceCentreId)
            .orElseThrow(() -> new NotFoundException("Service centre not found, ID: " + serviceCentreId));
    }

    /**
     * Get service centre details by slug.
     *
     * @param serviceCentreSlug The slug of the service centre details to get.
     * @return The service centre details entity.
     * @throws NotFoundException if the service centre details are not found.
     */
    public ServiceCentreDetails getServiceCentreDetailsBySlug(String serviceCentreSlug) {
        return serviceCentreDetailsRepository.findBySlug(serviceCentreSlug)
            .orElseThrow(() -> new NotFoundException("Service centre not found, slug: " + serviceCentreSlug));
    }

    /**
     * Get a service centre by its exact name.
     *
     * @param serviceCentreName The exact name of the service centre to get.
     * @return The service centre entity.
     * @throws NotFoundException if the service centre is not found.
     */
    public ServiceCentre getServiceCentreByName(String serviceCentreName) {
        return serviceCentreRepository.findByName(serviceCentreName)
            .orElseThrow(() -> new NotFoundException("Service centre not found, name: " + serviceCentreName));
    }

    /**
     * Creates a new service centre.
     *
     * @param serviceCentre The service centre to create.
     * @return The created service centre.
     */
    public ServiceCentre createServiceCentre(ServiceCentre serviceCentre) {
        serviceCentre.setId(null);
        serviceCentre.setSlug(toUniqueSlug(serviceCentre.getName()));
        serviceCentre.setOpen(false);
        serviceCentre.setServiceAreaIds(getValidatedServiceAreaIds(serviceCentre.getServiceAreaIds()));
        serviceCentre.setRegionId(getValidatedRegionId(serviceCentre.getRegionId()));
        serviceCentre.setCatchmentType(getCatchmentTypeOrDefault(serviceCentre.getCatchmentType()));

        return serviceCentreRepository.save(serviceCentre);
    }

    /**
     * Updates an existing service centre.
     *
     * @param serviceCentreId The id of the service centre to update.
     * @param serviceCentre The service centre entity with updated values.
     * @return The updated service centre.
     * @throws NotFoundException if the service centre is not found.
     */
    public ServiceCentre updateServiceCentre(UUID serviceCentreId, ServiceCentre serviceCentre) {
        ServiceCentre existingServiceCentre = getServiceCentreById(serviceCentreId);

        if (!existingServiceCentre.getName().equalsIgnoreCase(serviceCentre.getName())) {
            existingServiceCentre.setName(serviceCentre.getName());
            existingServiceCentre.setSlug(toUniqueSlug(serviceCentre.getName()));
        }

        existingServiceCentre.setOpen(serviceCentre.getOpen());
        existingServiceCentre.setWarningNotice(serviceCentre.getWarningNotice());
        existingServiceCentre.setServiceAreaIds(getValidatedServiceAreaIds(serviceCentre.getServiceAreaIds()));
        existingServiceCentre.setRegionId(getValidatedRegionId(serviceCentre.getRegionId()));
        existingServiceCentre.setCatchmentType(getCatchmentTypeOrDefault(serviceCentre.getCatchmentType()));

        return serviceCentreRepository.save(existingServiceCentre);
    }

    /**
     * Delete service centres matching the provided name prefix.
     *
     * @param serviceCentreNamePrefix The name prefix to match.
     * @return The number of service centres deleted.
     */
    @Transactional
    public long deleteServiceCentresByNamePrefix(String serviceCentreNamePrefix, boolean purgeAudits) {
        List<ServiceCentre> serviceCentresToDelete =
            serviceCentreRepository.findByNameStartingWithIgnoreCase(serviceCentreNamePrefix.trim());

        if (serviceCentresToDelete.isEmpty()) {
            return 0;
        }

        if  (purgeAudits) {
            auditRepository.deleteBySubjectIdIn(serviceCentresToDelete.stream().map(ServiceCentre::getId).toList());
        }

        serviceCentreRepository.deleteAllInBatch(serviceCentresToDelete);
        return serviceCentresToDelete.size();
    }

    /**
     * get all service centre names mapped to their ids.
     *
     * @return a {@link List} of service centre names with their ids.
     */
    public List<NameAndId> getAllServiceCentreNameAndIds() {
        return serviceCentreRepository.findAllNameAndId();
    }

    /**
     * Convert a service centre name into slug format.
     *
     * @param name The service centre name.
     * @return The slug-formatted name.
     */
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

    private CatchmentType getCatchmentTypeOrDefault(CatchmentType catchmentType) {
        return catchmentType == null ? CatchmentType.NATIONAL : catchmentType;
    }

    private UUID getValidatedRegionId(UUID regionId) {
        return regionId == null ? null : regionService.getRegionById(regionId).getId();
    }

}
