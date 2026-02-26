package uk.gov.hmcts.reform.fact.data.api.services;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.fact.data.api.entities.Court;
import uk.gov.hmcts.reform.fact.data.api.entities.CourtCounterServiceOpeningHours;
import uk.gov.hmcts.reform.fact.data.api.entities.CourtOpeningHours;
import uk.gov.hmcts.reform.fact.data.api.entities.CourtType;
import uk.gov.hmcts.reform.fact.data.api.entities.OpeningHourType;
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

    @Mock
    private TypesService typesService;

    @InjectMocks
    private CourtOpeningHoursService courtOpeningHoursService;

    private UUID courtId;
    private Court court;
    private CourtOpeningHours openingHours;
    private CourtCounterServiceOpeningHours counterServiceOpeningHours;
    private UUID openingHourTypeId;
    private OpeningHourType openingHourType;
    private List<OpeningTimesDetail> openingTimesDetails;

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

        openingTimesDetails = List.of(
            new OpeningTimesDetail(
                DayOfTheWeek.MONDAY,
                LocalTime.of(9, 0),
                LocalTime.of(17, 0)
            ),
            new OpeningTimesDetail(
                DayOfTheWeek.TUESDAY,
                LocalTime.of(10, 0),
                LocalTime.of(17, 0)
            ),
            new OpeningTimesDetail(
                DayOfTheWeek.WEDNESDAY,
                LocalTime.of(9, 0),
                LocalTime.of(16, 0)
            ),
            new OpeningTimesDetail(
                DayOfTheWeek.THURSDAY,
                LocalTime.of(10, 0),
                LocalTime.of(16, 0)
            ),
            new OpeningTimesDetail(
                DayOfTheWeek.FRIDAY,
                LocalTime.of(9, 30),
                LocalTime.of(17, 0)
            )
        );

        openingHours =
            CourtOpeningHours.builder()
                .id(UUID.randomUUID())
                .courtId(courtId)
                .openingTimesDetails(openingTimesDetails)
                .openingHourTypeId(openingHourTypeId)
                .build();

        counterServiceOpeningHours =
            CourtCounterServiceOpeningHours.builder()
                .id(UUID.randomUUID())
                .courtId(courtId)
                .openingTimesDetails(openingTimesDetails)
                .appointmentContact("Test Contact")
                .assistWithForms(true)
                .counterService(true)
                .assistWithDocuments(true)
                .assistWithSupport(true)
                .appointmentNeeded(false)
                .build();
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
        when(courtOpeningHoursRepository.findByCourtIdAndId(courtId, openingHours.getId()))
            .thenReturn(Optional.of(openingHours));

        CourtOpeningHours result = courtOpeningHoursService
            .getOpeningHoursById(courtId, openingHours.getId());

        assertThat(result).isEqualTo(openingHours);
    }

    @Test
    void getOpeningHoursByIdThrowsExceptionWhenNotFound() {
        when(courtService.getCourtById(courtId)).thenReturn(court);
        when(courtOpeningHoursRepository.findByCourtIdAndId(courtId, openingHours.getId()))
            .thenReturn(Optional.empty());

        assertThrows(
            CourtResourceNotFoundException.class,
            () -> courtOpeningHoursService.getOpeningHoursById(courtId, openingHours.getId())
        );
    }

    @Test
    void getOpeningHoursByTypeIdThrowsExceptionWhenCourtDoesNotExist() {
        when(courtService.getCourtById(courtId)).thenThrow(new NotFoundException(COURT_NOT_FOUND_MESSAGE));

        assertThrows(NotFoundException.class, () ->
            courtOpeningHoursService.getOpeningHoursById(courtId, openingHours.getId())
        );
    }

    @Test
    void getOpeningHoursThrowsExceptionWhenOpeningHourTypeDoesNotExist() {
        when(courtService.getCourtById(courtId)).thenReturn(court);
        when(courtOpeningHoursRepository.findByCourtIdAndId(courtId, openingHours.getId()))
            .thenReturn(Optional.empty());

        assertThrows(
            CourtResourceNotFoundException.class, () ->
                courtOpeningHoursService.getOpeningHoursById(courtId, openingHours.getId())
        );
    }

    @Test
    void getCounterServiceOpeningHoursByCourtIdReturnsOpeningHoursWhenFound() {
        when(courtService.getCourtById(courtId)).thenReturn(court);
        when(courtCounterServiceOpeningHoursRepository.findByCourtId(courtId))
            .thenReturn(Optional.of(counterServiceOpeningHours));

        CourtCounterServiceOpeningHours result =
            courtOpeningHoursService.getCounterServiceOpeningHoursByCourtId(courtId);

        assertThat(result).isEqualTo(counterServiceOpeningHours);
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
            .setOpeningHours(courtId, openingHours);

        assertThat(result).isEqualTo(openingHours);
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
            courtOpeningHoursService.setOpeningHours(courtId, hours);

        assertThat(result).isEqualTo(expectedHours);
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
            .setOpeningHours(courtId, updatedHours);

        assertThat(result).isEqualTo(updatedHours);
        verify(courtOpeningHoursRepository).save(updatedHours);
    }

    @Test
    void setOpeningHoursThrowsExceptionWhenCourtDoesNotExist() {
        when(courtService.getCourtById(courtId)).thenThrow(new NotFoundException(COURT_NOT_FOUND_MESSAGE));

        assertThrows(NotFoundException.class, () ->
            courtOpeningHoursService
                .setOpeningHours(courtId, openingHours)
        );
    }

    @Test
    void setOpeningHoursThrowsExceptionWhenOpeningHourTypeDoesNotExist() {
        when(courtService.getCourtById(courtId)).thenReturn(court);
        when(openingHoursTypeService.getOpeningHourTypeById(openingHours.getOpeningHourTypeId()))
            .thenThrow(new NotFoundException(OPENING_HOUR_TYPE_NOT_FOUND_MESSAGE));

        assertThrows(
            NotFoundException.class, () ->
                courtOpeningHoursService.setOpeningHours(courtId, openingHours)
        );
    }

    @Test
    void setCounterServiceOpeningHoursSuccessfullyCreatesNewOpeningHours() {

        when(courtService.getCourtById(courtId)).thenReturn(court);
        when(courtCounterServiceOpeningHoursRepository.save(any())).thenReturn(counterServiceOpeningHours);

        CourtCounterServiceOpeningHours result =
            courtOpeningHoursService.setCounterServiceOpeningHours(courtId, counterServiceOpeningHours);

        assertThat(result).isEqualTo(counterServiceOpeningHours);
        verify(courtCounterServiceOpeningHoursRepository).save(counterServiceOpeningHours);
    }

    @Test
    void setCounterServiceOpeningHoursRemovesOtherDaysWhenEverydayPresent() {

        CourtCounterServiceOpeningHours hours =
            CourtCounterServiceOpeningHours.builder()
                .id(UUID.randomUUID())
                .courtId(courtId)
                .openingTimesDetails(List.of(
                    OpeningTimesDetail.builder()
                        .dayOfWeek(DayOfTheWeek.MONDAY)
                        .openingTime(LocalTime.of(9, 0, 0))
                        .closingTime(LocalTime.of(17, 0, 0))
                        .build(),
                    OpeningTimesDetail.builder()
                        .dayOfWeek(DayOfTheWeek.EVERYDAY)
                        .openingTime(LocalTime.of(9, 0, 0))
                        .closingTime(LocalTime.of(17, 0, 0))
                        .build()
                ))
                .appointmentContact("Test Contact")
                .assistWithForms(true)
                .counterService(true)
                .assistWithDocuments(true)
                .assistWithSupport(true)
                .appointmentNeeded(false)
                .build();

        CourtCounterServiceOpeningHours expectedHours =
            CourtCounterServiceOpeningHours.builder()
                .id(UUID.randomUUID())
                .courtId(courtId)
                .openingTimesDetails(List.of(
                    OpeningTimesDetail.builder()
                        .dayOfWeek(DayOfTheWeek.EVERYDAY)
                        .openingTime(LocalTime.of(9, 0, 0))
                        .closingTime(LocalTime.of(17, 0, 0))
                        .build()
                ))
                .appointmentContact("Test Contact")
                .assistWithForms(true)
                .counterService(true)
                .assistWithDocuments(true)
                .assistWithSupport(true)
                .appointmentNeeded(false)
                .build();

        when(courtService.getCourtById(courtId)).thenReturn(court);
        when(courtCounterServiceOpeningHoursRepository.save(any())).thenReturn(expectedHours);

        CourtCounterServiceOpeningHours result =
            courtOpeningHoursService.setCounterServiceOpeningHours(courtId, hours);

        assertThat(result).isEqualTo(expectedHours);
        verify(courtCounterServiceOpeningHoursRepository).save(any());
    }

    @Test
    void setCounterServiceOpeningHoursUpdatesExistingOpeningHours() {
        CourtCounterServiceOpeningHours updatedHours = new CourtCounterServiceOpeningHours();
        updatedHours.setOpeningTimesDetails(List.of());
        when(courtService.getCourtById(courtId)).thenReturn(court);
        when(courtCounterServiceOpeningHoursRepository.save(any())).thenReturn(updatedHours);

        CourtCounterServiceOpeningHours result =
            courtOpeningHoursService.setCounterServiceOpeningHours(courtId, updatedHours);

        assertThat(result).isEqualTo(updatedHours);
        verify(courtCounterServiceOpeningHoursRepository).save(updatedHours);
    }

    @Test
    void setCounterServiceOpeningHoursThrowsExceptionWhenCourtDoesNotExist() {
        when(courtService.getCourtById(courtId)).thenThrow(new NotFoundException(COURT_NOT_FOUND_MESSAGE));
        CourtCounterServiceOpeningHours hours = new CourtCounterServiceOpeningHours();
        hours.setOpeningTimesDetails(List.of());

        assertThrows(
            NotFoundException.class, () ->
                courtOpeningHoursService.setCounterServiceOpeningHours(courtId, hours)
        );
    }

    @Test
    void setCounterServiceOpeningHoursValidatesCourtTypes() {
        List<UUID> courtTypeIds = List.of(UUID.randomUUID());
        counterServiceOpeningHours.setCourtTypes(courtTypeIds);

        when(courtService.getCourtById(courtId)).thenReturn(court);
        CourtType courtType = new CourtType();
        courtType.setId(courtTypeIds.getFirst());
        when(typesService.getAllCourtTypesByIds(courtTypeIds)).thenReturn(List.of(courtType));
        when(courtCounterServiceOpeningHoursRepository.save(any(CourtCounterServiceOpeningHours.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));

        CourtCounterServiceOpeningHours result
            = courtOpeningHoursService.setCounterServiceOpeningHours(courtId, counterServiceOpeningHours);

        assertThat(result.getCourtTypes()).isEqualTo(courtTypeIds);
        verify(typesService).getAllCourtTypesByIds(courtTypeIds);
        verify(courtCounterServiceOpeningHoursRepository).save(counterServiceOpeningHours);
    }

    @Test
    void deleteCourtOpeningHoursSuccessfullyDeletesHours() {
        courtOpeningHoursService.deleteCourtOpeningHours(courtId, openingHours.getId());

        verify(courtOpeningHoursRepository).deleteById(openingHours.getId());
    }
}

