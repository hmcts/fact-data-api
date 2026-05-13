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
import uk.gov.hmcts.reform.fact.data.api.services.CsvService;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Feature("CSV Controller")
@DisplayName("CSV Controller")
@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("test")
class CsvControllerTest {

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private CsvService csvService;

    @Test
    @DisplayName("POST /csv/ returns 200 when CSV is created and uploaded successfully")
    void createAndUploadCsvReturns200() throws Exception {
        doNothing().when(csvService).createAndUploadCsv();

        mvc.perform(post("/csv/"))
            .andExpect(status().isOk());
    }

    @Test
    @DisplayName("POST /csv/ propagates exception when CSV creation/upload fails")
    void createAndUploadCsvThrowsException() {
        doThrow(new RuntimeException("Failed to create or upload CSV file"))
            .when(csvService)
            .createAndUploadCsv();

        assertThatThrownBy(() -> mvc.perform(post("/csv/")))
            .hasRootCauseInstanceOf(RuntimeException.class)
            .hasMessageContaining("Failed to create or upload CSV file");
    }
}
