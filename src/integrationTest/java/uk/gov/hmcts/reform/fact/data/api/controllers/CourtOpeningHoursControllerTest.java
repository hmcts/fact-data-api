package uk.gov.hmcts.reform.fact.data.api.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import uk.gov.hmcts.reform.fact.data.api.entities.Court;
import uk.gov.hmcts.reform.fact.data.api.entities.CourtCounterServiceOpeningHours;
import uk.gov.hmcts.reform.fact.data.api.entities.CourtOpeningHours;
import uk.gov.hmcts.reform.fact.data.api.entities.OpeningHourType;
import uk.gov.hmcts.reform.fact.data.api.entities.types.DayOfTheWeek;
import uk.gov.hmcts.reform.fact.data.api.errorhandling.exceptions.CourtResourceNotFoundException;
import uk.gov.hmcts.reform.fact.data.api.errorhandling.exceptions.NotFoundException;
import uk.gov.hmcts.reform.fact.data.api.services.CourtOpeningHoursService;

import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.doThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

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
    private List<CourtOpeningHours> openingHours;
    private List<CourtCounterServiceOpeningHours> counterServiceOpeningHours;

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

        openingHours = List.of(
            CourtOpeningHours.builder()
                .id(UUID.randomUUID())
                .courtId(courtId)
                .openingHourTypeId(openingHourTypeId)
                .dayOfWeek(DayOfTheWeek.MONDAY)
                .openingHour(LocalTime.of(9, 0, 0))
                .closingHour(LocalTime.of(17, 0, 0))
                .build(),
            CourtOpeningHours.builder()
                .id(UUID.randomUUID())
                .courtId(courtId)
                .openingHourTypeId(openingHourTypeId)
                .dayOfWeek(DayOfTheWeek.TUESDAY)
                .openingHour(LocalTime.of(9, 0, 0))
                .closingHour(LocalTime.of(17, 0, 0))
                .build()
        );

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
    @DisplayName("GET /courts/{courtId}/v1/opening-hours returns opening hours successfully")
    void getOpeningHoursReturnsSuccessfully() throws Exception {
        when(courtOpeningHoursService.getOpeningHoursByCourtId(courtId)).thenReturn(openingHours);

        mockMvc.perform(get("/courts/{courtId}/v1/opening-hours", courtId))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].dayOfWeek").value(DayOfTheWeek.MONDAY.toString()))
            .andExpect(jsonPath("$[0].openingHour").value("09:00:00"));
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
    @DisplayName("GET /courts/{courtId}/v1/opening-hours/{openingHourTypeId} returns opening hours successfully")
    void getOpeningHoursByTypeIdReturnsSuccessfully() throws Exception {
        when(courtOpeningHoursService.getOpeningHoursByTypeId(courtId, openingHourType.getId()))
                 .thenReturn(openingHours);

        mockMvc
            .perform(
                get(
                    "/courts/{courtId}/v1/opening-hours/{openingHourTypeId}",
                    courtId,
                    openingHourType.getId()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].dayOfWeek").value(DayOfTheWeek.MONDAY.toString()))
            .andExpect(jsonPath("$[0].openingHour").value("09:00:00"));
    }

    @Test
    @DisplayName("GET /courts/{courtId}/v1/opening-hours/{openingHourTypeId} returns 404 if court does not exist")
    void getOpeningHoursByTypeIdCourtNonExistentReturnsNotFound() throws Exception {
        when(courtOpeningHoursService.getOpeningHoursByTypeId(nonExistentCourtId, openingHourTypeId))
            .thenThrow(new NotFoundException("Court not found"));

        mockMvc.perform(get(
            "/courts/{courtId}/v1/opening-hours/{openingHourTypeId}",
            nonExistentCourtId, openingHourTypeId))
            .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("GET /courts/{courtId}/v1/opening-hours/{openingHourTypeId}"
        + " returns 204 if opening hours does not exist")
    void getOpeningHoursByTypeIdNonExistentCourtReturnsNoContent() throws Exception {
        when(courtOpeningHoursService.getOpeningHoursByTypeId(courtId, openingHourTypeId))
            .thenThrow(new CourtResourceNotFoundException("Opening hours not found"));

        mockMvc
            .perform(
                get(
                    "/courts/{courtId}/v1/opening-hours/{openingHourTypeId}",
                    courtId, openingHourTypeId))
            .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("GET /courts/{courtId}/v1/opening-hours/{openingHourTypeId} returns 400 for invalid UUID")
    void getOpeningHoursByTypeIdInvalidUUID() throws Exception {
        mockMvc
            .perform(
                get(
                    "/courts/{courtId}/v1/opening-hours/{openingHourTypeId}",
                    "invalid-uuid", openingHourTypeId))
            .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("GET /courts/{courtId}/v1/opening-hours/counter-service returns opening hours successfully")
    void getCounterServiceOpeningHoursReturnsSuccessfully() throws Exception {
        when(courtOpeningHoursService.getCounterServiceOpeningHoursByCourtId(courtId))
            .thenReturn(counterServiceOpeningHours);

        mockMvc.perform(get("/courts/{courtId}/v1/opening-hours/counter-service", courtId))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].dayOfWeek")
                           .value(counterServiceOpeningHours.getFirst().getDayOfWeek().toString()))
            .andExpect(jsonPath("$[0].openingHour")
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
        mockMvc.perform(get("/courts/{courtId}/v1/opening-hours/counter-service", "invalid-uuid"))
            .andExpect(status().isBadRequest());
    }


    @Test
    @DisplayName("PUT /courts/{courtId}/v1/opening-hours/{openingHourTypeId} creates opening hours successfully")
    void setOpeningHoursCreatesSuccessfully() throws Exception {
        when(courtOpeningHoursService.setOpeningHours(any(UUID.class), any(UUID.class), anyList()))
            .thenReturn(openingHours);

        mockMvc
            .perform(
                put("/courts/{courtId}/v1/opening-hours/{openingHourTypeId}", courtId, openingHourTypeId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(openingHours)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].dayOfWeek").value(DayOfTheWeek.MONDAY.toString()))
            .andExpect(jsonPath("$[0].openingHour").value("09:00:00"));
    }

    @Test
    @DisplayName("PUT /courts/{courtId}/v1/opening-hours/{openingHourTypeId} returns 404 if court does not exist")
    void setOpeningHoursNonExistentCourtReturnsNotFound() throws Exception {
        when(courtOpeningHoursService.setOpeningHours(any(UUID.class), any(UUID.class), anyList()))
            .thenThrow(new NotFoundException("Court not found"));

        mockMvc
            .perform(
                put(
                    "/courts/{courtId}/v1/opening-hours/{openingHourTypeId}",
                    nonExistentCourtId, openingHourTypeId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(openingHours)))
            .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("PUT /courts/{courtId}/v1/opening-hours/{openingHourTypeId} returns 400 for invalid UUID")
    void setOpeningHoursInvalidUUID() throws Exception {
        mockMvc
            .perform(
                put(
                    "/courts/{courtId}/v1/opening-hours/{openingHourTypeId}",
                    "invalid-uuid", openingHourTypeId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(openingHours)))
            .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("PUT /courts/{courtId}/v1/opening-hours/{openingHourTypeId} returns 400 for null day of week")
    void setOpeningHoursNullDayOfWeekReturnsBadRequest() throws Exception {
        List<CourtOpeningHours> invalidOpeningHours = List.of(
            CourtOpeningHours.builder()
                .id(UUID.randomUUID())
                .courtId(courtId)
                .dayOfWeek(null)
                .openingHour(LocalTime.of(9, 0))
                .closingHour(LocalTime.of(17, 0))
                .build()
        );

        mockMvc
            .perform(
                put("/courts/{courtId}/v1/opening-hours/{openingHourTypeId}", courtId, openingHourTypeId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(invalidOpeningHours)))
            .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("PUT /courts/{courtId}/v1/opening-hours/{openingHourTypeId} returns 400 for null opening hour")
    void setOpeningHoursNullOpeningHourReturnsBadRequest() throws Exception {
        List<CourtOpeningHours> invalidOpeningHours = List.of(
            CourtOpeningHours.builder()
                .id(UUID.randomUUID())
                .courtId(courtId)
                .dayOfWeek(DayOfTheWeek.MONDAY)
                .openingHour(null)
                .closingHour(LocalTime.of(17, 0))
                .build()
        );

        mockMvc
            .perform(
                put("/courts/{courtId}/v1/opening-hours/{openingHourTypeId}", courtId, openingHourTypeId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(invalidOpeningHours)))
            .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("PUT /courts/{courtId}/v1/opening-hours/{openingHourTypeId} returns 400 for null closing hour")
    void setOpeningHourNullClosingHourReturnsBadRequest() throws Exception {
        List<CourtOpeningHours> invalidOpeningHours = List.of(
            CourtOpeningHours.builder()
                .id(UUID.randomUUID())
                .courtId(courtId)
                .dayOfWeek(DayOfTheWeek.MONDAY)
                .openingHour(LocalTime.of(9, 0))
                .closingHour(null)
                .build()
        );

        mockMvc
            .perform(
                put("/courts/{courtId}/v1/opening-hours/{openingHourTypeId}", courtId, openingHourTypeId)
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
            .andExpect(jsonPath("$[0].dayOfWeek").value(DayOfTheWeek.MONDAY.toString()))
            .andExpect(jsonPath("$[0].openingHour").value("09:00:00"));
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
        List<CourtCounterServiceOpeningHours> invalidOpeningHours = List.of(
            CourtCounterServiceOpeningHours.builder()
                .id(UUID.randomUUID())
                .courtId(courtId)
                .dayOfWeek(null)
                .openingHour(LocalTime.of(9, 0))
                .closingHour(LocalTime.of(17, 0))
                .build()
        );

        mockMvc.perform(put("/courts/{courtId}/v1/opening-hours/counter-service", courtId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(invalidOpeningHours)))
            .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("PUT /courts/{courtId}/v1/opening-hours/counter-service returns 400 for null opening hour")
    void setCounterServiceOpeningHoursNullOpeningHourReturnsBadRequest() throws Exception {
        List<CourtCounterServiceOpeningHours> invalidOpeningHours = List.of(
            CourtCounterServiceOpeningHours.builder()
                .id(UUID.randomUUID())
                .courtId(courtId)
                .dayOfWeek(DayOfTheWeek.MONDAY)
                .openingHour(null)
                .closingHour(LocalTime.of(17, 0))
                .build()
        );

        mockMvc.perform(put("/courts/{courtId}/v1/opening-hours/counter-service", courtId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(invalidOpeningHours)))
            .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("PUT /courts/{courtId}/v1/opening-hours/counter-service returns 400 for null closing hour")
    void setCounterServiceOpeningHoursNullClosingHourReturnsBadRequest() throws Exception {
        List<CourtCounterServiceOpeningHours> invalidOpeningHours = List.of(
            CourtCounterServiceOpeningHours.builder()
                .id(UUID.randomUUID())
                .courtId(courtId)
                .dayOfWeek(DayOfTheWeek.MONDAY)
                .openingHour(LocalTime.of(9, 0))
                .closingHour(null)
                .build()
        );

        mockMvc.perform(put("/courts/{courtId}/v1/opening-hours/counter-service", courtId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(invalidOpeningHours)))
            .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("DELETE /courts/{courtId}/v1/opening-hours/{openingHourTypeId} deletes opening hours successfully")
    void deleteOpeningHoursDeletesSuccessfully() throws Exception {
        mockMvc.perform(delete(
                "/courts/{courtId}/v1/opening-hours/{openingHourTypeId}",
                courtId, openingHourTypeId
            ))
            .andExpect(status().isOk());
    }

    @Test
    @DisplayName("DELETE /courts/{courtId}/v1/opening-hours/{openingHourTypeId} returns 404 if court does not exist")
    void deleteOpeningHoursNonExistentCourtReturnsNotFound() throws Exception {
        doThrow(new NotFoundException("Court not found"))
            .when(courtOpeningHoursService)
            .deleteCourtOpeningHours(nonExistentCourtId, openingHourTypeId);

        mockMvc.perform(delete(
                "/courts/{courtId}/v1/opening-hours/{openingHourTypeId}",
                nonExistentCourtId, openingHourTypeId
            ))
            .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("DELETE /courts/{courtId}/v1/opening-hours/{openingHourTypeId} returns 400 for invalid UUID")
    void deleteOpeningHoursInvalidUUID() throws Exception {
        mockMvc.perform(delete(
                "/courts/{courtId}/v1/opening-hours/{openingHourTypeId}",
                "invalid-uuid", openingHourTypeId
            ))
            .andExpect(status().isBadRequest());
    }

}
