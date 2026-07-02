package uk.gov.hmcts.reform.fact.data.api.controllers;

import io.qameta.allure.Feature;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import uk.gov.hmcts.reform.fact.data.api.clients.SlackClient;
import uk.gov.hmcts.reform.fact.data.api.services.AzureBlobService;
import uk.gov.hmcts.reform.fact.data.api.services.CourtDetailsViewService;
import uk.gov.hmcts.reform.fact.data.api.services.CourtService;

import java.util.Collections;

import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Feature("CSV Controller")
@DisplayName("CSV Controller")
@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("test")
class CsvControllerTest {

    @Autowired
    private MockMvc mvc;

    @MockitoBean(name = "csvAzureBlobService")
    private AzureBlobService azureBlobService;

    @MockitoBean
    private CourtService courtService;

    @MockitoBean
    private CourtDetailsViewService courtDetailsViewService;

    @MockitoBean
    private SlackClient slackClient;

    @Test
    @DisplayName("POST /csv/ returns 200 when CSV is created and uploaded successfully")
    void createAndUploadCsvReturns200() throws Exception {
        when(courtService.getAllCourtDetails()).thenReturn(Collections.emptyList());
        when(azureBlobService.uploadFile(anyString(), any())).thenReturn("http://example.com/blob");

        mvc.perform(post("/csv/"))
            .andExpect(status().isOk());

        verify(azureBlobService, times(1)).uploadFile(anyString(), any());
    }

    @Test
    @DisplayName("POST /csv/ returns 502 when Azure upload fails (e.g. container missing)")
    void createAndUploadCsvReturns502OnAzureUploadException() throws Exception {
        when(courtService.getAllCourtDetails()).thenReturn(Collections.emptyList());
        when(azureBlobService.uploadFile(anyString(), any())).thenThrow(new RuntimeException("Container not found"));

        mvc.perform(post("/csv/"))
            .andExpect(status().isBadGateway())
            .andExpect(jsonPath("$.message").value("Failed to upload CSV file to Azure Blob Storage"));
    }

    @Test
    @DisplayName("POST /csv/ returns 500 when CSV creation fails")
    void createAndUploadCsvReturns500OnCsvCreationException() throws Exception {
        when(courtService.getAllCourtDetails()).thenThrow(new RuntimeException("DB error"));

        mvc.perform(post("/csv/"))
            .andExpect(status().isInternalServerError())
            .andExpect(jsonPath("$.message").value("Failed to create CSV file"));
    }
}
