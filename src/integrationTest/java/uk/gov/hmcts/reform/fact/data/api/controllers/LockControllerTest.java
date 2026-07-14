package uk.gov.hmcts.reform.fact.data.api.controllers;

import tools.jackson.databind.ObjectMapper;
import io.qameta.allure.Feature;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import uk.gov.hmcts.reform.fact.data.api.entities.Lock;
import uk.gov.hmcts.reform.fact.data.api.entities.types.SubjectType;
import uk.gov.hmcts.reform.fact.data.api.entities.types.Page;
import uk.gov.hmcts.reform.fact.data.api.errorhandling.exceptions.NotFoundException;
import uk.gov.hmcts.reform.fact.data.api.services.LockService;

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

@Feature("Court Local Controller")
@DisplayName("Court Local Controller")
@WebMvcTest(LockController.class)
@AutoConfigureMockMvc(addFilters = false)
class LockControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private LockService lockService;

    private final UUID userId = UUID.fromString("123e4567-e89b-12d3-a456-426614174000");
    private final UUID courtId = UUID.fromString("222e4567-e89b-12d3-a456-426614174000");
    private final UUID nonExistentCourtId = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private final Page testPage = Page.GENERAL;

    @Test
    @DisplayName("GET /locks/{subjectType}/{subjectId}/v1/returns court locks successfully")
    void getCourtLocksReturnsSuccessfully() throws Exception {
        List<Lock> locks = List.of(new Lock());
        when(lockService.getAllSubjectLocks(SubjectType.COURT, courtId)).thenReturn(locks);

        mockMvc.perform(get("/locks/{subjectType}/{subjectId}/v1", SubjectType.COURT, courtId))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$").isArray());
    }

    @Test
    @DisplayName("GET /locks/{subjectType}/{subjectId}/v1/returns 404 if court does not exist")
    void getCourtLocksNonExistentReturnsNotFound() throws Exception {
        when(lockService.getAllSubjectLocks(SubjectType.COURT, nonExistentCourtId))
            .thenThrow(new NotFoundException("Court not found"));

        mockMvc.perform(get("/locks/{subjectType}/{subjectId}/v1", SubjectType.COURT, nonExistentCourtId))
            .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("GET /locks/{subjectType}/{subjectId}/v1/returns 400 for invalid UUID")
    void getCourtLocksInvalidUUID() throws Exception {
        mockMvc.perform(get("/locks/{subjectType}/{subjectId}/v1", SubjectType.COURT,  "invalid-uuid"))
            .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("GET /locks/{subjectType}/{subjectId}/v1/{page} returns lock status successfully")
    void getCourtLockStatusReturnsSuccessfully() throws Exception {
        Lock lock = new Lock();
        when(lockService.getPageLock(SubjectType.COURT, courtId, testPage)).thenReturn(Optional.of(lock));

        mockMvc.perform(get("/locks/{subjectType}/{subjectId}/v1/{page}", SubjectType.COURT, courtId, testPage))
            .andExpect(status().isOk());
    }

    @Test
    @DisplayName("GET /locks/{subjectType}/{subjectId}/v1/{page} returns 404 if court not found")
    void getCourtLockStatusNonExistentReturnsNotFound() throws Exception {
        when(lockService.getPageLock(SubjectType.COURT, nonExistentCourtId, testPage))
            .thenThrow(new NotFoundException("Court not found"));

        mockMvc.perform(get("/locks/{subjectType}/{subjectId}/v1/{page}",
                            SubjectType.COURT, nonExistentCourtId, testPage))
            .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("POST /locks/{subjectType}/{subjectId}/v1/{page} creates lock successfully")
    void createCourtLockSuccessfully() throws Exception {
        Lock lock = new Lock();
        when(lockService.createOrUpdateLock(SubjectType.COURT, courtId, testPage, userId)).thenReturn(lock);

        mockMvc.perform(post("/locks/{subjectType}/{subjectId}/v1/{page}", SubjectType.COURT, courtId, testPage)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(userId)))
            .andExpect(status().isCreated());
    }

    @Test
    @DisplayName("POST /locks/{subjectType}/{subjectId}/v1/{page} returns 404 if court not found")
    void createCourtLockNonExistentReturnsNotFound() throws Exception {
        when(lockService.createOrUpdateLock(SubjectType.COURT, nonExistentCourtId, testPage, userId))
            .thenThrow(new NotFoundException("Court not found"));

        mockMvc.perform(post("/locks/{subjectType}/{subjectId}/v1/{page}",
                             SubjectType.COURT, nonExistentCourtId, testPage)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(userId)))
            .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("DELETE /locks/{subjectType}/{subjectId}/v1/{page} deletes lock successfully")
    void deleteCourtLockSuccessfully() throws Exception {
        mockMvc.perform(delete("/locks/{subjectType}/{subjectId}/v1/{page}",
                               SubjectType.COURT, courtId, testPage))
            .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("DELETE /locks/{subjectType}/{subjectId}/v1/{page} returns 404 if court or lock not found")
    void deleteCourtLockNonExistentReturnsNotFound() throws Exception {
        doThrow(new NotFoundException("Court or lock not found"))
            .when(lockService).deleteLock(SubjectType.COURT, nonExistentCourtId, testPage);

        mockMvc.perform(delete("/locks/{subjectType}/{subjectId}/v1/{page}",
                               SubjectType.COURT, nonExistentCourtId, testPage))
            .andExpect(status().isNotFound());
    }
}
