package uk.gov.hmcts.reform.fact.data.api.services;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.hmcts.reform.fact.data.api.entities.ContactDescriptionType;
import uk.gov.hmcts.reform.fact.data.api.entities.Court;
import uk.gov.hmcts.reform.fact.data.api.entities.CourtContactDetails;
import uk.gov.hmcts.reform.fact.data.api.errorhandling.exceptions.NotFoundException;
import uk.gov.hmcts.reform.fact.data.api.repositories.ContactDescriptionTypeRepository;
import uk.gov.hmcts.reform.fact.data.api.repositories.CourtContactDetailsRepository;

import java.util.List;
import java.util.UUID;

@Service
@Slf4j
public class CourtContactDetailsService {

    private final CourtContactDetailsRepository courtContactDetailsRepository;
    private final ContactDescriptionTypeRepository contactDescriptionTypeRepository;
    private final CourtService courtService;

    public CourtContactDetailsService(CourtContactDetailsRepository courtContactDetailsRepository,
                                      ContactDescriptionTypeRepository contactDescriptionTypeRepository,
                                      CourtService courtService) {
        this.courtContactDetailsRepository = courtContactDetailsRepository;
        this.contactDescriptionTypeRepository = contactDescriptionTypeRepository;
        this.courtService = courtService;
    }

    /**
     * Retrieve all contact details for a specific court.
     *
     * @param courtId The court identifier.
     * @return List of contact detail records for the court.
     */
    public List<CourtContactDetails> getContactDetails(UUID courtId) {
        courtService.getCourtById(courtId);
        return courtContactDetailsRepository.findByCourtId(courtId);
    }

    /**
     * Retrieve a single contact detail entry by court and contact identifiers.
     *
     * @param courtId   The court identifier.
     * @param contactId The contact identifier.
     * @return Matching contact detail.
     */
    public CourtContactDetails getContactDetail(UUID courtId, UUID contactId) {
        courtService.getCourtById(courtId);
        return courtContactDetailsRepository.findByIdAndCourtId(contactId, courtId).orElseThrow(
            () -> new NotFoundException(
                "Court contact detail not found, contactId: " + contactId + ", courtId: " + courtId
            )
        );
    }

    /**
     * Persist a new contact detail for a court.
     *
     * @param courtId The court identifier.
     * @param request The contact detail to create.
     * @return Created contact detail.
     */
    @Transactional
    public CourtContactDetails createContactDetail(UUID courtId, CourtContactDetails request) {
        Court court = courtService.getCourtById(courtId);
        ContactDescriptionType description = getValidatedContactDescription(request.getCourtContactDescriptionId());

        request.setId(null);
        request.setCourtId(courtId);
        request.setCourt(court);
        request.setCourtContactDescription(description);

        log.info("Creating contact detail for court {}", courtId);
        return courtContactDetailsRepository.save(request);
    }

    /**
     * Update an existing contact detail for a court.
     *
     * @param courtId   The court identifier.
     * @param contactId The contact identifier.
     * @param request   Updated contact detail values.
     * @return Updated contact detail.
     */
    @Transactional
    public CourtContactDetails updateContactDetail(UUID courtId, UUID contactId, CourtContactDetails request) {
        CourtContactDetails existing = getContactDetail(courtId, contactId);
        ContactDescriptionType description = getValidatedContactDescription(request.getCourtContactDescriptionId());

        existing.setCourtContactDescriptionId(request.getCourtContactDescriptionId());
        existing.setCourtContactDescription(description);
        existing.setExplanation(request.getExplanation());
        existing.setExplanationCy(request.getExplanationCy());
        existing.setEmail(request.getEmail());
        existing.setPhoneNumber(request.getPhoneNumber());

        log.info("Updating contact detail {} for court {}", contactId, courtId);
        return courtContactDetailsRepository.save(existing);
    }

    /**
     * Remove a contact detail for a court.
     *
     * @param courtId   The court identifier.
     * @param contactId The contact identifier.
     */
    @Transactional
    public void deleteContactDetail(UUID courtId, UUID contactId) {
        courtService.getCourtById(courtId);
        if (!courtContactDetailsRepository.existsByIdAndCourtId(contactId, courtId)) {
            throw new NotFoundException(
                "Court contact detail not found, contactId: " + contactId + ", courtId: " + courtId
            );
        }

        log.info("Deleting contact detail {} for court {}", contactId, courtId);
        courtContactDetailsRepository.deleteByIdAndCourtId(contactId, courtId);
    }

    private ContactDescriptionType getValidatedContactDescription(UUID contactDescriptionId) {
        if (contactDescriptionId == null) {
            return null;
        }

        return contactDescriptionTypeRepository.findById(contactDescriptionId).orElseThrow(
            () -> new NotFoundException("Contact description type not found, ID: " + contactDescriptionId)
        );
    }
}
