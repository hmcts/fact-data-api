package uk.gov.hmcts.reform.fact.data.api.controllers;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import uk.gov.hmcts.reform.fact.data.api.services.BlobStorageService;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;


@WebMvcTest(RootController.class)
class RootControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private BlobStorageService blobStorageService;

    @Test
    void shouldReturnWelcomeMessage() throws Exception {
        mockMvc.perform(get("/"))
            .andExpect(status().isOk())
            .andExpect(content().string("Welcome to fact-data-api"));
    }

    @Test
    void shouldReturnWelcomeMessageOnTest() throws Exception {
        mockMvc.perform(get("/test"))
            .andExpect(status().isOk())
            .andExpect(content().string("Test endpoint is working"));
    }
}
