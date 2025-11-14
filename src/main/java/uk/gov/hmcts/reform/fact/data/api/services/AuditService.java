package uk.gov.hmcts.reform.fact.data.api.services;

import uk.gov.hmcts.reform.fact.data.api.config.properties.AuditConfigurationProperties;
import uk.gov.hmcts.reform.fact.data.api.entities.Audit;
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

@Service
@RequiredArgsConstructor
public class AuditService {

    private final AuditRepository auditRepository;
    private final AuditConfigurationProperties auditConfiguration;

    /**
     * Get a paginated list of {@link Audit}s with optional filters.
     *
     * @param pageNumber The page of results to return (zero based)
     * @param pageSize   The size of the results page
     * @param fromDate   The "from" date for filtering. Filtering assumes start of day.
     * @param courtId    The id of the court. can be {@code null}.
     * @param email      The email, or partial email of the related user. can be {@code null}.
     * @param toDate     The "to" date for auditing. Filtering assumes end of day. can e {@code null}.
     * @return a {@link Page} of {@link Audit} results.
     */
    public Page<Audit> getFilteredAndPaginatedAudits(int pageNumber, int pageSize, @NonNull LocalDate fromDate,
                                                     LocalDate toDate, String courtId, String email) {

        Pageable pageable = PageRequest.of(pageNumber, pageSize, Sort.by("createdAt").descending());

        ZonedDateTime fromDateTime = ZonedDateTime.ofInstant(
            fromDate.atStartOfDay(ZoneOffset.UTC).toInstant(),
            ZoneOffset.UTC
        );

        ZonedDateTime toDateTime = Optional.ofNullable(toDate).map(to -> ZonedDateTime.ofInstant(
            to.plusDays(1).atStartOfDay(ZoneOffset.UTC).toInstant(),
            ZoneOffset.UTC
        )).orElse(null);

        UUID courtUUID = Optional.ofNullable(courtId).map(UUID::fromString).orElse(null);

        if (email != null && !email.isBlank()) {
            return performEmailMatchAuditQuery(courtUUID, toDateTime, fromDateTime, email, pageable);
        }
        return performAuditQuery(courtUUID, toDateTime, fromDateTime, pageable);
    }

    /**
     * Removes any audit entries which have exceeded the configured retention period.
     *
     * <p>
     * Expiry retention days are configured via {@link AuditConfigurationProperties}.
     */
    public void removeExpiredAuditEntries() {
        auditRepository.deleteAllByCreatedAtBefore(
            ZonedDateTime.now().minusDays(auditConfiguration.getRetentionDays())
        );
    }

    private Page<Audit> performAuditQuery(final UUID courtUUID, final ZonedDateTime toDateTime,
                                          final ZonedDateTime fromDateTime, final Pageable pageable) {
        if (courtUUID != null && toDateTime != null) {
            return auditRepository.findByCourtIdAndCreatedAtBetween(
                courtUUID, fromDateTime, toDateTime, pageable);
        } else if (courtUUID != null) {
            return auditRepository.findByCourtIdAndCreatedAtAfter(
                courtUUID, fromDateTime, pageable);
        } else if (toDateTime != null) {
            return auditRepository.findByCreatedAtBetween(
                fromDateTime, toDateTime, pageable);
        } else {
            return auditRepository.findByCreatedAtAfter(
                fromDateTime, pageable);
        }
    }

    private Page<Audit> performEmailMatchAuditQuery(final UUID courtUUID, final ZonedDateTime toDateTime,
                                                    final ZonedDateTime fromDateTime, String email,
                                                    final Pageable pageable) {
        if (courtUUID != null && toDateTime != null) {
            return auditRepository.findByCourtIdAndCreatedAtBetweenAndEmailAddressLike(
                courtUUID, fromDateTime, toDateTime, email, pageable);
        } else if (courtUUID != null) {
            return auditRepository.findByCourtIdAndCreatedAtAfterAndEmailAddressLike(
                courtUUID, fromDateTime, email, pageable);
        } else if (toDateTime != null) {
            return auditRepository.findByCreatedAtBetweenAndEmailAddressLike(
                fromDateTime, toDateTime, email, pageable);
        } else {
            return auditRepository.findByCreatedAtAfterAndEmailAddressLike(
                fromDateTime, email, pageable);
        }
    }

}
