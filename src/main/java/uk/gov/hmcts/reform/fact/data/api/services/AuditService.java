package uk.gov.hmcts.reform.fact.data.api.services;

import uk.gov.hmcts.reform.fact.data.api.config.properties.AuditConfigurationProperties;
import uk.gov.hmcts.reform.fact.data.api.entities.Audit;
import uk.gov.hmcts.reform.fact.data.api.entities.types.AuditSubjectType;
import uk.gov.hmcts.reform.fact.data.api.errorhandling.exceptions.InvalidParameterCombinationException;
import uk.gov.hmcts.reform.fact.data.api.repositories.AuditRepository;

import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.Optional;
import java.util.UUID;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuditService {

    private final AuditRepository auditRepository;
    private final AuditConfigurationProperties auditConfiguration;

    // bitfields for query column matching
    private static final int IGNORE             = 0b0000;
    private static final int INCLUDE_SUBJECT_ID = 0b0001;
    private static final int INCLUDE_TO_DATE    = 0b0010;
    private static final int INCLUDE_EMAIL      = 0b0100;

    /**
     * Get a paginated list of {@link Audit}s with optional filters.
     *
     * @param pageNumber The page of results to return (zero based)
     * @param pageSize   The size of the results page
     * @param fromDate   The "from" date for filtering. Filtering assumes start of day.
     * @param courtId    The id of the court. can be {@code null}.
     * @param serviceCentreId The id of the service centre. can be {@code null}.
     * @param email      The email, or partial email of the related user. can be {@code null}.
     * @param toDate     The "to" date for auditing. Filtering assumes end of day. can e {@code null}.
     * @return a {@link Page} of {@link Audit} results.
     */
    public Page<Audit> getFilteredAndPaginatedAudits(int pageNumber, int pageSize, @NonNull LocalDate fromDate,
                                                     LocalDate toDate, String courtId, String serviceCentreId,
                                                     String email) {

        Pageable pageable = PageRequest.of(pageNumber, pageSize, Sort.by("createdAt").descending());

        ZonedDateTime fromDateTime = ZonedDateTime.ofInstant(
            fromDate.atStartOfDay(ZoneOffset.UTC).toInstant(),
            ZoneOffset.UTC
        );

        ZonedDateTime toDateTime = Optional.ofNullable(toDate).map(to -> ZonedDateTime.ofInstant(
            to.plusDays(1).atStartOfDay(ZoneOffset.UTC).toInstant(),
            ZoneOffset.UTC
        )).orElse(null);

        SubjectFilter subjectFilter = resolveSubjectFilter(courtId, serviceCentreId);

        return performAuditQuery(fromDateTime, toDateTime, subjectFilter, email, pageable);
    }

    /**
     * Removes any audit entries which have exceeded the configured retention period.
     *
     * <p>
     * Expiry retention days are configured via {@link AuditConfigurationProperties}.
     */
    @Transactional
    public void removeExpiredAuditEntries() {
        auditRepository.deleteAllByCreatedAtBefore(
            ZonedDateTime.now().minusDays(auditConfiguration.getRetentionDays())
        );
    }

    private Page<Audit> performAuditQuery(ZonedDateTime fromDateTime, ZonedDateTime toDateTime,
                                          SubjectFilter subjectFilter, String email, Pageable pageable) {

        // create a query bitfield that can be tested in the switch
        // below using boolean arithmetic matching.
        int query = 0;
        query |= subjectFilter != null ? INCLUDE_SUBJECT_ID : IGNORE;
        query |= toDateTime != null ? INCLUDE_TO_DATE : IGNORE;
        query |= email != null && !email.isBlank() ? INCLUDE_EMAIL : IGNORE;

        // perform the appropriate repository lookup based on matching
        // bits in the query param
        return switch (query) {
            case INCLUDE_SUBJECT_ID -> auditRepository.findBySubjectIdAndSubjectTypeAndCreatedAtAfter(
                subjectFilter.subjectId(), subjectFilter.subjectType(), fromDateTime, pageable);
            case INCLUDE_TO_DATE -> auditRepository.findByCreatedAtBetween(
                fromDateTime, toDateTime, pageable);
            case INCLUDE_SUBJECT_ID | INCLUDE_TO_DATE ->
                auditRepository.findBySubjectIdAndSubjectTypeAndCreatedAtBetween(
                    subjectFilter.subjectId(), subjectFilter.subjectType(), fromDateTime, toDateTime, pageable);
            case INCLUDE_EMAIL -> auditRepository.findByCreatedAtAfterAndEmailAddressLike(
                fromDateTime, email, pageable);
            case INCLUDE_SUBJECT_ID | INCLUDE_EMAIL ->
                auditRepository.findBySubjectIdAndSubjectTypeAndCreatedAtAfterAndEmailAddressLike(
                    subjectFilter.subjectId(), subjectFilter.subjectType(), fromDateTime, email, pageable);
            case INCLUDE_TO_DATE | INCLUDE_EMAIL -> auditRepository.findByCreatedAtBetweenAndEmailAddressLike(
                fromDateTime, toDateTime, email, pageable);
            case INCLUDE_SUBJECT_ID | INCLUDE_TO_DATE | INCLUDE_EMAIL ->
                auditRepository.findBySubjectIdAndSubjectTypeAndCreatedAtBetweenAndEmailAddressLike(
                    subjectFilter.subjectId(), subjectFilter.subjectType(), fromDateTime, toDateTime, email, pageable);
            // if no field match bits are set then test only using the
            // mandatory fromDate.
            default -> auditRepository.findByCreatedAtAfter(fromDateTime, pageable);
        };
    }

    private static SubjectFilter resolveSubjectFilter(String courtId, String serviceCentreId) {
        if (courtId != null && serviceCentreId != null) {
            throw new InvalidParameterCombinationException("Only one of courtId or serviceCentreId can be provided");
        }
        if (courtId != null) {
            return new SubjectFilter(UUID.fromString(courtId), AuditSubjectType.COURT);
        }
        if (serviceCentreId != null) {
            return new SubjectFilter(UUID.fromString(serviceCentreId), AuditSubjectType.SERVICE_CENTRE);
        }
        return null;
    }

    private record SubjectFilter(UUID subjectId, AuditSubjectType subjectType) {}
}
