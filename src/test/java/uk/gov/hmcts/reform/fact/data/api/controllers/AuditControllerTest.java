package uk.gov.hmcts.reform.fact.data.api.controllers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.when;

import uk.gov.hmcts.reform.fact.data.api.entities.Audit;
import uk.gov.hmcts.reform.fact.data.api.entities.Court;
import uk.gov.hmcts.reform.fact.data.api.entities.types.Change;
import uk.gov.hmcts.reform.fact.data.api.errorhandling.exceptions.InvalidDateRangeException;
import uk.gov.hmcts.reform.fact.data.api.services.AuditService;

import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

@ExtendWith(MockitoExtension.class)
class AuditControllerTest {

    private static final UUID AUDIT_ID = UUID.randomUUID();
    private static final UUID COURT_ID = UUID.randomUUID();
    private static final UUID USER_ID = UUID.randomUUID();

    private static final int PAGE_NUMBER = 0;
    private static final int PAGE_SIZE = 25;

    private static final String RESPONSE_STATUS_MISMATCH = "Response status does not match";
    private static final String RESPONSE_BODY_MISMATCH = "Response body does not match";

    @Mock
    AuditService auditService;

    @InjectMocks
    private AuditController auditController;

    @Test
    void getFilteredAndPaginatedAuditsReturns200() {
        Audit audit = createAudit();
        Page<Audit> auditPage = new PageImpl<>(List.of(audit));

        when(auditService.getFilteredAndPaginatedAudits(
            anyInt(),
            anyInt(),
            any(LocalDate.class),
            any(LocalDate.class),
            isNull(),
            isNull()
        )).thenReturn(auditPage);

        ResponseEntity<Page<Audit>> response = auditController.getFilteredAndPaginatedAudits(
            PAGE_NUMBER,
            PAGE_SIZE,
            null,
            null,
            LocalDate.now().minusDays(1),
            LocalDate.now()
        );

        assertEquals(HttpStatus.OK, response.getStatusCode(), RESPONSE_STATUS_MISMATCH);
        assertEquals(auditPage, response.getBody(), RESPONSE_BODY_MISMATCH);
    }

    @Test
    void getFilteredAndPaginatedAuditsThrowsInvalidDateRangeExceptionForInvalidDateRange() {
        LocalDate fromDate = LocalDate.now().plusDays(2);
        LocalDate toDate = LocalDate.now();
        assertThrows(
            InvalidDateRangeException.class, () ->
                auditController.getFilteredAndPaginatedAudits(
                    PAGE_NUMBER,
                    PAGE_SIZE,
                    null,
                    null,
                    fromDate,
                    toDate
                )
        );
    }

    private Audit createAudit() {
        return Audit.builder()
            .id(AUDIT_ID)
            .courtId(COURT_ID)
            .actionEntity(Court.class.getSimpleName())
            .actionDataDiff(List.of(
                new Change("isOpen", Boolean.FALSE, Boolean.TRUE),
                new Change("name", "Exeter Law Courts", "South Devon Law Courts")
            ))
            .userId(USER_ID)
            .createdAt(ZonedDateTime.now())
            .build();
    }
}
