package uk.gov.hmcts.reform.fact.data.api.services;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.hmcts.reform.fact.data.api.dto.CourtCodesDto;
import uk.gov.hmcts.reform.fact.data.api.dto.CourtDxCodeDto;
import uk.gov.hmcts.reform.fact.data.api.dto.CourtFaxDto;
import uk.gov.hmcts.reform.fact.data.api.dto.CourtProfessionalInformationDetailsDto;
import uk.gov.hmcts.reform.fact.data.api.dto.ProfessionalInformationDto;
import uk.gov.hmcts.reform.fact.data.api.entities.Court;
import uk.gov.hmcts.reform.fact.data.api.entities.CourtCodes;
import uk.gov.hmcts.reform.fact.data.api.entities.CourtDxCode;
import uk.gov.hmcts.reform.fact.data.api.entities.CourtFax;
import uk.gov.hmcts.reform.fact.data.api.entities.CourtProfessionalInformation;
import uk.gov.hmcts.reform.fact.data.api.errorhandling.exceptions.CourtResourceNotFoundException;
import uk.gov.hmcts.reform.fact.data.api.repositories.CourtCodesRepository;
import uk.gov.hmcts.reform.fact.data.api.repositories.CourtDxCodeRepository;
import uk.gov.hmcts.reform.fact.data.api.repositories.CourtFaxRepository;
import uk.gov.hmcts.reform.fact.data.api.repositories.CourtProfessionalInformationRepository;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Slf4j
public class CourtProfessionalInformationService {

    private final CourtProfessionalInformationRepository courtProfessionalInformationRepository;
    private final CourtCodesRepository courtCodesRepository;
    private final CourtDxCodeRepository courtDxCodeRepository;
    private final CourtFaxRepository courtFaxRepository;
    private final CourtService courtService;

    public CourtProfessionalInformationService(
        CourtProfessionalInformationRepository courtProfessionalInformationRepository,
        CourtCodesRepository courtCodesRepository,
        CourtDxCodeRepository courtDxCodeRepository,
        CourtFaxRepository courtFaxRepository,
        CourtService courtService
    ) {
        this.courtProfessionalInformationRepository = courtProfessionalInformationRepository;
        this.courtCodesRepository = courtCodesRepository;
        this.courtDxCodeRepository = courtDxCodeRepository;
        this.courtFaxRepository = courtFaxRepository;
        this.courtService = courtService;
    }

    /**
     * Get professional information by court id. A court will only ever have zero or one record.
     *
     * @param courtId The court id to find the professional information for.
     * @return A professional information record.
     * @throws CourtResourceNotFoundException if no record exists for the court.
     */
    public CourtProfessionalInformationDetailsDto getProfessionalInformationByCourtId(UUID courtId) {
        courtService.getCourtById(courtId);
        CourtProfessionalInformation professionalInformation =
            courtProfessionalInformationRepository.findByCourtId(courtId)
                .orElseThrow(() -> new CourtResourceNotFoundException(
                    "No professional information found for court id: " + courtId
                ));

        CourtCodes codes = courtCodesRepository.findByCourtId(courtId).orElse(null);
        List<CourtDxCode> dxCodes = courtDxCodeRepository.findAllByCourtId(courtId);
        List<CourtFax> faxNumbers = courtFaxRepository.findAllByCourtId(courtId);

        return buildDetailsDto(professionalInformation, codes, dxCodes, faxNumbers);
    }

    /**
     * Set professional information for a court.
     *
     * @param courtId The id of the court to set professional information for.
     * @param professionalInformationDetails The professional information payload to create or update.
     * @return The saved professional information entity and related resources.
     */
    @Transactional
    public CourtProfessionalInformationDetailsDto setProfessionalInformation(
        UUID courtId,
        CourtProfessionalInformationDetailsDto professionalInformationDetails
    ) {
        log.info("Setting professional information for court id: {}", courtId);
        Court court = courtService.getCourtById(courtId);

        CourtProfessionalInformation savedProfessionalInformation = saveProfessionalInformation(
            courtId,
            court,
            professionalInformationDetails.getProfessionalInformation()
        );

        CourtCodes savedCodes = replaceCourtCodes(courtId, court, professionalInformationDetails.getCodes());
        List<CourtDxCode> savedDxCodes = replaceCourtDxCodes(
            courtId,
            court,
            professionalInformationDetails.getDxCodes()
        );

        List<CourtFax> savedFaxNumbers = replaceCourtFaxNumbers(
            courtId,
            court,
            professionalInformationDetails.getFaxNumbers()
        );

        return buildDetailsDto(savedProfessionalInformation, savedCodes, savedDxCodes, savedFaxNumbers);
    }


    /**
     * Assemble a DTO response from the persisted professional information and related entities.
     *
     * @param professionalInformation The professional information entity.
     * @param codes The court codes entity.
     * @param dxCodes The list of DX code entities.
     * @param faxNumbers The list of fax number entities.
     * @return The assembled DTO.
     */
    private CourtProfessionalInformationDetailsDto buildDetailsDto(
        CourtProfessionalInformation professionalInformation,
        CourtCodes codes,
        List<CourtDxCode> dxCodes,
        List<CourtFax> faxNumbers
    ) {
        List<CourtDxCode> safeDxCodes = Optional.ofNullable(dxCodes).orElseGet(Collections::emptyList);
        List<CourtFax> safeFaxNumbers = Optional.ofNullable(faxNumbers).orElseGet(Collections::emptyList);

        return CourtProfessionalInformationDetailsDto.builder()
            .professionalInformation(ProfessionalInformationDto.fromEntity(professionalInformation))
            .codes(CourtCodesDto.fromEntity(codes))
            .dxCodes(safeDxCodes.stream().map(CourtDxCodeDto::fromEntity).collect(Collectors.toList()))
            .faxNumbers(safeFaxNumbers.stream().map(CourtFaxDto::fromEntity).collect(Collectors.toList()))
            .build();
    }

    /**
     * Create or update the core professional information entity for a court.
     *
     * @param courtId The court id.
     * @param court The court.
     * @param professionalInformationDto The professional information data.
     * @return The saved professional information entity.
     */
    private CourtProfessionalInformation saveProfessionalInformation(
        UUID courtId,
        Court court,
        ProfessionalInformationDto professionalInformationDto
    ) {
        CourtProfessionalInformation entity = CourtProfessionalInformation.builder()
            .interviewRooms(professionalInformationDto.getInterviewRooms())
            .interviewRoomCount(professionalInformationDto.getInterviewRoomCount())
            .interviewPhoneNumber(professionalInformationDto.getInterviewPhoneNumber())
            .videoHearings(professionalInformationDto.getVideoHearings())
            .commonPlatform(professionalInformationDto.getCommonPlatform())
            .accessScheme(professionalInformationDto.getAccessScheme())
            .courtId(courtId)
            .build();

        entity.setCourt(court);

        courtProfessionalInformationRepository.findByCourtId(courtId).ifPresent(existing ->
            entity.setId(existing.getId())
        );

        return courtProfessionalInformationRepository.save(entity);
    }

    /**
     * Replace existing CourtCodes for the court with the provided values.
     *
     * @param courtId The id of the court.
     * @param court The court.
     * @param codes The court codes to set.
     * @return The saved court codes.
     */
    private CourtCodes replaceCourtCodes(UUID courtId, Court court, CourtCodesDto codes) {
        if (codes == null) {
            // No codes provided; remove any existing record for this court.
            courtCodesRepository.deleteByCourtId(courtId);
            return null;
        }

        CourtCodes entity = CourtCodes.builder()
            .courtId(courtId)
            .magistrateCourtCode(codes.getMagistrateCourtCode())
            .familyCourtCode(codes.getFamilyCourtCode())
            .tribunalCode(codes.getTribunalCode())
            .countyCourtCode(codes.getCountyCourtCode())
            .crownCourtCode(codes.getCrownCourtCode())
            .gbs(StringUtils.trimToNull(codes.getGbs()))
            .build();

        entity.setCourt(court);

        courtCodesRepository.findByCourtId(courtId).ifPresent(existing ->
            entity.setId(existing.getId())
        );

        return courtCodesRepository.save(entity);
    }

    /**
     * Replace existing DX code entries for the court with the provided list.
     *
     * @param courtId The id of the court.
     * @param court The court.
     * @param dxCodes The DX codes to set.
     * @return The saved DX codes.
     */
    private List<CourtDxCode> replaceCourtDxCodes(UUID courtId, Court court, List<CourtDxCodeDto> dxCodes) {
        courtDxCodeRepository.deleteAllByCourtId(courtId);

        List<CourtDxCodeDto> safeDxCodes = Optional.ofNullable(dxCodes).orElse(Collections.emptyList());
        if (safeDxCodes.isEmpty()) {
            return Collections.emptyList();
        }

        List<CourtDxCode> toPersist = safeDxCodes.stream()
            .filter(Objects::nonNull)
            .map(code -> {
                CourtDxCode courtDxCode = CourtDxCode.builder()
                    .dxCode(StringUtils.trimToNull(code.getDxCode()))
                    .explanation(StringUtils.trimToNull(code.getExplanation()))
                    .courtId(courtId)
                    .build();
                courtDxCode.setCourt(court);
                return courtDxCode;
            })
            .collect(Collectors.toList());

        if (toPersist.isEmpty()) {
            return Collections.emptyList();
        }

        return courtDxCodeRepository.saveAll(toPersist);
    }

    /**
     * Replace existing fax entries for the court with the provided list.
     *
     * @param courtId The id of the court.
     * @param court The court.
     * @param faxNumbers The fax numbers to set.
     * @return The saved fax numbers.
     */
    private List<CourtFax> replaceCourtFaxNumbers(UUID courtId, Court court, List<CourtFaxDto> faxNumbers) {
        courtFaxRepository.deleteAllByCourtId(courtId);

        List<CourtFaxDto> safeFaxNumbers = Optional.ofNullable(faxNumbers).orElse(Collections.emptyList());
        if (safeFaxNumbers.isEmpty()) {
            return Collections.emptyList();
        }

        List<CourtFax> toPersist = safeFaxNumbers.stream()
            .filter(Objects::nonNull)
            .map(fax -> {
                CourtFax courtFax = CourtFax.builder()
                    .faxNumber(fax.getFaxNumber())
                    .description(StringUtils.trimToNull(fax.getDescription()))
                    .courtId(courtId)
                    .build();
                courtFax.setCourt(court);
                return courtFax;
            })
            .collect(Collectors.toList());

        if (toPersist.isEmpty()) {
            return Collections.emptyList();
        }

        return courtFaxRepository.saveAll(toPersist);
    }
}
