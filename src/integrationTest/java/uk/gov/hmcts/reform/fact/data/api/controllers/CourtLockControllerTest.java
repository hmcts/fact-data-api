package uk.gov.hmcts.reform.fact.data.api.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import uk.gov.hmcts.reform.fact.data.api.entities.CourtLock;
import uk.gov.hmcts.reform.fact.data.api.entities.types.Page;
import uk.gov.hmcts.reform.fact.data.api.errorhandling.exceptions.NotFoundException;
import uk.gov.hmcts.reform.fact.data.api.services.CourtLockService;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(CourtLockController.class)
class CourtLockControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private CourtLockService courtLockService;

    private final UUID userId = UUID.fromString("123e4567-e89b-12d3-a456-426614174000");
    private final UUID courtId = UUID.fromString("222e4567-e89b-12d3-a456-426614174000");
    private final UUID nonExistentCourtId = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private final Page testPage = Page.COURT;

    @Test
    @DisplayName("GET /courts/{courtId}/v1/locks returns court locks successfully")
    void getCourtLocksReturnsSuccessfully() throws Exception {
        List<CourtLock> locks = List.of(new CourtLock());
        when(courtLockService.getLocksByCourtId(courtId)).thenReturn(locks);

        mockMvc.perform(get("/courts/{courtId}/v1/locks", courtId))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$").isArray());
    }

    @Test
    @DisplayName("GET /courts/{courtId}/v1/locks returns 404 if court does not exist")
    void getCourtLocksNonExistentReturnsNotFound() throws Exception {
        when(courtLockService.getLocksByCourtId(nonExistentCourtId))
            .thenThrow(new NotFoundException("Court not found"));

        mockMvc.perform(get("/courts/{courtId}/v1/locks", nonExistentCourtId))
            .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("GET /courts/{courtId}/v1/locks returns 400 for invalid UUID")
    void getCourtLocksInvalidUUID() throws Exception {
        mockMvc.perform(get("/courts/{courtId}/v1/locks", "invalid-uuid"))
            .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("GET /courts/{courtId}/v1/locks/{page} returns lock status successfully")
    void getCourtLockStatusReturnsSuccessfully() throws Exception {
        CourtLock lock = new CourtLock();
        when(courtLockService.getPageLock(courtId, testPage)).thenReturn(Optional.of(lock));

        mockMvc.perform(get("/courts/{courtId}/v1/locks/{page}", courtId, testPage))
            .andExpect(status().isOk());
    }

    @Test
    @DisplayName("GET /courts/{courtId}/v1/locks/{page} returns 404 if court not found")
    void getCourtLockStatusNonExistentReturnsNotFound() throws Exception {
        when(courtLockService.getPageLock(nonExistentCourtId, testPage))
            .thenThrow(new NotFoundException("Court not found"));

        mockMvc.perform(get("/courts/{courtId}/v1/locks/{page}", nonExistentCourtId, testPage))
            .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("POST /courts/{courtId}/v1/locks/{page} creates lock successfully")
    void createCourtLockSuccessfully() throws Exception {
        CourtLock lock = new CourtLock();
        when(courtLockService.createOrUpdateLock(courtId, testPage, userId)).thenReturn(lock);

        mockMvc.perform(post("/courts/{courtId}/v1/locks/{page}", courtId, testPage)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(userId)))
            .andExpect(status().isCreated());
    }

    @Test
    @DisplayName("POST /courts/{courtId}/v1/locks/{page} returns 404 if court not found")
    void createCourtLockNonExistentReturnsNotFound() throws Exception {
        when(courtLockService.createOrUpdateLock(nonExistentCourtId, testPage, userId))
            .thenThrow(new NotFoundException("Court not found"));

        mockMvc.perform(post("/courts/{courtId}/v1/locks/{page}", nonExistentCourtId, testPage)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(userId)))
            .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("DELETE /courts/{courtId}/v1/locks/{page} deletes lock successfully")
    void deleteCourtLockSuccessfully() throws Exception {
        mockMvc.perform(delete("/courts/{courtId}/v1/locks/{page}", courtId, testPage))
            .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("DELETE /courts/{courtId}/v1/locks/{page} returns 404 if court or lock not found")
    void deleteCourtLockNonExistentReturnsNotFound() throws Exception {
        doThrow(new NotFoundException("Court or lock not found"))
            .when(courtLockService).deleteLock(nonExistentCourtId, testPage);

        mockMvc.perform(delete("/courts/{courtId}/v1/locks/{page}", nonExistentCourtId, testPage))
            .andExpect(status().isNotFound());
    }
}
