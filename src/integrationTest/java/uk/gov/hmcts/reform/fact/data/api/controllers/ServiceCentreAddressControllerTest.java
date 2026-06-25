package uk.gov.hmcts.reform.fact.data.api.controllers;

import io.qameta.allure.Feature;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;
import uk.gov.hmcts.reform.fact.data.api.entities.ServiceCentreAddress;
import uk.gov.hmcts.reform.fact.data.api.entities.types.AddressType;
import uk.gov.hmcts.reform.fact.data.api.errorhandling.exceptions.NotFoundException;
import uk.gov.hmcts.reform.fact.data.api.services.ServiceCentreAddressService;

import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Feature("Service Centre Address Controller")
@DisplayName("Service Centre Address Controller")
@WebMvcTest(ServiceCentreAddressController.class)
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("test")
class ServiceCentreAddressControllerTest {

    private static final UUID SERVICE_CENTRE_ID = UUID.fromString("123e4567-e89b-12d3-a456-426614174000");
    private static final UUID ADDRESS_ID = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private ServiceCentreAddressService serviceCentreAddressService;

    @Test
    void getAddressesReturnsAddresses() throws Exception {
        ServiceCentreAddress address = buildAddress();
        when(serviceCentreAddressService.getAddresses(SERVICE_CENTRE_ID)).thenReturn(List.of(address));

        mockMvc.perform(get("/service-centres/{serviceCentreId}/v1/address", SERVICE_CENTRE_ID))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].id").value(ADDRESS_ID.toString()));
    }

    @Test
    void getAddressReturnsAddress() throws Exception {
        ServiceCentreAddress address = buildAddress();
        when(serviceCentreAddressService.getAddress(SERVICE_CENTRE_ID, ADDRESS_ID)).thenReturn(address);

        mockMvc.perform(get("/service-centres/{serviceCentreId}/v1/address/{addressId}",
                            SERVICE_CENTRE_ID,
                            ADDRESS_ID))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(ADDRESS_ID.toString()));
    }

    @Test
    void getAddressReturnsBadRequestForInvalidUuid() throws Exception {
        mockMvc.perform(get("/service-centres/{serviceCentreId}/v1/address/{addressId}",
                            SERVICE_CENTRE_ID,
                            "invalid-uuid"))
            .andExpect(status().isBadRequest());
    }

    @Test
    void postAddressCreatesAddress() throws Exception {
        ServiceCentreAddress address = buildAddress();
        when(serviceCentreAddressService.createAddress(eq(SERVICE_CENTRE_ID), any(ServiceCentreAddress.class)))
            .thenReturn(address);

        mockMvc.perform(post("/service-centres/{serviceCentreId}/v1/address", SERVICE_CENTRE_ID)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(address)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.id").value(ADDRESS_ID.toString()));
    }

    @Test
    void putAddressUpdatesAddress() throws Exception {
        ServiceCentreAddress address = buildAddress();
        when(serviceCentreAddressService.updateAddress(
            eq(SERVICE_CENTRE_ID),
            eq(ADDRESS_ID),
            any(ServiceCentreAddress.class)
        )).thenReturn(address);

        mockMvc.perform(put("/service-centres/{serviceCentreId}/v1/address/{addressId}",
                            SERVICE_CENTRE_ID,
                            ADDRESS_ID)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(address)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(ADDRESS_ID.toString()));
    }

    @Test
    void deleteAddressReturnsNoContent() throws Exception {
        mockMvc.perform(delete("/service-centres/{serviceCentreId}/v1/address/{addressId}",
                               SERVICE_CENTRE_ID,
                               ADDRESS_ID))
            .andExpect(status().isNoContent());
    }

    @Test
    void deleteAddressReturnsNotFound() throws Exception {
        doThrow(new NotFoundException("Missing"))
            .when(serviceCentreAddressService).deleteAddress(SERVICE_CENTRE_ID, ADDRESS_ID);

        mockMvc.perform(delete("/service-centres/{serviceCentreId}/v1/address/{addressId}",
                               SERVICE_CENTRE_ID,
                               ADDRESS_ID))
            .andExpect(status().isNotFound());
    }

    private ServiceCentreAddress buildAddress() {
        return ServiceCentreAddress.builder()
            .id(ADDRESS_ID)
            .serviceCentreId(SERVICE_CENTRE_ID)
            .addressLine1("1 Test Street")
            .townCity("London")
            .postcode("SW1A 1AA")
            .addressType(AddressType.VISIT_OR_CONTACT_US)
            .build();
    }
}
