package uk.gov.hmcts.reform.fact.data.api.services;

import uk.gov.hmcts.reform.fact.data.api.config.properties.AuditConfigurationProperties;
import uk.gov.hmcts.reform.fact.data.api.entities.Audit;
import uk.gov.hmcts.reform.fact.data.api.entities.types.AuditSubjectType;
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
    private final CourtService courtService;
    private final ServiceCentreService serviceCentreService;
    private final AuditConfigurationProperties auditConfiguration;

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
        boolean hasToDate = toDateTime != null;
        boolean hasEmail = email != null && !email.isBlank();

        if (subjectFilter != null) {
            return performSubjectAuditQuery(fromDateTime, toDateTime, subjectFilter, email, pageable);
        }
        if (hasToDate && hasEmail) {
            return auditRepository.findByCreatedAtBetweenAndEmailAddressLike(
                fromDateTime, toDateTime, email, pageable);
        }
        if (hasToDate) {
            return auditRepository.findByCreatedAtBetween(fromDateTime, toDateTime, pageable);
        }
        if (hasEmail) {
            return auditRepository.findByCreatedAtAfterAndEmailAddressLike(fromDateTime, email, pageable);
        }
        return auditRepository.findByCreatedAtAfter(fromDateTime, pageable);
    }

    private Page<Audit> performSubjectAuditQuery(ZonedDateTime fromDateTime, ZonedDateTime toDateTime,
                                                 SubjectFilter subjectFilter, String email, Pageable pageable) {
        UUID subjectId = subjectFilter.subjectId();
        AuditSubjectType subjectType = subjectFilter.subjectType();
        boolean hasToDate = toDateTime != null;
        boolean hasEmail = email != null && !email.isBlank();

        if (hasToDate && hasEmail) {
            return auditRepository.findBySubjectIdAndSubjectTypeAndCreatedAtBetweenAndEmailAddressLike(
                subjectId, subjectType, fromDateTime, toDateTime, email, pageable);
        }
        if (hasToDate) {
            return auditRepository.findBySubjectIdAndSubjectTypeAndCreatedAtBetween(
                subjectId, subjectType, fromDateTime, toDateTime, pageable);
        }
        if (hasEmail) {
            return auditRepository.findBySubjectIdAndSubjectTypeAndCreatedAtAfterAndEmailAddressLike(
                subjectId, subjectType, fromDateTime, email, pageable);
        }
        return auditRepository.findBySubjectIdAndSubjectTypeAndCreatedAtAfter(
            subjectId, subjectType, fromDateTime, pageable);
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

    /**
     * Retrieves the complete set of supported audit subjects with their corresponding name and id pairs.
     *
     * @return a {@link Map} of {@link AuditSubjectType} to a list of {@link NameAndId} pairs
     *         representing the names and ids of the subjects.
     */
    public Map<AuditSubjectType, List<NameAndId>> getSubjectNameAndIdMap() {
        return Map.of(
            AuditSubjectType.COURT, courtService.getAllCourtNameAndIds(),
            AuditSubjectType.SERVICE_CENTRE, serviceCentreService.getAllServiceCentreNameAndIds()
        );
    }

    /**
     * Retrieves an {@link Audit} by its id.
     *
     * @param auditId the ID of the audit record.
     * @return the {@link Audit} record with the specified ID.
     * @throws NotFoundException if no audit record is found with the given ID.
     */
    public Audit getAuditById(final UUID auditId) {
        return auditRepository.findWithUserById(auditId).orElseThrow(() ->
            new NotFoundException("Audit not found, ID: " + auditId)
        );
    }
}
