package uk.gov.hmcts.reform.fact.data.api.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import uk.gov.hmcts.reform.fact.data.api.config.properties.AuditConfigurationProperties;
import uk.gov.hmcts.reform.fact.data.api.entities.Audit;
import uk.gov.hmcts.reform.fact.data.api.entities.Court;
import uk.gov.hmcts.reform.fact.data.api.entities.types.SubjectType;
import uk.gov.hmcts.reform.fact.data.api.entities.types.Change;
import uk.gov.hmcts.reform.fact.data.api.entities.types.NameAndId;
import uk.gov.hmcts.reform.fact.data.api.errorhandling.exceptions.InvalidParameterCombinationException;
import uk.gov.hmcts.reform.fact.data.api.errorhandling.exceptions.NotFoundException;
import uk.gov.hmcts.reform.fact.data.api.repositories.AuditRepository;

import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
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
    private static final UUID SERVICE_CENTRE_ID = UUID.randomUUID();
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
    private CourtService courtService;
    @Mock
    private ServiceCentreService serviceCentreService;
    @Mock
    private AuditConfigurationProperties auditConfigurationProperties;

    @InjectMocks
    private AuditService auditService;

    @Test
    void getFilteredAndPaginatedAuditsShouldPassNullFiltersWhenOnlyFromProvided() {
        when(auditRepository.findFiltered(any(), any(), any(), any(), any(), any(), any(Pageable.class)))
            .thenReturn(new PageImpl<>(List.of(createAudit())));

        Page<Audit> result = auditService.getFilteredAndPaginatedAudits(
            PAGE_NUMBER,
            PAGE_SIZE,
            fromDate,
            null,
            null,
            null,
            null,
            null,
            null
        );

        assertThat(result.getContent()).hasSize(1);
        verify(auditRepository).findFiltered(
            eq(fromDateTime),
            isNull(),
            isNull(),
            isNull(),
            isNull(),
            eq(""),
            any(Pageable.class)
        );
    }

    @Test
    void getFilteredAndPaginatedAuditsShouldPassAllFiltersWhenAllProvided() {
        when(auditRepository.findFiltered(any(), any(), any(), any(), any(), any(), any(Pageable.class)))
            .thenReturn(new PageImpl<>(List.of(createAudit())));

        Page<Audit> result = auditService.getFilteredAndPaginatedAudits(
            PAGE_NUMBER,
            PAGE_SIZE,
            fromDate,
            toDate,
            null,
            COURT_ID.toString(),
            null,
            "clarewood",
            EMAIL
        );

        assertThat(result.getContent()).hasSize(1);
        verify(auditRepository).findFiltered(
            eq(fromDateTime),
            eq(toDateTime),
            eq(COURT_ID),
            eq(SubjectType.COURT),
            eq("%clarewood%"),
            eq(EMAIL),
            any(Pageable.class)
        );
    }

    @Test
    void getFilteredAndPaginatedAuditsShouldPassServiceCentreIdAndSubjectType() {
        when(auditRepository.findFiltered(any(), any(), any(), any(), any(), any(), any(Pageable.class)))
            .thenReturn(new PageImpl<>(List.of(createAudit())));

        Page<Audit> result = auditService.getFilteredAndPaginatedAudits(
            PAGE_NUMBER,
            PAGE_SIZE,
            fromDate,
            null,
            null,
            null,
            SERVICE_CENTRE_ID.toString(),
            null,
            null
        );

        assertThat(result.getContent()).hasSize(1);
        verify(auditRepository).findFiltered(
            eq(fromDateTime),
            isNull(),
            eq(SERVICE_CENTRE_ID),
            eq(SubjectType.SERVICE_CENTRE),
            isNull(),
            eq(""),
            any(Pageable.class)
        );
    }

    @Test
    void getFilteredAndPaginatedAuditsShouldPassSubjectTypeWithNullSubjectIdWhenOnlySubjectTypeProvided() {
        when(auditRepository.findFiltered(any(), any(), any(), any(), any(), any(), any(Pageable.class)))
            .thenReturn(new PageImpl<>(List.of(createAudit())));

        Page<Audit> result = auditService.getFilteredAndPaginatedAudits(
            PAGE_NUMBER,
            PAGE_SIZE,
            fromDate,
            null,
            SubjectType.COURT,
            null,
            null,
            null,
            null
        );

        assertThat(result.getContent()).hasSize(1);
        verify(auditRepository).findFiltered(
            eq(fromDateTime),
            isNull(),
            isNull(),
            eq(SubjectType.COURT),
            isNull(),
            eq(""),
            any(Pageable.class)
        );
    }

    @Test
    void getFilteredAndPaginatedAuditsShouldPrioritiseCourtIdOverSubjectType() {
        when(auditRepository.findFiltered(any(), any(), any(), any(), any(), any(), any(Pageable.class)))
            .thenReturn(new PageImpl<>(List.of(createAudit())));

        Page<Audit> result = auditService.getFilteredAndPaginatedAudits(
            PAGE_NUMBER,
            PAGE_SIZE,
            fromDate,
            null,
            SubjectType.SERVICE_CENTRE,
            COURT_ID.toString(),
            null,
            null,
            null
        );

        assertThat(result.getContent()).hasSize(1);
        verify(auditRepository).findFiltered(
            eq(fromDateTime),
            isNull(),
            eq(COURT_ID),
            eq(SubjectType.COURT),
            isNull(),
            eq(""),
            any(Pageable.class)
        );
    }

    @Test
    void getFilteredAndPaginatedAuditsShouldIgnoreBlankSearchTextAndEmail() {
        when(auditRepository.findFiltered(any(), any(), any(), any(), any(), any(), any(Pageable.class)))
            .thenReturn(new PageImpl<>(List.of(createAudit())));

        auditService.getFilteredAndPaginatedAudits(
            PAGE_NUMBER,
            PAGE_SIZE,
            fromDate,
            null,
            null,
            null,
            null,
            "  ",
            "  "
        );
        auditService.getFilteredAndPaginatedAudits(
            PAGE_NUMBER,
            PAGE_SIZE,
            fromDate,
            null,
            null,
            null,
            null,
            "  ",
            "  "
        );

        verify(auditRepository, times(2)).findFiltered(
            eq(fromDateTime),
            isNull(),
            isNull(),
            isNull(),
            isNull(),
            eq(""),
            any(Pageable.class)
        );
    }

    @Test
    void getFilteredAndPaginatedAuditsShouldTrimSearchText() {
        when(auditRepository.findFiltered(any(), any(), any(), any(), any(), any(), any(Pageable.class)))
            .thenReturn(new PageImpl<>(List.of(createAudit())));

        Page<Audit> result = auditService.getFilteredAndPaginatedAudits(
            PAGE_NUMBER,
            PAGE_SIZE,
            fromDate,
            null,
            null,
            null,
            null,
            " clarewood  ",
            null
        );

        assertThat(result.getContent()).hasSize(1);
        verify(auditRepository).findFiltered(
            eq(fromDateTime),
            isNull(),
            isNull(),
            isNull(),
            eq("%clarewood%"),
            eq(""),
            any(Pageable.class)
        );
    }

    @Test
    void getFilteredAndPaginatedAuditsShouldScapeLikeWildcardsInSearchText() {
        when(auditRepository.findFiltered(any(), any(), any(), any(), any(), any(), any(Pageable.class)))
            .thenReturn(new PageImpl<>(List.of(createAudit())));

        auditService.getFilteredAndPaginatedAudits(
            PAGE_NUMBER,
            PAGE_SIZE,
            fromDate,
            null,
            null,
            null,
            null,
            "50%_a\\b",
            null
        );

        verify(auditRepository).findFiltered(
            eq(fromDateTime),
            isNull(),
            isNull(),
            isNull(),
            eq("%50\\%\\_a\\\\b%"),
            eq(""),
            any(Pageable.class)
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

    @Test
    void shouldThrowInvalidParameterCombinationExceptionWhenBothSubjectIdsProvided() {
        String courtId = COURT_ID.toString();
        String serviceCentreId = SERVICE_CENTRE_ID.toString();

        assertThrows(
            InvalidParameterCombinationException.class, () -> auditService.getFilteredAndPaginatedAudits(
                PAGE_NUMBER,
                PAGE_SIZE,
                fromDate,
                null,
                null,
                courtId,
                serviceCentreId,
                null,
                null
            )
        );
    }

    @Test
    void shouldThrowNullPointerExceptionWhenFromDateIsNull() {
        assertThrows(
            NullPointerException.class,
            () -> auditService.getFilteredAndPaginatedAudits(0, 1, null, null, null, null, null, null, null)
        );
    }

    @Test
    void getSubjectNameAndIdMapShouldAllSubjectData() {
        List<NameAndId> courts = List.of(
            new NameAndId("Birmingham Civil and Family Justice Centre", UUID.randomUUID()),
            new NameAndId("Exeter Law Courts", UUID.randomUUID())
        );
        List<NameAndId> serviceCentres = List.of(
            new NameAndId("Traffic Enforcement Centre", UUID.randomUUID())
        );
        when(courtService.getAllCourtNameAndIds()).thenReturn(courts);
        when(serviceCentreService.getAllServiceCentreNameAndIds()).thenReturn(serviceCentres);

        Map<SubjectType, List<NameAndId>> result = auditService.getSubjectNameAndIdMap();

        assertThat(result).hasSize(2).containsKeys(SubjectType.COURT, SubjectType.SERVICE_CENTRE);
        assertThat(result.get(SubjectType.COURT)).containsExactlyElementsOf(courts);
        assertThat(result.get(SubjectType.SERVICE_CENTRE)).containsExactlyElementsOf(serviceCentres);
        verify(courtService).getAllCourtNameAndIds();
        verify(serviceCentreService).getAllServiceCentreNameAndIds();
    }

    @Test
    void getSubjectNameAndIdMapShouldReturnEmptyListsWhenNoSubjectsExist() {
        when(courtService.getAllCourtNameAndIds()).thenReturn(List.of());
        when(serviceCentreService.getAllServiceCentreNameAndIds()).thenReturn(List.of());

        Map<SubjectType, List<NameAndId>> result = auditService.getSubjectNameAndIdMap();

        assertThat(result).containsKeys(SubjectType.COURT, SubjectType.SERVICE_CENTRE);
        assertThat(result.get(SubjectType.COURT)).isEmpty();
        assertThat(result.get(SubjectType.SERVICE_CENTRE)).isEmpty();
        verify(courtService).getAllCourtNameAndIds();
        verify(serviceCentreService).getAllServiceCentreNameAndIds();
    }

    @Test
    void getAuditByIdShouldReturnAuditWhenRecordExists() {
        Audit audit = createAudit();

        when(auditRepository.findWithUserById(AUDIT_ID)).thenReturn(Optional.of(audit));

        Audit result = auditService.getAuditById(AUDIT_ID);

        assertThat(result).isSameAs(audit);
        verify(auditRepository).findWithUserById(AUDIT_ID);
    }

    @Test
    void getAuditByIdShouldThrowNotFoundExceptionWhenRecordDoesNotExist() {
        when(auditRepository.findWithUserById(AUDIT_ID)).thenReturn(Optional.empty());

        NotFoundException exception = assertThrows(
            NotFoundException.class,
            () -> auditService.getAuditById(AUDIT_ID)
        );

        assertThat(exception.getMessage()).isEqualTo("Audit not found, ID: " + AUDIT_ID);
        verify(auditRepository).findWithUserById(AUDIT_ID);
    }

    private Audit createAudit() {
        return Audit.builder()
            .id(AUDIT_ID)
            .subjectId(COURT_ID)
            .subjectType(SubjectType.COURT)
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
