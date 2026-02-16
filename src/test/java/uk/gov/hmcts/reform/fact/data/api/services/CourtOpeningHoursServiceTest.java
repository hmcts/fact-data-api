package uk.gov.hmcts.reform.fact.data.api.services;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.fact.data.api.entities.Court;
import uk.gov.hmcts.reform.fact.data.api.entities.CourtOpeningHours;
import uk.gov.hmcts.reform.fact.data.api.entities.OpeningHourType;
import uk.gov.hmcts.reform.fact.data.api.entities.CourtCounterServiceOpeningHours;
import uk.gov.hmcts.reform.fact.data.api.entities.types.DayOfTheWeek;
import uk.gov.hmcts.reform.fact.data.api.entities.types.OpeningTimesDetail;
import uk.gov.hmcts.reform.fact.data.api.errorhandling.exceptions.CourtResourceNotFoundException;
import uk.gov.hmcts.reform.fact.data.api.errorhandling.exceptions.NotFoundException;
import uk.gov.hmcts.reform.fact.data.api.repositories.CourtCounterServiceOpeningHoursRepository;
import uk.gov.hmcts.reform.fact.data.api.repositories.CourtOpeningHoursRepository;

import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CourtOpeningHoursServiceTest {

    @Mock
    private CourtOpeningHoursRepository courtOpeningHoursRepository;

    @Mock
    private CourtCounterServiceOpeningHoursRepository courtCounterServiceOpeningHoursRepository;

    @Mock
    private CourtService courtService;

    @Mock
    private OpeningHoursTypeService openingHoursTypeService;

    @InjectMocks
    private CourtOpeningHoursService courtOpeningHoursService;

    private UUID courtId;
    private Court court;
    private CourtOpeningHours openingHours;
    private List<CourtCounterServiceOpeningHours> counterServiceOpeningHours;
    private UUID openingHourTypeId;
    private OpeningHourType openingHourType;

    private static final String COURT_NOT_FOUND_MESSAGE = "Court not found";
    private static final String OPENING_HOUR_TYPE_NOT_FOUND_MESSAGE = "Opening hour type not found";

    @BeforeEach
    void setup() {
        openingHourTypeId = UUID.randomUUID();
        openingHourType = new OpeningHourType();
        openingHourType.setId(openingHourTypeId);
        openingHourType.setName("name");

        courtId = UUID.randomUUID();
        court = new Court();
        court.setId(courtId);
        court.setName("Test Court");

        openingHours =
            CourtOpeningHours.builder()
                .id(UUID.randomUUID())
                .courtId(courtId)
                .openingTimesDetails(List.of(
                    OpeningTimesDetail.builder()
                        .dayOfWeek(DayOfTheWeek.MONDAY)
                        .openingTime(LocalTime.of(9, 0))
                        .closingTime(LocalTime.of(17, 0))
                        .build(),
                    OpeningTimesDetail.builder()
                        .dayOfWeek(DayOfTheWeek.TUESDAY)
                        .openingTime(LocalTime.of(9, 0))
                        .closingTime(LocalTime.of(17, 0))
                        .build()
                ))
                .build();

        counterServiceOpeningHours = List.of(
            CourtCounterServiceOpeningHours.builder()
                .id(UUID.randomUUID())
                .courtId(courtId)
                .dayOfWeek(DayOfTheWeek.MONDAY)
                .openingHour(LocalTime.of(9, 0, 0))
                .closingHour(LocalTime.of(17, 0, 0))
                .appointmentContact("Test Contact")
                .assistWithForms(true)
                .counterService(true)
                .assistWithDocuments(true)
                .assistWithSupport(true)
                .appointmentNeeded(false)
                .build()
        );
    }

    @Test
    void getOpeningHoursByCourtIdReturnsOpeningHoursWhenFound() {
        when(courtService.getCourtById(courtId)).thenReturn(court);
        when(courtOpeningHoursRepository.findByCourtId(courtId)).thenReturn(Optional.of(List.of(openingHours)));

        List<CourtOpeningHours> result = courtOpeningHoursService.getOpeningHoursByCourtId(courtId);

        assertThat(result).isEqualTo(List.of(openingHours));
    }

    @Test
    void getOpeningHoursByCourtIdThrowsExceptionWhenNotFound() {
        when(courtService.getCourtById(courtId)).thenReturn(court);
        when(courtOpeningHoursRepository.findByCourtId(courtId)).thenReturn(Optional.empty());

        assertThrows(
            CourtResourceNotFoundException.class,
            () -> courtOpeningHoursService.getOpeningHoursByCourtId(courtId)
        );
    }

    @Test
    void getOpeningHoursThrowsExceptionWhenCourtDoesNotExist() {
        when(courtService.getCourtById(courtId)).thenThrow(new NotFoundException(COURT_NOT_FOUND_MESSAGE));

        assertThrows(NotFoundException.class, () ->
            courtOpeningHoursService.getOpeningHoursByCourtId(courtId)
        );
    }

    @Test
    void getOpeningHoursByTypeIdReturnsOpeningHoursWhenFound() {
        when(courtService.getCourtById(courtId)).thenReturn(court);
        when(openingHoursTypeService.getOpeningHourTypeById(openingHourType.getId())).thenReturn(openingHourType);
        when(courtOpeningHoursRepository.findByCourtIdAndOpeningHourTypeId(courtId, openingHourType.getId()))
            .thenReturn(Optional.of(openingHours));

        CourtOpeningHours result = courtOpeningHoursService
            .getOpeningHoursByTypeId(courtId, openingHourType.getId());

        assertThat(result).isEqualTo(openingHours);
    }

    @Test
    void getOpeningHoursByTypeIdThrowsExceptionWhenNotFound() {
        when(courtService.getCourtById(courtId)).thenReturn(court);
        when(openingHoursTypeService.getOpeningHourTypeById(openingHourType.getId())).thenReturn(openingHourType);
        when(courtOpeningHoursRepository.findByCourtIdAndOpeningHourTypeId(courtId, openingHourType.getId()))
            .thenReturn(Optional.empty());

        assertThrows(
            CourtResourceNotFoundException.class,
            () -> courtOpeningHoursService.getOpeningHoursByTypeId(courtId, openingHourType.getId())
        );
    }

    @Test
    void getOpeningHoursByTypeIdThrowsExceptionWhenCourtDoesNotExist() {
        when(courtService.getCourtById(courtId)).thenThrow(new NotFoundException(COURT_NOT_FOUND_MESSAGE));

        assertThrows(NotFoundException.class, () ->
            courtOpeningHoursService.getOpeningHoursByTypeId(courtId, openingHourType.getId())
        );
    }

    @Test
    void getOpeningHoursThrowsExceptionWhenOpeningHourTypeDoesNotExist() {
        UUID typeId = UUID.randomUUID();
        when(courtService.getCourtById(courtId)).thenReturn(court);
        when(openingHoursTypeService.getOpeningHourTypeById(typeId))
            .thenThrow(new NotFoundException(OPENING_HOUR_TYPE_NOT_FOUND_MESSAGE));

        assertThrows(
            NotFoundException.class, () ->
                courtOpeningHoursService.getOpeningHoursByTypeId(courtId, typeId)
        );
    }

    @Test
    void getCounterServiceOpeningHoursByCourtIdReturnsOpeningHoursWhenFound() {
        List<CourtCounterServiceOpeningHours> counterHours = List.of(new CourtCounterServiceOpeningHours());
        when(courtService.getCourtById(courtId)).thenReturn(court);
        when(courtCounterServiceOpeningHoursRepository.findByCourtId(courtId)).thenReturn(Optional.of(counterHours));

        List<CourtCounterServiceOpeningHours> result =
            courtOpeningHoursService.getCounterServiceOpeningHoursByCourtId(courtId);

        assertThat(result).isEqualTo(counterHours);
    }

    @Test
    void getCounterServiceOpeningHoursByCourtIdThrowsExceptionWhenNotFound() {
        when(courtService.getCourtById(courtId)).thenReturn(court);
        when(courtCounterServiceOpeningHoursRepository.findByCourtId(courtId)).thenReturn(Optional.empty());

        assertThrows(
            CourtResourceNotFoundException.class,
            () -> courtOpeningHoursService.getCounterServiceOpeningHoursByCourtId(courtId)
        );
    }

    @Test
    void getCounterServiceOpeningHoursThrowsExceptionWhenCourtDoesNotExist() {
        when(courtService.getCourtById(courtId)).thenThrow(new NotFoundException(COURT_NOT_FOUND_MESSAGE));

        assertThrows(NotFoundException.class, () ->
            courtOpeningHoursService.getCounterServiceOpeningHoursByCourtId(courtId)
        );
    }

    @Test
    void setOpeningHoursSuccessfullyCreatesNewOpeningHours() {
        when(courtService.getCourtById(courtId)).thenReturn(court);
        when(openingHoursTypeService.getOpeningHourTypeById(openingHourType.getId())).thenReturn(openingHourType);
        when(courtOpeningHoursRepository.save(any())).thenReturn(openingHours);

        CourtOpeningHours result = courtOpeningHoursService
            .setOpeningHours(courtId, openingHourType.getId(), openingHours);

        assertThat(result).isEqualTo(openingHours);
        verify(courtOpeningHoursRepository).deleteByCourtIdAndOpeningHourTypeId(courtId, openingHourType.getId());
        verify(courtOpeningHoursRepository).save(openingHours);
    }

    @Test
    void setOpeningHoursRemovesOtherDaysWhenEverydayPresent() {

        CourtOpeningHours hours =
            CourtOpeningHours.builder()
                .id(UUID.randomUUID())
                .courtId(courtId)
                .openingHourTypeId(openingHourTypeId)
                .openingTimesDetails(List.of(
                    OpeningTimesDetail.builder()
                        .dayOfWeek(DayOfTheWeek.MONDAY)
                        .openingTime(LocalTime.of(9, 0))
                        .closingTime(LocalTime.of(17, 0))
                        .build(),
                    OpeningTimesDetail.builder()
                        .dayOfWeek(DayOfTheWeek.EVERYDAY)
                        .openingTime(LocalTime.of(9, 0))
                        .closingTime(LocalTime.of(17, 0))
                        .build()
                ))
                .build();

        CourtOpeningHours expectedHours =
            CourtOpeningHours.builder()
                .id(UUID.randomUUID())
                .courtId(courtId)
                .openingHourTypeId(openingHourTypeId)
                .openingTimesDetails(List.of(
                    OpeningTimesDetail.builder()
                        .dayOfWeek(DayOfTheWeek.EVERYDAY)
                        .openingTime(LocalTime.of(9, 0))
                        .closingTime(LocalTime.of(17, 0))
                        .build()
                ))
                .build();

        when(courtService.getCourtById(courtId)).thenReturn(court);
        when(openingHoursTypeService.getOpeningHourTypeById(openingHourType.getId())).thenReturn(openingHourType);
        when(courtOpeningHoursRepository.save(any())).thenReturn(expectedHours);

        CourtOpeningHours result =
            courtOpeningHoursService.setOpeningHours(courtId, openingHourTypeId, hours);

        assertThat(result).isEqualTo(expectedHours);
        verify(courtOpeningHoursRepository).deleteByCourtIdAndOpeningHourTypeId(courtId, openingHourType.getId());
        verify(courtOpeningHoursRepository).save(any());
    }

    @Test
    void setOpeningHoursSuccessfullyUpdatesExistingOpeningHours() {
        CourtOpeningHours updatedHours =
            CourtOpeningHours.builder()
                .id(UUID.randomUUID())
                .courtId(courtId)
                .openingHourTypeId(openingHourType.getId())
                .openingTimesDetails(List.of(
                    OpeningTimesDetail.builder()
                        .dayOfWeek(DayOfTheWeek.WEDNESDAY)
                        .openingTime(LocalTime.of(10, 0))
                        .closingTime(LocalTime.of(16, 0))
                        .build()
                ))
                .build();

        when(courtService.getCourtById(courtId)).thenReturn(court);
        when(openingHoursTypeService.getOpeningHourTypeById(openingHourType.getId())).thenReturn(openingHourType);
        when(courtOpeningHoursRepository.save(any())).thenReturn(updatedHours);

        CourtOpeningHours result = courtOpeningHoursService
            .setOpeningHours(courtId, openingHourType.getId(), updatedHours);

        assertThat(result).isEqualTo(updatedHours);
        verify(courtOpeningHoursRepository).deleteByCourtIdAndOpeningHourTypeId(courtId, openingHourType.getId());
        verify(courtOpeningHoursRepository).save(updatedHours);
    }

    @Test
    void setOpeningHoursThrowsExceptionWhenCourtDoesNotExist() {
        when(courtService.getCourtById(courtId)).thenThrow(new NotFoundException(COURT_NOT_FOUND_MESSAGE));

        assertThrows(NotFoundException.class, () ->
            courtOpeningHoursService
                .setOpeningHours(courtId, openingHourType.getId(), openingHours)
        );
    }

    @Test
    void setOpeningHoursThrowsExceptionWhenOpeningHourTypeDoesNotExist() {
        UUID typeId = UUID.randomUUID();
        when(courtService.getCourtById(courtId)).thenReturn(court);
        when(openingHoursTypeService.getOpeningHourTypeById(typeId))
            .thenThrow(new NotFoundException(OPENING_HOUR_TYPE_NOT_FOUND_MESSAGE));

        assertThrows(
            NotFoundException.class, () ->
                courtOpeningHoursService.setOpeningHours(courtId, typeId, openingHours)
        );
    }

    @Test
    void setCounterServiceOpeningHoursSuccessfullyCreatesNewOpeningHours() {

        when(courtService.getCourtById(courtId)).thenReturn(court);
        when(courtCounterServiceOpeningHoursRepository.saveAll(any())).thenReturn(counterServiceOpeningHours);

        List<CourtCounterServiceOpeningHours> result =
            courtOpeningHoursService.setCounterServiceOpeningHours(courtId, counterServiceOpeningHours);

        assertThat(result).isEqualTo(counterServiceOpeningHours);
        verify(courtCounterServiceOpeningHoursRepository).deleteByCourtId(courtId);
        verify(courtCounterServiceOpeningHoursRepository).saveAll(counterServiceOpeningHours);
    }

    @Test
    void setCounterServiceOpeningHoursRemovesOtherDaysWhenEverydayPresent() {

        List<CourtCounterServiceOpeningHours> hours = List.of(
            CourtCounterServiceOpeningHours.builder()
                .id(UUID.randomUUID())
                .courtId(courtId)
                .dayOfWeek(DayOfTheWeek.MONDAY)
                .openingHour(LocalTime.of(9, 0, 0))
                .closingHour(LocalTime.of(17, 0, 0))
                .appointmentContact("Test Contact")
                .assistWithForms(true)
                .counterService(true)
                .assistWithDocuments(true)
                .assistWithSupport(true)
                .appointmentNeeded(false)
                .build(),
            CourtCounterServiceOpeningHours.builder()
                .id(UUID.randomUUID())
                .courtId(courtId)
                .dayOfWeek(DayOfTheWeek.EVERYDAY)
                .openingHour(LocalTime.of(9, 0, 0))
                .closingHour(LocalTime.of(17, 0, 0))
                .appointmentContact("Test Contact")
                .assistWithForms(true)
                .counterService(true)
                .assistWithDocuments(true)
                .assistWithSupport(true)
                .appointmentNeeded(false)
                .build());

        List<CourtCounterServiceOpeningHours> expectedHours = List.of(
            CourtCounterServiceOpeningHours.builder()
                .id(UUID.randomUUID())
                .courtId(courtId)
                .dayOfWeek(DayOfTheWeek.EVERYDAY)
                .openingHour(LocalTime.of(9, 0, 0))
                .closingHour(LocalTime.of(17, 0, 0))
                .appointmentContact("Test Contact")
                .assistWithForms(true)
                .counterService(true)
                .assistWithDocuments(true)
                .assistWithSupport(true)
                .appointmentNeeded(false)
                .build());

        when(courtService.getCourtById(courtId)).thenReturn(court);
        when(courtCounterServiceOpeningHoursRepository.saveAll(any())).thenReturn(expectedHours);

        List<CourtCounterServiceOpeningHours> result =
            courtOpeningHoursService.setCounterServiceOpeningHours(courtId, hours);

        assertThat(result).isEqualTo(expectedHours);
        verify(courtCounterServiceOpeningHoursRepository).deleteByCourtId(courtId);
        verify(courtCounterServiceOpeningHoursRepository).saveAll(any());
    }

    @Test
    void setCounterServiceOpeningHoursUpdatesExistingOpeningHours() {
        List<CourtCounterServiceOpeningHours> updatedHours =
            List.of(new CourtCounterServiceOpeningHours());
        when(courtService.getCourtById(courtId)).thenReturn(court);
        when(courtCounterServiceOpeningHoursRepository.saveAll(any())).thenReturn(updatedHours);

        List<CourtCounterServiceOpeningHours> result =
            courtOpeningHoursService.setCounterServiceOpeningHours(courtId, updatedHours);

        assertThat(result).isEqualTo(updatedHours);
        verify(courtCounterServiceOpeningHoursRepository).deleteByCourtId(courtId);
        verify(courtCounterServiceOpeningHoursRepository).saveAll(updatedHours);
    }

    @Test
    void setCounterServiceOpeningHoursThrowsExceptionWhenCourtDoesNotExist() {
        when(courtService.getCourtById(courtId)).thenThrow(new NotFoundException(COURT_NOT_FOUND_MESSAGE));

        assertThrows(
            NotFoundException.class, () ->
                courtOpeningHoursService.setCounterServiceOpeningHours(courtId, List.of())
        );
    }

    @Test
    void deleteCourtOpeningHoursSuccessfullyDeletesHours() {
        UUID typeId = UUID.randomUUID();
        OpeningHourType type = new OpeningHourType();
        type.setId(typeId);

        when(courtService.getCourtById(courtId)).thenReturn(court);
        when(openingHoursTypeService.getOpeningHourTypeById(typeId)).thenReturn(type);

        courtOpeningHoursService.deleteCourtOpeningHours(courtId, typeId);

        verify(courtOpeningHoursRepository).deleteByCourtIdAndOpeningHourTypeId(courtId, typeId);
    }

    @Test
    void deleteCourtOpeningHoursThrowsExceptionWhenCourtDoesNotExist() {
        UUID typeId = UUID.randomUUID();
        when(courtService.getCourtById(courtId)).thenThrow(new NotFoundException(COURT_NOT_FOUND_MESSAGE));

        assertThrows(
            NotFoundException.class, () ->
                courtOpeningHoursService.deleteCourtOpeningHours(courtId, typeId)
        );
    }

    @Test
    void deleteCourtOpeningHoursThrowsExceptionWhenOpeningHourTypeDoesNotExist() {
        UUID typeId = UUID.randomUUID();
        when(courtService.getCourtById(courtId)).thenReturn(court);
        when(openingHoursTypeService.getOpeningHourTypeById(typeId))
            .thenThrow(new NotFoundException(OPENING_HOUR_TYPE_NOT_FOUND_MESSAGE));

        assertThrows(
            NotFoundException.class, () ->
                courtOpeningHoursService.deleteCourtOpeningHours(courtId, typeId)
        );
    }
}

