package uk.gov.hmcts.reform.fact.data.api.services;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.hmcts.reform.fact.data.api.entities.ContactDescriptionType;
import uk.gov.hmcts.reform.fact.data.api.entities.ServiceCentre;
import uk.gov.hmcts.reform.fact.data.api.entities.ServiceCentreContactDetails;
import uk.gov.hmcts.reform.fact.data.api.errorhandling.exceptions.NotFoundException;
import uk.gov.hmcts.reform.fact.data.api.repositories.ContactDescriptionTypeRepository;
import uk.gov.hmcts.reform.fact.data.api.repositories.ServiceCentreContactDetailsRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class ServiceCentreContactDetailsService {

    private final ServiceCentreContactDetailsRepository serviceCentreContactDetailsRepository;
    private final ContactDescriptionTypeRepository contactDescriptionTypeRepository;
    private final ServiceCentreService serviceCentreService;

    /**
     * Retrieve all contact details for a specific service centre.
     *
     * @param serviceCentreId The service centre identifier.
     * @return List of contact detail records for the service centre.
     * @throws NotFoundException if the service centre does not exist.
     */
    public List<ServiceCentreContactDetails> getContactDetails(UUID serviceCentreId) {
        serviceCentreService.getServiceCentreById(serviceCentreId);
        return serviceCentreContactDetailsRepository.findByServiceCentreId(serviceCentreId);
    }

    /**
     * Retrieve a single contact detail entry by service centre and contact identifiers.
     *
     * @param serviceCentreId The service centre identifier.
     * @param contactId The contact identifier.
     * @return Matching contact detail.
     * @throws NotFoundException if the service centre or contact detail does not exist.
     */
    public ServiceCentreContactDetails getContactDetail(UUID serviceCentreId, UUID contactId) {
        serviceCentreService.getServiceCentreById(serviceCentreId);
        return serviceCentreContactDetailsRepository.findByIdAndServiceCentreId(contactId, serviceCentreId)
            .orElseThrow(() -> new NotFoundException(
                "Service centre contact detail not found, contactId: " + contactId
                    + ", serviceCentreId: " + serviceCentreId
            ));
    }

    /**
     * Persist a new contact detail for a service centre.
     *
     * @param serviceCentreId The service centre identifier.
     * @param request The contact detail to create.
     * @return Created contact detail.
     * @throws NotFoundException if the service centre or supplied description type does not exist.
     */
    @Transactional
    public ServiceCentreContactDetails createContactDetail(UUID serviceCentreId, ServiceCentreContactDetails request) {
        ServiceCentre serviceCentre = serviceCentreService.getServiceCentreById(serviceCentreId);
        Optional<ContactDescriptionType> description =
            getValidatedContactDescription(request.getServiceCentreContactDescriptionId());

        request.setId(null);
        request.setServiceCentreId(serviceCentreId);
        request.setServiceCentre(serviceCentre);
        request.setServiceCentreContactDescription(description.orElse(null));

        log.info("Creating contact detail for service centre {}", serviceCentreId);
        return serviceCentreContactDetailsRepository.save(request);
    }

    /**
     * Update an existing contact detail for a service centre.
     *
     * @param serviceCentreId The service centre identifier.
     * @param contactId The contact identifier.
     * @param request Updated contact detail values.
     * @return Updated contact detail.
     * @throws NotFoundException if the service centre, contact detail, or supplied description type does not exist.
     */
    @Transactional
    public ServiceCentreContactDetails updateContactDetail(UUID serviceCentreId,
                                                           UUID contactId,
                                                           ServiceCentreContactDetails request) {
        ServiceCentreContactDetails existing = getContactDetail(serviceCentreId, contactId);
        Optional<ContactDescriptionType> description =
            getValidatedContactDescription(request.getServiceCentreContactDescriptionId());

        existing.setServiceCentreContactDescriptionId(request.getServiceCentreContactDescriptionId());
        existing.setServiceCentreContactDescription(description.orElse(null));
        existing.setExplanation(request.getExplanation());
        existing.setExplanationCy(request.getExplanationCy());
        existing.setEmail(request.getEmail());
        existing.setPhoneNumber(request.getPhoneNumber());

        log.info("Updating contact detail {} for service centre {}", contactId, serviceCentreId);
        return serviceCentreContactDetailsRepository.save(existing);
    }

    /**
     * Remove a contact detail for a service centre.
     *
     * @param serviceCentreId The service centre identifier.
     * @param contactId The contact identifier.
     * @throws NotFoundException if the service centre or contact detail does not exist.
     */
    @Transactional
    public void deleteContactDetail(UUID serviceCentreId, UUID contactId) {
        serviceCentreService.getServiceCentreById(serviceCentreId);
        if (!serviceCentreContactDetailsRepository.existsByIdAndServiceCentreId(contactId, serviceCentreId)) {
            throw new NotFoundException(
                "Service centre contact detail not found, contactId: " + contactId
                    + ", serviceCentreId: " + serviceCentreId
            );
        }

        log.info("Deleting contact detail {} for service centre {}", contactId, serviceCentreId);
        serviceCentreContactDetailsRepository.deleteByIdAndServiceCentreId(contactId, serviceCentreId);
    }

    private Optional<ContactDescriptionType> getValidatedContactDescription(UUID contactDescriptionId) {
        if (contactDescriptionId == null) {
            return Optional.empty();
        }

        return Optional.of(contactDescriptionTypeRepository.findById(contactDescriptionId).orElseThrow(
            () -> new NotFoundException("Contact description type not found, ID: " + contactDescriptionId)
        ));
    }
}
