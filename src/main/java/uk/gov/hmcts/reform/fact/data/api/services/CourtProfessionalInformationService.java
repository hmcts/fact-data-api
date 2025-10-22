package uk.gov.hmcts.reform.fact.data.api.services;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.fact.data.api.entities.Court;
import uk.gov.hmcts.reform.fact.data.api.entities.CourtProfessionalInformation;
import uk.gov.hmcts.reform.fact.data.api.errorhandling.exceptions.ProfessionalInformationNotFoundException;
import uk.gov.hmcts.reform.fact.data.api.repositories.CourtProfessionalInformationRepository;

import java.util.UUID;

@Service
@Slf4j
public class CourtProfessionalInformationService {

    private final CourtProfessionalInformationRepository professionalInformationRepository;
    private final CourtService courtService;

    public CourtProfessionalInformationService(
        CourtProfessionalInformationRepository professionalInformationRepository,
        CourtService courtService
    ) {
        this.professionalInformationRepository = professionalInformationRepository;
        this.courtService = courtService;
    }

    /**
     * Returns the professional information for the supplied court.
     *
     * @param courtId the ID of the court to find professional information for.
     * @return professional information for the supplied court.
     * @throws ProfessionalInformationNotFoundException if no professional information exists for the court.
     */
    public CourtProfessionalInformation getProfessionalInformation(UUID courtId) {
        courtService.getCourtById(courtId);
        return professionalInformationRepository.findByCourtId(courtId)
            .orElseThrow(() -> new ProfessionalInformationNotFoundException(
                "No professional information found for court id: " + courtId
            ));
    }

    /**
     * Creates or updates professional information for the supplied court.
     *
     * @param courtId the ID of the court to update.
     * @param professionalInformation the payload to persist.
     * @return the persisted professional information.
     */
    public CourtProfessionalInformation setProfessionalInformation(
        UUID courtId,
        CourtProfessionalInformation professionalInformation
    ) {
        log.info("Setting professional information for court id: {}", courtId);
        Court court = courtService.getCourtById(courtId);
        professionalInformation.setCourt(court);
        professionalInformation.setCourtId(courtId);

        professionalInformationRepository.findByCourtId(courtId).ifPresent(existing ->
            professionalInformation.setId(existing.getId())
        );

        return professionalInformationRepository.save(professionalInformation);
    }
}
