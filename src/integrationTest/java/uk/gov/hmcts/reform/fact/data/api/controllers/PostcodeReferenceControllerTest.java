package uk.gov.hmcts.reform.fact.data.api.controllers;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import uk.gov.hmcts.reform.fact.data.api.errorhandling.exceptions.PostcodeReferenceImportException;
import uk.gov.hmcts.reform.fact.data.api.models.PostcodeReferenceImportResult;
import uk.gov.hmcts.reform.fact.data.api.services.PostcodeReferenceImportService;

import java.time.Instant;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("test")
class PostcodeReferenceControllerTest {

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private PostcodeReferenceImportService importService;

    @Test
    void importsPostcodeReferenceData() throws Exception {
        when(importService.importLatest()).thenReturn(new PostcodeReferenceImportResult(
            true,
            "item-id:12345",
            "2026-08",
            993_979,
            Instant.parse("2026-10-16T09:00:00Z")
        ));

        mvc.perform(post("/postcode-reference/v1/import"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.imported").value(true))
            .andExpect(jsonPath("$.sourceVersion").value("item-id:12345"))
            .andExpect(jsonPath("$.release").value("2026-08"))
            .andExpect(jsonPath("$.recordCount").value(993_979));
    }

    @Test
    void returnsInternalServerErrorWhenImportFails() throws Exception {
        when(importService.importLatest()).thenThrow(new PostcodeReferenceImportException("Import failed"));

        mvc.perform(post("/postcode-reference/v1/import"))
            .andExpect(status().isInternalServerError())
            .andExpect(jsonPath("$.message").value("Unable to import ONSPD postcode reference data."));
    }
}
