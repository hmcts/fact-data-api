package uk.gov.hmcts.reform.fact.data.api.services;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

import lombok.extern.slf4j.Slf4j;
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
        ProfessionalInformationDto professionalInformationDto =
            professionalInformationDetails.getProfessionalInformation();
        CourtProfessionalInformation professionalInformation =
            toProfessionalInformationEntity(courtId, court, professionalInformationDto);
        courtProfessionalInformationRepository.findByCourtId(courtId).ifPresent(existing ->
            professionalInformation.setId(existing.getId())
        );
        CourtProfessionalInformation savedProfessionalInformation =
            courtProfessionalInformationRepository.save(professionalInformation);

        CourtCodes savedCodes = upsertCourtCodes(courtId, court, professionalInformationDetails.getCodes());
        List<CourtDxCode> savedDxCodes =
            replaceCourtDxCodes(courtId, court, professionalInformationDetails.getDxCodes());
        List<CourtFax> savedFaxNumbers =
            replaceCourtFaxNumbers(courtId, court, professionalInformationDetails.getFaxNumbers());

        return buildDetailsDto(savedProfessionalInformation, savedCodes, savedDxCodes, savedFaxNumbers);
    }

    private CourtProfessionalInformationDetailsDto buildDetailsDto(
        CourtProfessionalInformation professionalInformation,
        CourtCodes codes,
        List<CourtDxCode> dxCodes,
        List<CourtFax> faxNumbers
    ) {
        List<CourtDxCode> safeDxCodes = dxCodes == null ? Collections.emptyList() : dxCodes;
        List<CourtFax> safeFaxNumbers = faxNumbers == null ? Collections.emptyList() : faxNumbers;

        return CourtProfessionalInformationDetailsDto.builder()
            .professionalInformation(toProfessionalInformationDto(professionalInformation))
            .codes(toCodesDto(codes))
            .dxCodes(safeDxCodes.stream().map(this::toDxCodeDto).collect(Collectors.toList()))
            .faxNumbers(safeFaxNumbers.stream().map(this::toFaxDto).collect(Collectors.toList()))
            .build();
    }

    private CourtProfessionalInformation toProfessionalInformationEntity(
        UUID courtId,
        Court court,
        ProfessionalInformationDto dto
    ) {
        CourtProfessionalInformation entity = CourtProfessionalInformation.builder()
            .interviewRooms(dto.getInterviewRooms())
            .interviewRoomCount(dto.getInterviewRoomCount())
            .interviewPhoneNumber(dto.getInterviewPhoneNumber())
            .videoHearings(dto.getVideoHearings())
            .commonPlatform(dto.getCommonPlatform())
            .accessScheme(dto.getAccessScheme())
            .courtId(courtId)
            .build();
        entity.setCourt(court);
        return entity;
    }

    private ProfessionalInformationDto toProfessionalInformationDto(CourtProfessionalInformation entity) {
        return ProfessionalInformationDto.builder()
            .interviewRooms(entity.getInterviewRooms())
            .interviewRoomCount(entity.getInterviewRoomCount())
            .interviewPhoneNumber(entity.getInterviewPhoneNumber())
            .videoHearings(entity.getVideoHearings())
            .commonPlatform(entity.getCommonPlatform())
            .accessScheme(entity.getAccessScheme())
            .build();
    }

    private CourtCodes upsertCourtCodes(UUID courtId, Court court, CourtCodesDto codes) {
        if (codes == null) {
            courtCodesRepository.findByCourtId(courtId).ifPresent(courtCodesRepository::delete);
            return null;
        }
        CourtCodes entity = CourtCodes.builder()
            .courtId(courtId)
            .magistrateCourtCode(codes.getMagistrateCourtCode())
            .familyCourtCode(codes.getFamilyCourtCode())
            .tribunalCode(codes.getTribunalCode())
            .countyCourtCode(codes.getCountyCourtCode())
            .crownCourtCode(codes.getCrownCourtCode())
            .gbs(trimToNull(codes.getGbs()))
            .build();
        entity.setCourt(court);
        courtCodesRepository.findByCourtId(courtId).ifPresent(existing ->
            entity.setId(existing.getId())
        );
        return courtCodesRepository.save(entity);
    }

    private List<CourtDxCode> replaceCourtDxCodes(UUID courtId, Court court, List<CourtDxCodeDto> dxCodes) {
        courtDxCodeRepository.deleteAllByCourtId(courtId);
        if (dxCodes == null || dxCodes.isEmpty()) {
            return Collections.emptyList();
        }
        List<CourtDxCode> toPersist = dxCodes.stream()
            .filter(Objects::nonNull)
            .map(code -> {
                CourtDxCode courtDxCode = CourtDxCode.builder()
                    .dxCode(trimToNull(code.getDxCode()))
                    .explanation(trimToNull(code.getExplanation()))
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

    private List<CourtFax> replaceCourtFaxNumbers(UUID courtId, Court court, List<CourtFaxDto> faxNumbers) {
        courtFaxRepository.deleteAllByCourtId(courtId);
        if (faxNumbers == null || faxNumbers.isEmpty()) {
            return Collections.emptyList();
        }
        List<CourtFax> toPersist = faxNumbers.stream()
            .filter(Objects::nonNull)
            .map(fax -> {
                CourtFax courtFax = CourtFax.builder()
                    .faxNumber(fax.getFaxNumber())
                    .description(trimToNull(fax.getDescription()))
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

    private CourtCodesDto toCodesDto(CourtCodes codes) {
        if (codes == null) {
            return null;
        }
        return CourtCodesDto.builder()
            .magistrateCourtCode(codes.getMagistrateCourtCode())
            .familyCourtCode(codes.getFamilyCourtCode())
            .tribunalCode(codes.getTribunalCode())
            .countyCourtCode(codes.getCountyCourtCode())
            .crownCourtCode(codes.getCrownCourtCode())
            .gbs(codes.getGbs())
            .build();
    }

    private CourtDxCodeDto toDxCodeDto(CourtDxCode dxCode) {
        return CourtDxCodeDto.builder()
            .dxCode(dxCode.getDxCode())
            .explanation(dxCode.getExplanation())
            .build();
    }

    private CourtFaxDto toFaxDto(CourtFax fax) {
        return CourtFaxDto.builder()
            .faxNumber(fax.getFaxNumber())
            .description(fax.getDescription())
            .build();
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
