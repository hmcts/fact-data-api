package uk.gov.hmcts.reform.fact.data.api.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import uk.gov.hmcts.reform.fact.data.api.entities.CourtFacilities;
import uk.gov.hmcts.reform.fact.data.api.errorhandling.exceptions.CourtResourceNotFoundException;
import uk.gov.hmcts.reform.fact.data.api.errorhandling.exceptions.NotFoundException;
import uk.gov.hmcts.reform.fact.data.api.services.CourtFacilitiesService;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(CourtFacilitiesController.class)
class CourtFacilitiesControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private CourtFacilitiesService courtFacilitiesService;

    private final UUID courtId = UUID.fromString("123e4567-e89b-12d3-a456-426614174000");
    private final UUID nonExistentCourtId = UUID.fromString("11111111-1111-1111-1111-111111111111");

    @Test
    @DisplayName("GET /courts/{courtId}/v1/building-facilities returns facilities successfully")
    void getFacilitiesReturnsSuccessfully() throws Exception {
        CourtFacilities facilities = CourtFacilities.builder()
            .id(courtId)
            .courtId(courtId)
            .court(null)
            .freeWaterDispensers(true)
            .snackVendingMachines(false)
            .drinkVendingMachines(true)
            .cafeteria(false)
            .waitingArea(true)
            .waitingAreaChildren(false)
            .quietRoom(true)
            .wifi(false)
            .babyChanging(true)
            .parking(false)
            .build();

        when(courtFacilitiesService.getFacilitiesByCourtId(courtId)).thenReturn(facilities);

        mockMvc.perform(get("/courts/{courtId}/v1/building-facilities", courtId))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.babyChanging").value(true))
            .andExpect(jsonPath("$.parking").value(false));
    }

    @Test
    @DisplayName("GET /courts/{courtId}/v1/building-facilities returns 404 if court does not exist")
    void getCourtNonExistentReturnsNotFound() throws Exception {
        when(courtFacilitiesService.getFacilitiesByCourtId(nonExistentCourtId))
            .thenThrow(new NotFoundException("Court not found"));

        mockMvc.perform(get("/courts/{courtId}/v1/building-facilities", nonExistentCourtId))
            .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("GET /courts/{courtId}/v1/building-facilities returns 204 if facilities does not exist")
    void getFacilitiesNonExistentCourtReturnsNoContent() throws Exception {
        when(courtFacilitiesService.getFacilitiesByCourtId(courtId))
            .thenThrow(new CourtResourceNotFoundException("Facilities not found"));

        mockMvc.perform(get("/courts/{courtId}/v1/building-facilities", courtId))
            .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("GET /courts/{courtId}/v1/building-facilities returns 400 for invalid UUID")
    void getFacilitiesInvalidUUID() throws Exception {
        mockMvc.perform(get("/courts/{courtId}/v1/building-facilities", "invalid-uuid"))
            .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /courts/{courtId}/v1/building-facilities creates facilities successfully")
    void postFacilitiesCreatesSuccessfully() throws Exception {
        CourtFacilities facilities = CourtFacilities.builder()
            .id(courtId)
            .courtId(courtId)
            .court(null)
            .freeWaterDispensers(true)
            .snackVendingMachines(false)
            .drinkVendingMachines(true)
            .cafeteria(false)
            .waitingArea(true)
            .waitingAreaChildren(false)
            .quietRoom(true)
            .wifi(false)
            .babyChanging(true)
            .parking(false)
            .build();

        when(courtFacilitiesService.setFacilities(any(UUID.class),
                                                    any(CourtFacilities.class))).thenReturn(facilities);

        mockMvc.perform(post("/courts/{courtId}/v1/building-facilities", courtId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(facilities)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.babyChanging").value(true))
            .andExpect(jsonPath("$.parking").value(false));
    }

    @Test
    @DisplayName("POST /courts/{courtId}/v1/building-facilities returns 404 if court does not exist")
    void postFacilitiesNonExistentCourtReturnsNotFound() throws Exception {
        CourtFacilities facilities = CourtFacilities.builder()
            .id(nonExistentCourtId)
            .courtId(nonExistentCourtId)
            .freeWaterDispensers(true)
            .snackVendingMachines(false)
            .drinkVendingMachines(true)
            .cafeteria(false)
            .waitingArea(true)
            .waitingAreaChildren(false)
            .quietRoom(true)
            .wifi(false)
            .babyChanging(true)
            .parking(false)
            .build();

        when(courtFacilitiesService.setFacilities(any(UUID.class), any(CourtFacilities.class)))
            .thenThrow(new NotFoundException("Court not found"));

        mockMvc.perform(post("/courts/{courtId}/v1/building-facilities", nonExistentCourtId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(facilities)))
            .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("POST /courts/{courtId}/v1/building-facilities returns 400 for invalid UUID")
    void postFacilitiesInvalidUUID() throws Exception {
        CourtFacilities facilities = CourtFacilities.builder()
            .id(courtId)
            .courtId(courtId)
            .court(null)
            .freeWaterDispensers(true)
            .snackVendingMachines(false)
            .drinkVendingMachines(true)
            .cafeteria(false)
            .waitingArea(true)
            .waitingAreaChildren(false)
            .quietRoom(true)
            .wifi(false)
            .babyChanging(true)
            .parking(false)
            .build();

        mockMvc.perform(post("/courts/{courtId}/v1/building-facilities", "invalid-uuid")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(facilities)))
            .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /courts/{courtId}/v1/building-facilities returns 400 for null freeWaterDispensers")
    void postFacilitiesNullFreeWaterDispensersReturnsBadRequest() throws Exception {
        CourtFacilities invalid = CourtFacilities.builder()
            .id(courtId)
            .courtId(courtId)
            .court(null)
            .freeWaterDispensers(null)
            .snackVendingMachines(false)
            .drinkVendingMachines(true)
            .cafeteria(false)
            .waitingArea(true)
            .waitingAreaChildren(false)
            .quietRoom(true)
            .wifi(false)
            .babyChanging(true)
            .parking(false)
            .build();

        mockMvc.perform(post("/courts/{courtId}/v1/building-facilities", courtId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(invalid)))
            .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /courts/{courtId}/v1/building-facilities returns 400 for null snackVendingMachines")
    void postFacilitiesNullSnackVendingMachinesReturnsBadRequest() throws Exception {
        CourtFacilities invalid = CourtFacilities.builder()
            .id(courtId)
            .courtId(courtId)
            .court(null)
            .freeWaterDispensers(true)
            .snackVendingMachines(null)
            .drinkVendingMachines(true)
            .cafeteria(false)
            .waitingArea(true)
            .waitingAreaChildren(false)
            .quietRoom(true)
            .wifi(false)
            .babyChanging(true)
            .parking(false)
            .build();

        mockMvc.perform(post("/courts/{courtId}/v1/building-facilities", courtId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(invalid)))
            .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /courts/{courtId}/v1/building-facilities returns 400 for null drinkVendingMachines")
    void postFacilitiesNullDrinkVendingMachinesReturnsBadRequest() throws Exception {
        CourtFacilities invalid = CourtFacilities.builder()
            .id(courtId)
            .courtId(courtId)
            .court(null)
            .freeWaterDispensers(true)
            .snackVendingMachines(true)
            .drinkVendingMachines(null)
            .cafeteria(false)
            .waitingArea(true)
            .waitingAreaChildren(false)
            .quietRoom(true)
            .wifi(false)
            .babyChanging(true)
            .parking(false)
            .build();

        mockMvc.perform(post("/courts/{courtId}/v1/building-facilities", courtId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(invalid)))
            .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /courts/{courtId}/v1/building-facilities returns 400 for null cafeteria")
    void postFacilitiesNullCafeteriaReturnsBadRequest() throws Exception {
        CourtFacilities invalid = CourtFacilities.builder()
            .id(courtId)
            .courtId(courtId)
            .court(null)
            .freeWaterDispensers(true)
            .snackVendingMachines(true)
            .drinkVendingMachines(false)
            .cafeteria(null)
            .waitingArea(true)
            .waitingAreaChildren(false)
            .quietRoom(true)
            .wifi(false)
            .babyChanging(true)
            .parking(false)
            .build();

        mockMvc.perform(post("/courts/{courtId}/v1/building-facilities", courtId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(invalid)))
            .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /courts/{courtId}/v1/building-facilities returns 400 for null waitingArea")
    void postFacilitiesNullWaitingAreaReturnsBadRequest() throws Exception {
        CourtFacilities invalid = CourtFacilities.builder()
            .id(courtId)
            .courtId(courtId)
            .court(null)
            .freeWaterDispensers(true)
            .snackVendingMachines(true)
            .drinkVendingMachines(false)
            .cafeteria(true)
            .waitingArea(null)
            .waitingAreaChildren(false)
            .quietRoom(true)
            .wifi(false)
            .babyChanging(true)
            .parking(false)
            .build();

        mockMvc.perform(post("/courts/{courtId}/v1/building-facilities", courtId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(invalid)))
            .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /courts/{courtId}/v1/building-facilities returns 400 for null waitingAreaChildren")
    void postFacilitiesNullWaitingAreaChildrenReturnsBadRequest() throws Exception {
        CourtFacilities invalid = CourtFacilities.builder()
            .id(courtId)
            .courtId(courtId)
            .court(null)
            .freeWaterDispensers(true)
            .snackVendingMachines(true)
            .drinkVendingMachines(false)
            .cafeteria(true)
            .waitingArea(false)
            .waitingAreaChildren(null)
            .quietRoom(true)
            .wifi(false)
            .babyChanging(true)
            .parking(false)
            .build();

        mockMvc.perform(post("/courts/{courtId}/v1/building-facilities", courtId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(invalid)))
            .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /courts/{courtId}/v1/building-facilities returns 400 for null quietRoom")
    void postFacilitiesNullQuietRoomReturnsBadRequest() throws Exception {
        CourtFacilities invalid = CourtFacilities.builder()
            .id(courtId)
            .courtId(courtId)
            .court(null)
            .freeWaterDispensers(true)
            .snackVendingMachines(true)
            .drinkVendingMachines(false)
            .cafeteria(true)
            .waitingArea(false)
            .waitingAreaChildren(true)
            .quietRoom(null)
            .wifi(false)
            .babyChanging(true)
            .parking(false)
            .build();

        mockMvc.perform(post("/courts/{courtId}/v1/building-facilities", courtId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(invalid)))
            .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /courts/{courtId}/v1/building-facilities returns 400 for null babyChanging")
    void postFacilitiesNullBabyChangingReturnsBadRequest() throws Exception {
        CourtFacilities invalid = CourtFacilities.builder()
            .id(courtId)
            .courtId(courtId)
            .court(null)
            .freeWaterDispensers(true)
            .snackVendingMachines(true)
            .drinkVendingMachines(false)
            .cafeteria(true)
            .waitingArea(false)
            .waitingAreaChildren(true)
            .quietRoom(true)
            .wifi(false)
            .babyChanging(null)
            .parking(false)
            .build();

        mockMvc.perform(post("/courts/{courtId}/v1/building-facilities", courtId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(invalid)))
            .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /courts/{courtId}/v1/building-facilities returns 400 for null wifi")
    void postFacilitiesNullWifiReturnsBadRequest() throws Exception {
        CourtFacilities invalid = CourtFacilities.builder()
            .id(courtId)
            .courtId(courtId)
            .court(null)
            .freeWaterDispensers(true)
            .snackVendingMachines(true)
            .drinkVendingMachines(false)
            .cafeteria(true)
            .waitingArea(false)
            .waitingAreaChildren(true)
            .quietRoom(true)
            .wifi(null)
            .babyChanging(true)
            .parking(false)
            .build();

        mockMvc.perform(post("/courts/{courtId}/v1/building-facilities", courtId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(invalid)))
            .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /courts/{courtId}/v1/building-facilities returns 400 for null parking")
    void postFacilitiesNullParkingReturnsBadRequest() throws Exception {
        CourtFacilities invalid = CourtFacilities.builder()
            .id(courtId)
            .courtId(courtId)
            .court(null)
            .freeWaterDispensers(true)
            .snackVendingMachines(true)
            .drinkVendingMachines(false)
            .cafeteria(true)
            .waitingArea(false)
            .waitingAreaChildren(true)
            .quietRoom(true)
            .wifi(false)
            .babyChanging(true)
            .parking(null)
            .build();

        mockMvc.perform(post("/courts/{courtId}/v1/building-facilities", courtId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(invalid)))
            .andExpect(status().isBadRequest());
    }


}
