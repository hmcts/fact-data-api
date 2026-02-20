package uk.gov.hmcts.reform.fact.data.api.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import uk.gov.hmcts.reform.fact.data.api.config.properties.AuditConfigurationProperties;
import uk.gov.hmcts.reform.fact.data.api.entities.Audit;
import uk.gov.hmcts.reform.fact.data.api.entities.Court;
import uk.gov.hmcts.reform.fact.data.api.entities.types.Change;
import uk.gov.hmcts.reform.fact.data.api.repositories.AuditRepository;

import java.time.LocalDate;
import java.time.ZoneOffset;
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
import org.springframework.data.domain.Pageable;

@ExtendWith(MockitoExtension.class)
class AuditServiceTest {

    private static final UUID AUDIT_ID = UUID.randomUUID();
    private static final UUID COURT_ID = UUID.randomUUID();
    private static final UUID USER_ID = UUID.randomUUID();

    private static final String EMAIL = "test.user@example.com";

    private static final int PAGE_NUMBER = 0;
    private static final int PAGE_SIZE = 25;

    private static final LocalDate fromDate = LocalDate.now();
    private static final LocalDate toDate = LocalDate.now();
    private static final ZonedDateTime fromDateTime = ZonedDateTime.ofInstant(
        fromDate.atStartOfDay(ZoneOffset.UTC).toInstant(),
        ZoneOffset.UTC
    );
    private static final ZonedDateTime toDateTime = ZonedDateTime.ofInstant(
        toDate.plusDays(1).atStartOfDay(ZoneOffset.UTC).toInstant(),
        ZoneOffset.UTC
    );

    @Mock
    private AuditRepository auditRepository;
    @Mock
    private AuditConfigurationProperties auditConfigurationProperties;

    @InjectMocks
    private AuditService auditService;

    // simple paged queries

    @Test
    void getFilteredAndPaginatedAuditsShouldCallFindByCreatedAtAfter() {

        when(auditRepository.findByCreatedAtAfter(
                 eq(fromDateTime),
                 any(Pageable.class)
             )
        ).thenReturn(new PageImpl<>(List.of(createAudit())));

        Page<Audit> result =
            auditService.getFilteredAndPaginatedAudits(
                PAGE_NUMBER,
                PAGE_SIZE,
                fromDate,
                null,
                null,
                null
            );

        assertThat(result.getContent()).hasSize(1);
        verify(auditRepository).findByCreatedAtAfter(
            eq(fromDateTime), any(Pageable.class)
        );
    }

    @Test
    void getFilteredAndPaginatedAuditsShouldCallFindByCreatedAtBetween() {

        when(auditRepository.findByCreatedAtBetween(
                 eq(fromDateTime),
                 eq(toDateTime),
                 any(Pageable.class)
             )
        ).thenReturn(new PageImpl<>(List.of(createAudit())));

        Page<Audit> result =
            auditService.getFilteredAndPaginatedAudits(
                PAGE_NUMBER,
                PAGE_SIZE,
                fromDate,
                toDate,
                null,
                null
            );

        assertThat(result.getContent()).hasSize(1);
        verify(auditRepository).findByCreatedAtBetween(
            eq(fromDateTime), eq(toDateTime), any(Pageable.class)
        );
    }

    @Test
    void getFilteredAndPaginatedAuditsShouldCallFindByCourtIdAndCreatedAtAfter() {

        when(auditRepository.findByCourtIdAndCreatedAtAfter(
                 eq(COURT_ID),
                 eq(fromDateTime),
                 any(Pageable.class)
             )
        ).thenReturn(new PageImpl<>(List.of(createAudit())));

        Page<Audit> result =
            auditService.getFilteredAndPaginatedAudits(
                PAGE_NUMBER,
                PAGE_SIZE,
                fromDate,
                null,
                COURT_ID.toString(),
                null
            );

        assertThat(result.getContent()).hasSize(1);
        verify(auditRepository).findByCourtIdAndCreatedAtAfter(
            eq(COURT_ID), eq(fromDateTime), any(Pageable.class)
        );
    }

    @Test
    void getFilteredAndPaginatedAuditsShouldCallFindByCourtIdAndCreatedAtBetween() {

        when(auditRepository.findByCourtIdAndCreatedAtBetween(
                 eq(COURT_ID),
                 eq(fromDateTime),
                 eq(toDateTime),
                 any(Pageable.class)
             )
        ).thenReturn(new PageImpl<>(List.of(createAudit())));

        Page<Audit> result =
            auditService.getFilteredAndPaginatedAudits(
                PAGE_NUMBER,
                PAGE_SIZE,
                fromDate,
                toDate,
                COURT_ID.toString(),
                null
            );

        assertThat(result.getContent()).hasSize(1);
        verify(auditRepository).findByCourtIdAndCreatedAtBetween(
            eq(COURT_ID), eq(fromDateTime), eq(toDateTime), any(Pageable.class)
        );
    }

    // complex paged queries (that have email matching as well)

    @Test
    void getFilteredAndPaginatedAuditsShouldCallFindByCreatedAtAfterAndEmailAddressLike() {

        when(auditRepository.findByCreatedAtAfterAndEmailAddressLike(
                 eq(fromDateTime),
                 eq(EMAIL),
                 any(Pageable.class)
             )
        ).thenReturn(new PageImpl<>(List.of(createAudit())));

        Page<Audit> result =
            auditService.getFilteredAndPaginatedAudits(
                PAGE_NUMBER,
                PAGE_SIZE,
                fromDate,
                null,
                null,
                EMAIL
            );

        assertThat(result.getContent()).hasSize(1);
        verify(auditRepository).findByCreatedAtAfterAndEmailAddressLike(
            eq(fromDateTime), eq(EMAIL), any(Pageable.class)
        );
    }

    @Test
    void getFilteredAndPaginatedAuditsShouldCallFindByCreatedAtBetweenAndEmailAddressLike() {

        when(auditRepository.findByCreatedAtBetweenAndEmailAddressLike(
                 eq(fromDateTime),
                 eq(toDateTime),
                 eq(EMAIL),
                 any(Pageable.class)
             )
        ).thenReturn(new PageImpl<>(List.of(createAudit())));

        Page<Audit> result =
            auditService.getFilteredAndPaginatedAudits(
                PAGE_NUMBER,
                PAGE_SIZE,
                fromDate,
                toDate,
                null,
                EMAIL
            );

        assertThat(result.getContent()).hasSize(1);
        verify(auditRepository).findByCreatedAtBetweenAndEmailAddressLike(
            eq(fromDateTime), eq(toDateTime), eq(EMAIL), any(Pageable.class)
        );
    }

    @Test
    void getFilteredAndPaginatedAuditsShouldCallFindByCourtIdAndCreatedAtAfterAndEmailAddressLike() {

        when(auditRepository.findByCourtIdAndCreatedAtAfterAndEmailAddressLike(
                 eq(COURT_ID),
                 eq(fromDateTime),
                 eq(EMAIL),
                 any(Pageable.class)
             )
        ).thenReturn(new PageImpl<>(List.of(createAudit())));

        Page<Audit> result =
            auditService.getFilteredAndPaginatedAudits(
                PAGE_NUMBER,
                PAGE_SIZE,
                fromDate,
                null,
                COURT_ID.toString(),
                EMAIL
            );

        assertThat(result.getContent()).hasSize(1);
        verify(auditRepository).findByCourtIdAndCreatedAtAfterAndEmailAddressLike(
            eq(COURT_ID), eq(fromDateTime), eq(EMAIL), any(Pageable.class)
        );
    }

    @Test
    void getFilteredAndPaginatedAuditsShouldCallFindByCourtIdAndCreatedAtBetweenAndEmailAddressLike() {

        when(auditRepository.findByCourtIdAndCreatedAtBetweenAndEmailAddressLike(
                 eq(COURT_ID),
                 eq(fromDateTime),
                 eq(toDateTime),
                 eq(EMAIL),
                 any(Pageable.class)
             )
        ).thenReturn(new PageImpl<>(List.of(createAudit())));

        Page<Audit> result =
            auditService.getFilteredAndPaginatedAudits(
                PAGE_NUMBER,
                PAGE_SIZE,
                fromDate,
                toDate,
                COURT_ID.toString(),
                EMAIL
            );

        assertThat(result.getContent()).hasSize(1);
        verify(auditRepository).findByCourtIdAndCreatedAtBetweenAndEmailAddressLike(
            eq(COURT_ID), eq(fromDateTime), eq(toDateTime), eq(EMAIL), any(Pageable.class)
        );
    }

    @Test
    void removeExpiredAuditEntriesShouldUserPropertiesAndCallDeleteAllBeforeCreatedDate() {
        int retentionDays = 365;
        doNothing().when(auditRepository).deleteAllByCreatedAtBefore(any(ZonedDateTime.class));
        when(auditConfigurationProperties.getRetentionDays()).thenReturn(retentionDays);

        auditService.removeExpiredAuditEntries();

        verify(auditRepository).deleteAllByCreatedAtBefore(any(ZonedDateTime.class));
        verify(auditConfigurationProperties).getRetentionDays();
    }


    // edge case tests

    @Test
    void shouldThrowNullPointerExceptionWhenFromDateIsNull() {
        assertThrows(
            NullPointerException.class, () -> {
                auditService.getFilteredAndPaginatedAudits(0, 1, null, null, null, null);
            }
        );
    }

    @Test
    void shouldCallSimpleQueryForBlankEmail() {

        when(auditRepository.findByCreatedAtAfter(
                 eq(fromDateTime),
                 any(Pageable.class)
             )
        ).thenReturn(new PageImpl<>(List.of(createAudit())));

        Page<Audit> result =
            auditService.getFilteredAndPaginatedAudits(
                PAGE_NUMBER,
                PAGE_SIZE,
                fromDate,
                null,
                null,
                ""
            );
        assertThat(result.getContent()).hasSize(1);

        result =
            auditService.getFilteredAndPaginatedAudits(
                PAGE_NUMBER,
                PAGE_SIZE,
                fromDate,
                null,
                null,
                " "
            );
        assertThat(result.getContent()).hasSize(1);

        verify(auditRepository, times(2)).findByCreatedAtAfter(
            eq(fromDateTime), any(Pageable.class)
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
