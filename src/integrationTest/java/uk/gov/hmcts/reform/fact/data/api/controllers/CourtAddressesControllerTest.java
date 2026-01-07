package uk.gov.hmcts.reform.fact.data.api.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import uk.gov.hmcts.reform.fact.data.api.entities.CourtAddress;
import uk.gov.hmcts.reform.fact.data.api.entities.types.AddressType;
import uk.gov.hmcts.reform.fact.data.api.errorhandling.exceptions.NotFoundException;
import uk.gov.hmcts.reform.fact.data.api.services.CourtAddressesService;

import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;


@WebMvcTest(CourtAddressesController.class)
class CourtAddressesControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private CourtAddressesService courtAddressesService;

    private static final String ADDRESSES_V1_PATH = "/courts/{courtId}/v1/addresses";
    private static final String ADDRESS_V1_PATH = "/courts/{courtId}/v1/addresses/{addressId}";

    private final UUID courtId = UUID.fromString("123e4567-e89b-12d3-a456-426614174000");
    private final UUID addressId = UUID.fromString("223e4567-e89b-12d3-a456-426614174111");

    private CourtAddress buildAddress() {
        return CourtAddress.builder()
            .id(addressId)
            .courtId(courtId)
            .addressType(AddressType.VISIT_US)
            .addressLine1("123 Test Street")
            .build();
    }

    @Test
    @DisplayName("GET /courts/{courtId}/v1/addresses returns addresses successfully")
    void getAddressesReturnsSuccessfully() throws Exception {
        when(courtAddressesService.getAddresses(courtId))
            .thenReturn(List.of(buildAddress()));

        mockMvc.perform(get(ADDRESSES_V1_PATH, courtId))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].id").value(addressId.toString()))
            .andExpect(jsonPath("$[0].addressLine1").value("123 Test Street"));
    }

    @Test
    @DisplayName("GET /courts/{courtId}/v1/addresses returns 404 when court not found")
    void getAddressesReturnsNotFoundForUnknownCourt() throws Exception {
        when(courtAddressesService.getAddresses(courtId))
            .thenThrow(new NotFoundException("Court not found"));

        mockMvc.perform(get(ADDRESSES_V1_PATH, courtId))
            .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("GET /courts/{courtId}/v1/addresses returns 400 for invalid court ID")
    void getAddressesInvalidUUID() throws Exception {
        mockMvc.perform(get(ADDRESSES_V1_PATH, "invalid"))
            .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("GET /courts/{courtId}/v1/addresses/{addressId} returns address successfully")
    void getAddressReturnsSuccessfully() throws Exception {
        when(courtAddressesService.getAddress(courtId, addressId))
            .thenReturn(buildAddress());

        mockMvc.perform(get(ADDRESS_V1_PATH, courtId, addressId))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(addressId.toString()));
    }

    @Test
    @DisplayName("GET /courts/{courtId}/v1/addresses/{addressId} returns 404 when not found")
    void getAddressReturnsNotFound() throws Exception {
        when(courtAddressesService.getAddress(courtId, addressId))
            .thenThrow(new NotFoundException("Address not found"));

        mockMvc.perform(get(ADDRESS_V1_PATH, courtId, addressId))
            .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("GET /courts/{courtId}/v1/addresses/{addressId} returns 400 for invalid ID")
    void getAddressInvalidUUID() throws Exception {
        mockMvc.perform(get(ADDRESS_V1_PATH, "invalid", addressId))
            .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /courts/{courtId}/v1/addresses creates address successfully")
    void createAddressReturnsCreated() throws Exception {
        CourtAddress request = buildAddress();
        request.setId(UUID.randomUUID());

        when(courtAddressesService.createAddress(any(UUID.class), any(CourtAddress.class)))
            .thenReturn(buildAddress());

        mockMvc.perform(post(ADDRESSES_V1_PATH, courtId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.addressLine1").value("123 Test Street"));
    }

    @Test
    @DisplayName("POST /courts/{courtId}/v1/addresses returns 404 when court not found")
    void createAddressReturnsNotFound() throws Exception {
        CourtAddress request = buildAddress();
        request.setId(null);
        request.setAddressLine1("123 Test Street");
        request.setAddressType(AddressType.VISIT_US);

        when(courtAddressesService.createAddress(any(UUID.class), any(CourtAddress.class)))
            .thenThrow(new NotFoundException("Court not found"));

        mockMvc.perform(post(ADDRESSES_V1_PATH, courtId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("POST /courts/{courtId}/v1/addresses returns 400 for null address type")
    void createAddressNullAddressType() throws Exception {
        CourtAddress request = buildAddress();
        request.setId(null);
        request.setAddressType(null);

        mockMvc.perform(post(ADDRESSES_V1_PATH, courtId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /courts/{courtId}/v1/addresses returns 400 when address line is too long")
    void createAddressAddressLineTooLong() throws Exception {
        CourtAddress request = buildAddress();
        request.setId(null);
        request.setAddressLine1("a".repeat(256));

        mockMvc.perform(post(ADDRESSES_V1_PATH, courtId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("PUT /courts/{courtId}/v1/addresses/{addressId} updates address successfully")
    void updateAddressReturnsOk() throws Exception {
        CourtAddress request = buildAddress();
        request.setAddressLine1("Updated address");

        when(courtAddressesService.updateAddress(
            any(UUID.class),
            any(UUID.class),
            any(CourtAddress.class)
        )).thenReturn(request);

        mockMvc.perform(put(ADDRESS_V1_PATH, courtId, addressId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.addressLine1").value("Updated address"));
    }

    @Test
    @DisplayName("PUT /courts/{courtId}/v1/addresses/{addressId} returns 404 when contact not found")
    void updateAddressReturnsNotFound() throws Exception {
        CourtAddress request = buildAddress();

        when(courtAddressesService.updateAddress(
            any(UUID.class),
            any(UUID.class),
            any(CourtAddress.class)
        )).thenThrow(new NotFoundException("Contact not found"));

        mockMvc.perform(put(ADDRESS_V1_PATH, courtId, addressId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("PUT /courts/{courtId}/v1/addresses/{addressId} returns 400 for invalid postcode")
    void updateAddressInvalidPostcode() throws Exception {
        CourtAddress request = buildAddress();
        request.setPostcode("123 456 789");

        mockMvc.perform(put(ADDRESS_V1_PATH, courtId, addressId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("DELETE /courts/{courtId}/v1/addresses/{addressId} deletes address successfully")
    void deleteAddressReturnsNoContent() throws Exception {
        doNothing().when(courtAddressesService).deleteAddress(courtId, addressId);

        mockMvc.perform(delete(ADDRESS_V1_PATH, courtId, addressId))
            .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("DELETE /courts/{courtId}/v1/addresses/{addressId} returns 404 when address not found")
    void deleteAddressReturnsNotFound() throws Exception {
        doThrow(new NotFoundException("Address not found"))
            .when(courtAddressesService).deleteAddress(courtId, addressId);

        mockMvc.perform(delete(ADDRESS_V1_PATH, courtId, addressId))
            .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("DELETE /courts/{courtId}/v1/addresses/{addressId} returns 400 for invalid UUID")
    void deleteAddressInvalidUUID() throws Exception {
        mockMvc.perform(delete(ADDRESS_V1_PATH, "invalid", addressId))
            .andExpect(status().isBadRequest());
    }
}


