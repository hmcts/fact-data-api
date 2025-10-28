package uk.gov.hmcts.reform.fact.data.api.services;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
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
import uk.gov.hmcts.reform.fact.data.api.errorhandling.exceptions.NotFoundException;
import uk.gov.hmcts.reform.fact.data.api.repositories.CourtCodesRepository;
import uk.gov.hmcts.reform.fact.data.api.repositories.CourtDxCodeRepository;
import uk.gov.hmcts.reform.fact.data.api.repositories.CourtFaxRepository;
import uk.gov.hmcts.reform.fact.data.api.repositories.CourtProfessionalInformationRepository;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CourtProfessionalInformationServiceTest {

    @Mock
    private CourtProfessionalInformationRepository courtProfessionalInformationRepository;

    @Mock
    private CourtCodesRepository courtCodesRepository;

    @Mock
    private CourtDxCodeRepository courtDxCodeRepository;

    @Mock
    private CourtFaxRepository courtFaxRepository;

    @Mock
    private CourtService courtService;

    @InjectMocks
    private CourtProfessionalInformationService courtProfessionalInformationService;

    private UUID courtId;
    private Court court;
    private CourtProfessionalInformation professionalInformation;
    private CourtCodes courtCodes;
    private List<CourtDxCode> courtDxCodes;
    private List<CourtFax> courtFaxes;
    private ProfessionalInformationDto professionalInformationDto;
    private CourtCodesDto courtCodesDto;
    private List<CourtDxCodeDto> courtDxCodeDtos;
    private List<CourtFaxDto> courtFaxDtos;
    private CourtProfessionalInformationDetailsDto requestDetails;

    @Captor
    private ArgumentCaptor<CourtProfessionalInformation> professionalInformationCaptor;

    @Captor
    private ArgumentCaptor<CourtCodes> courtCodesCaptor;

    @Captor
    private ArgumentCaptor<List<CourtDxCode>> courtDxCodesCaptor;

    @Captor
    private ArgumentCaptor<List<CourtFax>> courtFaxCaptor;

    @BeforeEach
    void setup() {
        courtId = UUID.randomUUID();
        court = new Court();
        court.setId(courtId);
        court.setName("Test Court");

        professionalInformation = CourtProfessionalInformation.builder()
            .courtId(courtId)
            .interviewRooms(true)
            .interviewRoomCount(2)
            .interviewPhoneNumber("01234567890")
            .videoHearings(true)
            .commonPlatform(true)
            .accessScheme(true)
            .build();

        courtCodes = CourtCodes.builder()
            .courtId(courtId)
            .magistrateCourtCode(123)
            .gbs("456")
            .build();

        courtDxCodes = List.of(CourtDxCode.builder()
            .courtId(courtId)
            .dxCode("789")
            .explanation("DX explanation")
            .build());

        courtFaxes = List.of(CourtFax.builder()
            .courtId(courtId)
            .faxNumber("01234567890")
            .description("Fax description")
            .build());

        professionalInformationDto = ProfessionalInformationDto.builder()
            .interviewRooms(professionalInformation.getInterviewRooms())
            .interviewRoomCount(professionalInformation.getInterviewRoomCount())
            .interviewPhoneNumber(professionalInformation.getInterviewPhoneNumber())
            .videoHearings(professionalInformation.getVideoHearings())
            .commonPlatform(professionalInformation.getCommonPlatform())
            .accessScheme(professionalInformation.getAccessScheme())
            .build();

        courtCodesDto = CourtCodesDto.builder()
            .magistrateCourtCode(courtCodes.getMagistrateCourtCode())
            .familyCourtCode(courtCodes.getFamilyCourtCode())
            .tribunalCode(courtCodes.getTribunalCode())
            .countyCourtCode(courtCodes.getCountyCourtCode())
            .crownCourtCode(courtCodes.getCrownCourtCode())
            .gbs(courtCodes.getGbs())
            .build();

        courtDxCodeDtos = List.of(CourtDxCodeDto.builder()
            .dxCode(courtDxCodes.get(0).getDxCode())
            .explanation(courtDxCodes.get(0).getExplanation())
            .build());

        courtFaxDtos = List.of(CourtFaxDto.builder()
            .faxNumber(courtFaxes.get(0).getFaxNumber())
            .description(courtFaxes.get(0).getDescription())
            .build());

        requestDetails = CourtProfessionalInformationDetailsDto.builder()
            .professionalInformation(professionalInformationDto)
            .codes(courtCodesDto)
            .dxCodes(courtDxCodeDtos)
            .faxNumbers(courtFaxDtos)
            .build();
    }

    @Test
    void getProfessionalInformationByCourtIdReturnsRecordWhenFound() {
        when(courtService.getCourtById(courtId)).thenReturn(court);
        when(courtProfessionalInformationRepository.findByCourtId(courtId))
            .thenReturn(Optional.of(professionalInformation));
        when(courtCodesRepository.findByCourtId(courtId)).thenReturn(Optional.of(courtCodes));
        when(courtDxCodeRepository.findAllByCourtId(courtId)).thenReturn(courtDxCodes);
        when(courtFaxRepository.findAllByCourtId(courtId)).thenReturn(courtFaxes);

        CourtProfessionalInformationDetailsDto result = courtProfessionalInformationService
            .getProfessionalInformationByCourtId(courtId);

        assertThat(result.getProfessionalInformation()).isEqualTo(professionalInformationDto);
        assertThat(result.getCodes()).isEqualTo(courtCodesDto);
        assertThat(result.getDxCodes()).containsExactlyElementsOf(courtDxCodeDtos);
        assertThat(result.getFaxNumbers()).containsExactlyElementsOf(courtFaxDtos);
    }

    @Test
    void getProfessionalInformationByCourtIdThrowsCourtResourceNotFoundWhenAbsent() {
        when(courtService.getCourtById(courtId)).thenReturn(court);
        when(courtProfessionalInformationRepository.findByCourtId(courtId)).thenReturn(Optional.empty());

        CourtResourceNotFoundException exception = assertThrows(
            CourtResourceNotFoundException.class,
            () -> courtProfessionalInformationService.getProfessionalInformationByCourtId(courtId)
        );

        assertThat(exception.getMessage()).contains(courtId.toString());
    }

    @Test
    void getProfessionalInformationByCourtIdThrowsNotFoundWhenCourtMissing() {
        when(courtService.getCourtById(courtId)).thenThrow(new NotFoundException("Court not found"));

        assertThrows(
            NotFoundException.class,
            () -> courtProfessionalInformationService.getProfessionalInformationByCourtId(courtId)
        );
    }

    @Test
    void setProfessionalInformationCreatesNewRecordWhenNoneExists() {
        when(courtService.getCourtById(courtId)).thenReturn(court);
        when(courtProfessionalInformationRepository.findByCourtId(courtId)).thenReturn(Optional.empty());
        when(courtProfessionalInformationRepository.save(any(CourtProfessionalInformation.class)))
            .thenReturn(professionalInformation);
        when(courtCodesRepository.findByCourtId(courtId)).thenReturn(Optional.empty());
        when(courtCodesRepository.save(any(CourtCodes.class))).thenReturn(courtCodes);
        when(courtDxCodeRepository.saveAll(any())).thenReturn(courtDxCodes);
        when(courtFaxRepository.saveAll(any())).thenReturn(courtFaxes);

        CourtProfessionalInformationDetailsDto result =
            courtProfessionalInformationService.setProfessionalInformation(courtId, requestDetails);

        assertThat(result.getProfessionalInformation()).isEqualTo(professionalInformationDto);
        assertThat(result.getCodes()).isEqualTo(courtCodesDto);
        assertThat(result.getDxCodes()).containsExactlyElementsOf(courtDxCodeDtos);
        assertThat(result.getFaxNumbers()).containsExactlyElementsOf(courtFaxDtos);
        verify(courtProfessionalInformationRepository).save(any(CourtProfessionalInformation.class));
    }

    @Test
    void setProfessionalInformationUpdatesExistingRecord() {
        CourtProfessionalInformation existing = CourtProfessionalInformation.builder()
            .id(UUID.randomUUID())
            .courtId(courtId)
            .interviewRooms(false)
            .videoHearings(false)
            .commonPlatform(false)
            .accessScheme(false)
            .build();

        when(courtService.getCourtById(courtId)).thenReturn(court);
        when(courtProfessionalInformationRepository.findByCourtId(courtId)).thenReturn(Optional.of(existing));
        when(courtProfessionalInformationRepository.save(any(CourtProfessionalInformation.class)))
            .thenReturn(professionalInformation);
        when(courtCodesRepository.findByCourtId(courtId)).thenReturn(Optional.of(courtCodes));
        when(courtCodesRepository.save(any(CourtCodes.class))).thenReturn(courtCodes);
        when(courtDxCodeRepository.saveAll(any())).thenReturn(courtDxCodes);
        when(courtFaxRepository.saveAll(any())).thenReturn(courtFaxes);

        CourtProfessionalInformationDetailsDto result =
            courtProfessionalInformationService.setProfessionalInformation(courtId, requestDetails);

        assertThat(result.getProfessionalInformation()).isEqualTo(professionalInformationDto);
    }

    @Test
    void setProfessionalInformationThrowsNotFoundWhenCourtMissing() {
        when(courtService.getCourtById(courtId)).thenThrow(new NotFoundException("Court not found"));

        CourtProfessionalInformationDetailsDto request = CourtProfessionalInformationDetailsDto.builder()
            .professionalInformation(professionalInformationDto)
            .build();

        assertThrows(
            NotFoundException.class,
            () -> courtProfessionalInformationService.setProfessionalInformation(courtId, request)
        );
    }

    @Test
    void setProfessionalInformationTrimsAndNormalisesValues() {
        when(courtService.getCourtById(courtId)).thenReturn(court);
        when(courtProfessionalInformationRepository.findByCourtId(courtId)).thenReturn(Optional.empty());
        when(courtProfessionalInformationRepository.save(any(CourtProfessionalInformation.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));
        when(courtCodesRepository.findByCourtId(courtId)).thenReturn(Optional.empty());
        when(courtCodesRepository.save(any(CourtCodes.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(courtDxCodeRepository.saveAll(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(courtFaxRepository.saveAll(any())).thenAnswer(invocation -> invocation.getArgument(0));

        CourtProfessionalInformationDetailsDto request = CourtProfessionalInformationDetailsDto.builder()
            .professionalInformation(ProfessionalInformationDto.builder()
                .interviewRooms(true)
                .interviewRoomCount(10)
                .interviewPhoneNumber(" 0207 123 4567 ")
                .videoHearings(true)
                .commonPlatform(true)
                .accessScheme(true)
                .build())
            .codes(CourtCodesDto.builder()
                .magistrateCourtCode(123456)
                .gbs("  GBS001  ")
                .build())
            .dxCodes(List.of(CourtDxCodeDto.builder()
                .dxCode(" 120551 Marylebone 9 ")
                .explanation(" Main DX entry ")
                .build()))
            .faxNumbers(List.of(CourtFaxDto.builder()
                .faxNumber("0207 000 0000")
                .description(" Fax description  ")
                .build()))
            .build();

        CourtProfessionalInformationDetailsDto result =
            courtProfessionalInformationService.setProfessionalInformation(courtId, request);

        verify(courtProfessionalInformationRepository).save(professionalInformationCaptor.capture());
        verify(courtCodesRepository).save(courtCodesCaptor.capture());
        verify(courtDxCodeRepository).saveAll(courtDxCodesCaptor.capture());
        verify(courtFaxRepository).saveAll(courtFaxCaptor.capture());

        assertThat(courtCodesCaptor.getValue().getGbs()).isEqualTo("GBS001");
        assertThat(courtDxCodesCaptor.getValue()).hasSize(1);
        assertThat(courtDxCodesCaptor.getValue().get(0).getDxCode()).isEqualTo("120551 Marylebone 9");
        assertThat(courtDxCodesCaptor.getValue().get(0).getExplanation()).isEqualTo("Main DX entry");
        assertThat(courtFaxCaptor.getValue()).hasSize(1);
        assertThat(courtFaxCaptor.getValue().get(0).getDescription()).isEqualTo("Fax description");

        assertThat(result.getCodes().getGbs()).isEqualTo("GBS001");
        assertThat(result.getDxCodes()).extracting(CourtDxCodeDto::getDxCode)
            .containsExactly("120551 Marylebone 9");
        assertThat(result.getFaxNumbers()).extracting(CourtFaxDto::getDescription)
            .containsExactly("Fax description");
    }

    @Test
    void setProfessionalInformationClearsExistingRelatedDataWhenAbsent() {
        when(courtService.getCourtById(courtId)).thenReturn(court);
        when(courtProfessionalInformationRepository.findByCourtId(courtId))
            .thenReturn(Optional.of(professionalInformation));
        when(courtProfessionalInformationRepository.save(any(CourtProfessionalInformation.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));
        when(courtCodesRepository.findByCourtId(courtId)).thenReturn(Optional.of(courtCodes));

        CourtProfessionalInformationDetailsDto request = CourtProfessionalInformationDetailsDto.builder()
            .professionalInformation(professionalInformationDto)
            .dxCodes(Collections.emptyList())
            .faxNumbers(Collections.emptyList())
            .build();

        CourtProfessionalInformationDetailsDto result =
            courtProfessionalInformationService.setProfessionalInformation(courtId, request);

        verify(courtCodesRepository).delete(courtCodes);
        verify(courtDxCodeRepository).deleteAllByCourtId(courtId);
        verify(courtFaxRepository).deleteAllByCourtId(courtId);
        verify(courtDxCodeRepository, never()).saveAll(any());
        verify(courtFaxRepository, never()).saveAll(any());

        assertThat(result.getCodes()).isNull();
        assertThat(result.getDxCodes()).isEmpty();
        assertThat(result.getFaxNumbers()).isEmpty();
    }
}
