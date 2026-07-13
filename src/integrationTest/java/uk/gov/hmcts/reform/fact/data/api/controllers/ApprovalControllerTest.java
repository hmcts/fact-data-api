package uk.gov.hmcts.reform.fact.data.api.controllers;

import tools.jackson.databind.ObjectMapper;
import uk.gov.hmcts.reform.fact.data.api.dto.ApprovalStatus;
import uk.gov.hmcts.reform.fact.data.api.entities.Approval;
import uk.gov.hmcts.reform.fact.data.api.entities.User;
import uk.gov.hmcts.reform.fact.data.api.entities.types.AuditSubjectType;
import uk.gov.hmcts.reform.fact.data.api.errorhandling.exceptions.NotFoundException;
import uk.gov.hmcts.reform.fact.data.api.services.ApprovalService;

import java.util.List;
import java.util.UUID;

import io.qameta.allure.Feature;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Feature("Approval Controller")
@DisplayName("Approval Controller")
@WebMvcTest(ApprovalController.class)
@AutoConfigureMockMvc(addFilters = false)
class ApprovalControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private ApprovalService approvalService;

    private final UUID approvalId = UUID.fromString("123e4567-e89b-12d3-a456-426614174000");
    private final UUID subjectId = UUID.fromString("222e4567-e89b-12d3-a456-426614174000");
    private final UUID userId = UUID.fromString("333e4567-e89b-12d3-a456-426614174000");

    @Test
    @DisplayName("GET /approvals/v1 returns approvals successfully")
    void getAllApprovalsReturnsSuccessfully() throws Exception {
        when(approvalService.getAllApprovalStatuses()).thenReturn(List.of(createApprovalStatus()));

        mockMvc.perform(get("/approvals/v1"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$").isArray())
            .andExpect(jsonPath("$[0].subjectId").value(subjectId.toString()))
            .andExpect(jsonPath("$[0].subjectType").value(AuditSubjectType.COURT.name()))
            .andExpect(jsonPath("$[0].name").value("Test Court"))
            .andExpect(jsonPath("$[0].approved").value(true))
            .andExpect(jsonPath("$[0].approvalId").value(approvalId.toString()))
            .andExpect(jsonPath("$[0].userId").value(userId.toString()))
            .andExpect(jsonPath("$[0].user.id").value(userId.toString()))
            .andExpect(jsonPath("$[0].user.email").value("approver@justice.gov.uk"));
    }

    @Test
    @DisplayName("POST /approvals/v1 creates approval successfully")
    void createApprovalSuccessfully() throws Exception {
        Approval approval = createApproval();
        when(approvalService.createApproval(any(Approval.class))).thenReturn(approval);

        mockMvc.perform(post("/approvals/v1")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(approval)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.id").value(approvalId.toString()));
    }

    @Test
    @DisplayName("POST /approvals/v1 returns 400 for invalid request body")
    void createApprovalWithInvalidBodyReturnsBadRequest() throws Exception {
        Approval approval = createApproval();
        approval.setSubjectId(null);

        mockMvc.perform(post("/approvals/v1")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(approval)))
            .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /approvals/v1 returns 404 when user or subject does not exist")
    void createApprovalWhenReferenceDoesNotExistReturnsNotFound() throws Exception {
        Approval approval = createApproval();
        when(approvalService.createApproval(any(Approval.class))).thenThrow(new NotFoundException("User not found"));

        mockMvc.perform(post("/approvals/v1")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(approval)))
            .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("DELETE /approvals/{approvalId}/v1 deletes approval successfully")
    void deleteApprovalSuccessfully() throws Exception {
        mockMvc.perform(delete("/approvals/{approvalId}/v1", approvalId))
            .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("DELETE /approvals/{approvalId}/v1 returns 400 for invalid UUID")
    void deleteApprovalWithInvalidUuidReturnsBadRequest() throws Exception {
        mockMvc.perform(delete("/approvals/{approvalId}/v1", "invalid-uuid"))
            .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("DELETE /approvals/{approvalId}/v1 returns 404 when approval does not exist")
    void deleteApprovalWhenApprovalDoesNotExistReturnsNotFound() throws Exception {
        doThrow(new NotFoundException("Approval not found")).when(approvalService).deleteApproval(approvalId);

        mockMvc.perform(delete("/approvals/{approvalId}/v1", approvalId))
            .andExpect(status().isNotFound());
    }

    private Approval createApproval() {
        return Approval.builder()
            .id(approvalId)
            .subjectId(subjectId)
            .subjectType(AuditSubjectType.COURT)
            .userId(userId)
            .build();
    }

    private ApprovalStatus createApprovalStatus() {
        return new ApprovalStatus(
            subjectId,
            AuditSubjectType.COURT,
            "Test Court",
            true,
            approvalId,
            userId,
            User.builder().id(userId).email("approver@justice.gov.uk").build(),
            null
        );
    }
}
