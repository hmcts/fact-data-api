package uk.gov.hmcts.reform.fact.data.api.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.qameta.allure.Feature;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import uk.gov.hmcts.reform.fact.data.api.entities.Court;
import uk.gov.hmcts.reform.fact.data.api.entities.CourtCounterServiceOpeningHours;
import uk.gov.hmcts.reform.fact.data.api.entities.CourtOpeningHours;
import uk.gov.hmcts.reform.fact.data.api.entities.OpeningHourType;
import uk.gov.hmcts.reform.fact.data.api.entities.types.DayOfTheWeek;
import uk.gov.hmcts.reform.fact.data.api.entities.types.OpeningTimesDetail;
import uk.gov.hmcts.reform.fact.data.api.errorhandling.exceptions.CourtResourceNotFoundException;
import uk.gov.hmcts.reform.fact.data.api.errorhandling.exceptions.NotFoundException;
import uk.gov.hmcts.reform.fact.data.api.services.CourtOpeningHoursService;

import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.doThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Feature("Court Opening Hours Controller")
@DisplayName("Court Opening Hours Controller")
@WebMvcTest(CourtOpeningHoursController.class)
@AutoConfigureMockMvc(addFilters = false)
class CourtOpeningHoursControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private CourtOpeningHoursService courtOpeningHoursService;

    private UUID courtId;
    private UUID nonExistentCourtId;
    private UUID openingHourTypeId;
    private Court court;
    private OpeningHourType openingHourType;
    private CourtOpeningHours openingHours;
    private CourtCounterServiceOpeningHours counterServiceOpeningHours;
    private List<OpeningTimesDetail> openingTimesDetails;

    @BeforeEach
    public void setup() {
        nonExistentCourtId = UUID.randomUUID();
        openingHourTypeId = UUID.randomUUID();
        courtId = UUID.randomUUID();
        openingHourType = new OpeningHourType();
        openingHourType.setId(openingHourTypeId);
        openingHourType.setName("name");
        court = new Court();
        court.setId(courtId);
        court.setName("Test Court");

        openingHourType = OpeningHourType.builder()
            .id(UUID.randomUUID())
            .name("name")
            .nameCy("nameCy")
            .build();

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
                .openingHourTypeId(openingHourTypeId)
                .openingTimesDetails(openingTimesDetails)
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
    @DisplayName("GET /courts/{courtId}/v1/opening-hours returns opening hours successfully")
    void getOpeningHoursReturnsSuccessfully() throws Exception {
        when(courtOpeningHoursService.getOpeningHoursByCourtId(courtId)).thenReturn(List.of(openingHours));

        mockMvc.perform(get("/courts/{courtId}/v1/opening-hours", courtId))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].openingTimesDetails[0].dayOfWeek")
                           .value(DayOfTheWeek.MONDAY.toString()))
            .andExpect(jsonPath("$[0].openingTimesDetails[0].openingTime").value("09:00:00"));
    }

    @Test
    @DisplayName("GET /courts/{courtId}/v1/opening-hours returns 404 if court does not exist")
    void getOpeningHoursCourtNonExistentReturnsNotFound() throws Exception {
        when(courtOpeningHoursService.getOpeningHoursByCourtId(nonExistentCourtId))
            .thenThrow(new NotFoundException("Court not found"));

        mockMvc.perform(get("/courts/{courtId}/v1/opening-hours", nonExistentCourtId))
            .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("GET /courts/{courtId}/v1/opening-hours returns 204 if opening hours does not exist")
    void getOpeningHoursNonExistentCourtReturnsNoContent() throws Exception {
        when(courtOpeningHoursService.getOpeningHoursByCourtId(courtId))
            .thenThrow(new CourtResourceNotFoundException("Opening hours not found"));

        mockMvc.perform(get("/courts/{courtId}/v1/opening-hours", courtId))
            .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("GET /courts/{courtId}/v1/opening-hours returns 400 for invalid UUID")
    void getOpeningHoursInvalidUUID() throws Exception {
        mockMvc.perform(get("/courts/{courtId}/v1/opening-hours", "invalid-uuid"))
            .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("GET /courts/{courtId}/v1/opening-hours/{openingHoursId} returns opening hours successfully")
    void getOpeningHoursByIdReturnsSuccessfully() throws Exception {
        when(courtOpeningHoursService.getOpeningHoursById(courtId, openingHours.getId()))
                 .thenReturn(openingHours);

        mockMvc
            .perform(
                get(
                    "/courts/{courtId}/v1/opening-hours/{openingHoursId}",
                    courtId,
                    openingHours.getId()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.openingTimesDetails[0].dayOfWeek")
                           .value(DayOfTheWeek.MONDAY.toString()))
            .andExpect(jsonPath("$.openingTimesDetails[0].openingTime").value("09:00:00"));
    }

    @Test
    @DisplayName("GET /courts/{courtId}/v1/opening-hours/{openingHoursId} returns 404 if court does not exist")
    void getOpeningHoursByIdCourtNonExistentReturnsNotFound() throws Exception {
        when(courtOpeningHoursService.getOpeningHoursById(nonExistentCourtId, openingHours.getId()))
            .thenThrow(new NotFoundException("Court not found"));

        mockMvc.perform(get(
            "/courts/{courtId}/v1/opening-hours/{openingHoursId}",
            nonExistentCourtId, openingHours.getId()))
            .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("GET /courts/{courtId}/v1/opening-hours/{openingHoursId}"
        + " returns 204 if opening hours does not exist")
    void getOpeningHoursByIdNonExistentCourtReturnsNoContent() throws Exception {
        when(courtOpeningHoursService.getOpeningHoursById(courtId, openingHours.getId()))
            .thenThrow(new CourtResourceNotFoundException("Opening hours not found"));

        mockMvc
            .perform(
                get(
                    "/courts/{courtId}/v1/opening-hours/{openingHoursId}",
                    courtId, openingHours.getId()))
            .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("GET /courts/{courtId}/v1/opening-hours/{openingHoursId} returns 400 for invalid UUID")
    void getOpeningHoursByIdInvalidUUID() throws Exception {
        mockMvc
            .perform(
                get(
                    "/courts/{courtId}/v1/opening-hours/{openingHoursId}",
                    "invalid-uuid", openingHours.getId()))
            .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("GET /courts/{courtId}/v1/opening-hours/counter-service returns opening hours successfully")
    void getCounterServiceOpeningHoursReturnsSuccessfully() throws Exception {
        when(courtOpeningHoursService.getCounterServiceOpeningHoursByCourtId(courtId))
            .thenReturn(counterServiceOpeningHours);

        mockMvc.perform(get("/courts/{courtId}/v1/opening-hours/counter-service", courtId))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.openingTimesDetails[0].dayOfWeek")
                           .value(counterServiceOpeningHours
                                      .getOpeningTimesDetails().getFirst().getDayOfWeek().toString()))
            .andExpect(jsonPath("$.openingTimesDetails[0].openingTime")
                           .value("09:00:00"));
    }

    @Test
    @DisplayName("GET /courts/{courtId}/v1/opening-hours/counter-service returns 404 if court does not exist")
    void getCounterServiceOpeningHoursCourtNonExistentReturnsNotFound() throws Exception {
        when(courtOpeningHoursService.getCounterServiceOpeningHoursByCourtId(nonExistentCourtId))
            .thenThrow(new NotFoundException("Court not found"));

        mockMvc.perform(get("/courts/{courtId}/v1/opening-hours/counter-service", nonExistentCourtId))
            .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("GET /courts/{courtId}/v1/opening-hours/counter-service returns 204 if opening hours does not exist")
    void getCounterServiceOpeningHoursNonExistentCourtReturnsNoContent() throws Exception {
        when(courtOpeningHoursService.getCounterServiceOpeningHoursByCourtId(courtId))
            .thenThrow(new CourtResourceNotFoundException("Opening hours not found"));

        mockMvc.perform(get("/courts/{courtId}/v1/opening-hours/counter-service", courtId))
            .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("GET /courts/{courtId}/v1/opening-hours/counter-service returns 400 for invalid UUID")
    void getCounterServiceOpeningHoursInvalidUUID() throws Exception {
        mockMvc.perform(
            get("/courts/{courtId}/v1/opening-hours/counter-service", "invalid-uuid"))
            .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("PUT /courts/{courtId}/v1/opening-hours creates opening hours successfully")
    void setOpeningHoursCreatesSuccessfully() throws Exception {
        when(courtOpeningHoursService.setOpeningHours(any(UUID.class), any()))
            .thenReturn(openingHours);

        mockMvc
            .perform(
                put("/courts/{courtId}/v1/opening-hours", courtId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(openingHours)))
            .andExpect(status().isOk())
            .andExpect(
                jsonPath("$.openingTimesDetails[0].dayOfWeek").value(DayOfTheWeek.MONDAY.toString()))
            .andExpect(jsonPath("$.openingTimesDetails[0].openingTime").value("09:00:00"));
    }

    @Test
    @DisplayName("PUT /courts/{courtId}/v1/opening-hours returns 404 if court does not exist")
    void setOpeningHoursNonExistentCourtReturnsNotFound() throws Exception {
        when(courtOpeningHoursService.setOpeningHours(any(UUID.class), any()))
            .thenThrow(new NotFoundException("Court not found"));

        mockMvc
            .perform(
                put(
                    "/courts/{courtId}/v1/opening-hours",
                    nonExistentCourtId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(openingHours)))
            .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("PUT /courts/{courtId}/v1/opening-hours returns 400 for invalid UUID")
    void setOpeningHoursInvalidUUID() throws Exception {
        mockMvc
            .perform(
                put(
                    "/courts/{courtId}/v1/opening-hours",
                    "invalid-uuid", openingHourTypeId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(openingHours)))
            .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("PUT /courts/{courtId}/v1/opening-hours returns 400 for null day of week")
    void setOpeningHoursNullDayOfWeekReturnsBadRequest() throws Exception {
        List<CourtOpeningHours> invalidOpeningHours = List.of(
            CourtOpeningHours.builder()
                .id(UUID.randomUUID())
                .courtId(courtId)
                .openingHourTypeId(openingHourTypeId)
                .openingTimesDetails(List.of(
                    OpeningTimesDetail.builder()
                        .dayOfWeek(null)
                        .openingTime(LocalTime.of(9, 0))
                        .closingTime(LocalTime.of(17, 0))
                        .build()
                ))
                .build()
        );

        mockMvc
            .perform(
                put("/courts/{courtId}/v1/opening-hours", courtId, openingHourTypeId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(invalidOpeningHours)))
            .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("PUT /courts/{courtId}/v1/opening-hours returns 400 for null opening hour")
    void setOpeningHoursNullOpeningHourReturnsBadRequest() throws Exception {
        List<CourtOpeningHours> invalidOpeningHours = List.of(
            CourtOpeningHours.builder()
                .id(UUID.randomUUID())
                .courtId(courtId)
                .openingHourTypeId(openingHourTypeId)
                .openingTimesDetails(List.of(
                    OpeningTimesDetail.builder()
                        .dayOfWeek(DayOfTheWeek.MONDAY)
                        .openingTime(null)
                        .closingTime(LocalTime.of(17, 0))
                        .build()
                ))
                .build()
        );

        mockMvc
            .perform(
                put("/courts/{courtId}/v1/opening-hours", courtId, openingHours.getId())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(invalidOpeningHours)))
            .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("PUT /courts/{courtId}/v1/opening-hours returns 400 for null closing hour")
    void setOpeningHourNullClosingHourReturnsBadRequest() throws Exception {
        List<CourtOpeningHours> invalidOpeningHours = List.of(
            CourtOpeningHours.builder()
                .id(UUID.randomUUID())
                .courtId(courtId)
                .openingHourTypeId(openingHourTypeId)
                .openingTimesDetails(List.of(
                    OpeningTimesDetail.builder()
                        .dayOfWeek(DayOfTheWeek.MONDAY)
                        .openingTime(LocalTime.of(9, 0))
                        .closingTime(null)
                        .build()
                ))
                .build()
        );

        mockMvc
            .perform(
                put("/courts/{courtId}/v1/opening-hours", courtId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(invalidOpeningHours)))
            .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("PUT /courts/{courtId}/v1/opening-hours/counter-service creates opening hours successfully")
    void setCounterServiceOpeningHoursCreatesSuccessfully() throws Exception {
        when(courtOpeningHoursService.setCounterServiceOpeningHours(any(UUID.class), any()))
            .thenReturn(counterServiceOpeningHours);

        mockMvc.perform(put("/courts/{courtId}/v1/opening-hours/counter-service", courtId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(counterServiceOpeningHours)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.openingTimesDetails[0].dayOfWeek").value(DayOfTheWeek.MONDAY.toString()))
            .andExpect(jsonPath("$.openingTimesDetails[0].openingTime").value("09:00:00"));
    }

    @Test
    @DisplayName("PUT /courts/{courtId}/v1/opening-hours/counter-service returns 404 if court does not exist")
    void putCounterServiceOpeningHoursNonExistentCourtReturnsNotFound() throws Exception {
        when(courtOpeningHoursService.setCounterServiceOpeningHours(any(UUID.class), any()))
            .thenThrow(new NotFoundException("Court not found"));

        mockMvc.perform(put("/courts/{courtId}/v1/opening-hours/counter-service", nonExistentCourtId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(counterServiceOpeningHours)))
            .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("PUT /courts/{courtId}/v1/opening-hours/counter-service returns 400 for invalid UUID")
    void setCounterServiceOpeningHoursInvalidUUID() throws Exception {
        mockMvc.perform(put("/courts/{courtId}/v1/opening-hours/counter-service", "invalid-uuid")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(counterServiceOpeningHours)))
            .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("PUT /courts/{courtId}/v1/opening-hours/counter-service returns 400 for null day of week")
    void setCounterServiceOpeningHoursNullDayOfWeekReturnsBadRequest() throws Exception {
        CourtCounterServiceOpeningHours invalidOpeningHours =
            CourtCounterServiceOpeningHours.builder()
                .id(UUID.randomUUID())
                .courtId(courtId)
                .openingTimesDetails(List.of(
                    OpeningTimesDetail.builder()
                        .dayOfWeek(null)
                        .openingTime(LocalTime.of(9, 0))
                        .closingTime(LocalTime.of(17, 0))
                        .build()
                ))
                .build();

        mockMvc.perform(put("/courts/{courtId}/v1/opening-hours/counter-service", courtId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(invalidOpeningHours)))
            .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("PUT /courts/{courtId}/v1/opening-hours/counter-service returns 400 for null opening hour")
    void setCounterServiceOpeningHoursNullOpeningHourReturnsBadRequest() throws Exception {
        CourtCounterServiceOpeningHours invalidOpeningHours =
            CourtCounterServiceOpeningHours.builder()
                .id(UUID.randomUUID())
                .courtId(courtId)
                .openingTimesDetails(List.of(
                    OpeningTimesDetail.builder()
                        .dayOfWeek(DayOfTheWeek.MONDAY)
                        .openingTime(null)
                        .closingTime(LocalTime.of(17, 0))
                        .build()
                ))
                .build();

        mockMvc.perform(put("/courts/{courtId}/v1/opening-hours/counter-service", courtId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(invalidOpeningHours)))
            .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("PUT /courts/{courtId}/v1/opening-hours/counter-service returns 400 for null closing hour")
    void setCounterServiceOpeningHoursNullClosingHourReturnsBadRequest() throws Exception {
        CourtCounterServiceOpeningHours invalidOpeningHours =
            CourtCounterServiceOpeningHours.builder()
                .id(UUID.randomUUID())
                .courtId(courtId)
                .openingTimesDetails(List.of(
                    OpeningTimesDetail.builder()
                        .dayOfWeek(DayOfTheWeek.MONDAY)
                        .openingTime(LocalTime.of(9, 0))
                        .closingTime(null)
                        .build()
                ))
                .build();

        mockMvc.perform(put("/courts/{courtId}/v1/opening-hours/counter-service", courtId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(invalidOpeningHours)))
            .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("DELETE /courts/{courtId}/v1/opening-hours/{openingHoursId} deletes opening hours successfully")
    void deleteOpeningHoursDeletesSuccessfully() throws Exception {
        mockMvc.perform(delete(
                "/courts/{courtId}/v1/opening-hours/{openingHourTypeId}",
                courtId, openingHours.getId()
            ))
            .andExpect(status().isOk());
    }

    @Test
    @DisplayName("DELETE /courts/{courtId}/v1/opening-hours/{openingHoursId} returns 404 if court does not exist")
    void deleteOpeningHoursNonExistentCourtReturnsNotFound() throws Exception {
        doThrow(new NotFoundException("Court not found"))
            .when(courtOpeningHoursService)
            .deleteCourtOpeningHours(nonExistentCourtId, openingHours.getId());

        mockMvc.perform(delete(
                "/courts/{courtId}/v1/opening-hours/{openingHoursId}",
                nonExistentCourtId, openingHours.getId()
            ))
            .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("DELETE /courts/{courtId}/v1/opening-hours/{openingHoursId} returns 400 for invalid UUID")
    void deleteOpeningHoursInvalidUUID() throws Exception {
        mockMvc.perform(delete(
                "/courts/{courtId}/v1/opening-hours/{openingHoursId}",
                "invalid-uuid", openingHours.getId()
            ))
            .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("DELETE /courts/{courtId}/v1/opening-hours/counter-service deletes opening hours successfully")
    void deleteCounterServiceOpeningHoursDeletesSuccessfully() throws Exception {
        mockMvc.perform(delete(
                "/courts/{courtId}/v1/opening-hours/counter-service",
                courtId
            ))
            .andExpect(status().isOk());
    }

    @Test
    @DisplayName("DELETE /courts/{courtId}/v1/opening-hours/counter-service returns 404 if court does not exist")
    void deleteCounterServiceOpeningHoursNonExistentCourtReturnsNotFound() throws Exception {
        doThrow(new NotFoundException("Court not found"))
            .when(courtOpeningHoursService)
            .deleteCourtCounterServiceOpeningHours(nonExistentCourtId);

        mockMvc.perform(delete(
                "/courts/{courtId}/v1/opening-hours/counter-service",
                nonExistentCourtId
            ))
            .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("DELETE /courts/{courtId}/v1/opening-hours/counter-service returns 400 for invalid UUID")
    void deleteCounterServiceOpeningHoursInvalidUUID() throws Exception {
        mockMvc.perform(delete(
                "/courts/{courtId}/v1/opening-hours/counter-service",
                "invalid-uuid"
            ))
            .andExpect(status().isBadRequest());
    }
}
