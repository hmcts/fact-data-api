package uk.gov.hmcts.reform.fact.data.api.services;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import uk.gov.hmcts.reform.fact.data.api.dto.AllLocation;
import uk.gov.hmcts.reform.fact.data.api.dto.FavouriteReference;
import uk.gov.hmcts.reform.fact.data.api.dto.FavouriteStatus;
import uk.gov.hmcts.reform.fact.data.api.entities.Court;
import uk.gov.hmcts.reform.fact.data.api.entities.ServiceCentre;
import uk.gov.hmcts.reform.fact.data.api.entities.User;
import uk.gov.hmcts.reform.fact.data.api.entities.types.SubjectType;
import uk.gov.hmcts.reform.fact.data.api.errorhandling.exceptions.InvalidParameterCombinationException;
import uk.gov.hmcts.reform.fact.data.api.errorhandling.exceptions.NotFoundException;
import uk.gov.hmcts.reform.fact.data.api.repositories.CourtRepository;
import uk.gov.hmcts.reform.fact.data.api.repositories.ServiceCentreRepository;
import uk.gov.hmcts.reform.fact.data.api.repositories.UserRepository;
import uk.gov.hmcts.reform.fact.data.api.repositories.UserRepository.FavouriteLocationReference;

import java.time.ZonedDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
@RequiredArgsConstructor
public class UserService {

    private static final String SORT_BY_LAST_LOGIN = "lastlogin";
    private static final String SORT_ORDER_ASC = "asc";
    private static final String SORT_ORDER_DESC = "desc";

    @Value("${user.retention-period}")
    private long retentionPeriod;

    private final UserRepository userRepository;
    private final CourtRepository courtRepository;
    private final ServiceCentreRepository serviceCentreRepository;

    /**
     * Get a user by their unique identifier.
     *
     * @param userId The unique identifier of the user to find
     * @return The user entity matching the provided ID
     * @throws NotFoundException if no user exists with the given ID
     */
    public User getUserById(UUID userId) {
        return userRepository.findById(userId)
            .orElseThrow(() -> new NotFoundException("No user found for user id: " + userId));
    }

    public Page<AllLocation> getFavourites(UUID userId, int pageNumber, int pageSize) {
        validateUserExists(userId);
        Page<FavouriteLocationReference> referencePage = userRepository.findFavouriteLocationsByUserId(
            userId,
            PageRequest.of(pageNumber, pageSize)
        );

        Map<UUID, Court> courts = courtRepository.findAllById(idsFor(referencePage, SubjectType.COURT)).stream()
            .collect(Collectors.toMap(Court::getId, Function.identity()));
        Map<UUID, ServiceCentre> serviceCentres = serviceCentreRepository
            .findAllById(idsFor(referencePage, SubjectType.SERVICE_CENTRE)).stream()
            .collect(Collectors.toMap(ServiceCentre::getId, Function.identity()));

        List<AllLocation> locations = referencePage.stream()
            .map(reference -> toLocation(reference, courts, serviceCentres))
            .filter(java.util.Objects::nonNull)
            .toList();

        return new PageImpl<>(locations, referencePage.getPageable(), referencePage.getTotalElements());
    }

    public List<FavouriteStatus> getFavouriteStatuses(UUID userId, List<FavouriteReference> subjects) {
        validateUserExists(userId);
        if (subjects.isEmpty()) {
            return List.of();
        }

        List<UUID> subjectIds = subjects.stream().map(FavouriteReference::getSubjectId).distinct().toList();
        Set<FavouriteReference> favourites = userRepository.findExistingFavouriteReferences(userId, subjectIds).stream()
            .map(reference -> new FavouriteReference(
                reference.getSubjectId(),
                SubjectType.valueOf(reference.getSubjectType())
            ))
            .collect(Collectors.toSet());

        return subjects.stream()
            .map(subject -> new FavouriteStatus(
                subject.getSubjectId(),
                subject.getSubjectType(),
                favourites.contains(subject)
            ))
            .toList();
    }

    @Transactional
    public void addFavourite(UUID userId, FavouriteReference favourite) {
        validateUserExists(userId);
        validateSubjectExists(favourite.getSubjectId(), favourite.getSubjectType());
        switch (favourite.getSubjectType()) {
            case COURT -> userRepository.addFavouriteCourtIfAbsent(userId, favourite.getSubjectId());
            case SERVICE_CENTRE -> userRepository.addFavouriteServiceCentreIfAbsent(userId, favourite.getSubjectId());
        }
    }

    @Transactional
    public void removeFavourite(UUID userId, UUID subjectId, SubjectType subjectType) {
        validateUserExists(userId);
        validateSubjectExists(subjectId, subjectType);
        switch (subjectType) {
            case COURT -> userRepository.removeFavouriteCourt(userId, subjectId);
            case SERVICE_CENTRE -> userRepository.removeFavouriteServiceCentre(userId, subjectId);
        }
    }

    public Page<User> getFilteredAndPaginatedUsers(int pageNumber, int pageSize, String search,
                                                   String sortBy, String sortOrder) {
        String searchFilter = StringUtils.defaultString(search).toLowerCase(Locale.ROOT);
        Stream<User> users = userRepository.findAll().stream()
            .filter(user -> matchesSearch(user, searchFilter));

        return page(sortIfRequested(users, sortBy, sortOrder), pageNumber, pageSize);
    }

    /**
     * Creates a new user or updates an existing user's details.
     * Updates the last login time to the current timestamp.
     *
     * @param user The user entity to create or update
     * @return The saved user entity
     */
    @Transactional
    public User createOrUpdateLastLoginUser(User user) {
        Optional.ofNullable(user.getId()).flatMap(userRepository::findById)
            .or(() -> Optional.ofNullable(user.getEmail()).flatMap(userRepository::findByEmail))
            .or(() -> Optional.ofNullable(user.getSsoId()).flatMap(userRepository::findBySsoId))
            .ifPresent(
                existing -> {
                    user.setId(existing.getId());
                    user.setEmail(existing.getEmail());
                    user.setSsoId(existing.getSsoId());
                    user.setFavouriteCourts(existing.getFavouriteCourts());
                    user.setFavouriteServiceCentres(existing.getFavouriteServiceCentres());
                    user.setRole(Optional.ofNullable(user.getRole()).orElse(existing.getRole()));
                }
        );

        user.setLastLogin(ZonedDateTime.now());
        return userRepository.save(user);
    }

    /**
     * Deletes all users who haven't logged in within the retention period.
     * Users are considered inactive if their last login was more than retentionPeriod days ago.
     */
    @Transactional
    public void deleteInactiveUsers() {
        ZonedDateTime cutoffDate = ZonedDateTime.now().minusDays(retentionPeriod);
        List<User> inactiveUsers = userRepository.findAllByLastLoginBefore(cutoffDate);
        userRepository.deleteAll(inactiveUsers);
    }

    private boolean matchesSearch(User user, String searchFilter) {
        return StringUtils.isBlank(searchFilter)
            || matchesValue(user.getEmail(), searchFilter)
            || matchesValue(user.getSsoId() == null ? null : user.getSsoId().toString(), searchFilter);
    }

    private List<UUID> idsFor(Page<FavouriteLocationReference> references, SubjectType subjectType) {
        return references.stream()
            .filter(reference -> subjectType.name().equals(reference.getSubjectType()))
            .map(FavouriteLocationReference::getSubjectId)
            .toList();
    }

    private AllLocation toLocation(
        FavouriteLocationReference reference,
        Map<UUID, Court> courts,
        Map<UUID, ServiceCentre> serviceCentres
    ) {
        return switch (SubjectType.valueOf(reference.getSubjectType())) {
            case COURT -> {
                Court court = courts.get(reference.getSubjectId());
                yield court == null ? null : AllLocation.fromCourt(court);
            }
            case SERVICE_CENTRE -> {
                ServiceCentre serviceCentre = serviceCentres.get(reference.getSubjectId());
                yield serviceCentre == null ? null : AllLocation.fromServiceCentre(serviceCentre);
            }
        };
    }

    private void validateSubjectExists(UUID subjectId, SubjectType subjectType) {
        boolean exists = switch (subjectType) {
            case COURT -> courtRepository.existsById(subjectId);
            case SERVICE_CENTRE -> serviceCentreRepository.existsById(subjectId);
        };

        if (!exists) {
            throw new NotFoundException(subjectType == SubjectType.COURT
                ? "Court not found, ID: " + subjectId
                : "Service centre not found, ID: " + subjectId);
        }
    }

    private void validateUserExists(UUID userId) {
        if (!userRepository.existsById(userId)) {
            throw new NotFoundException("No user found for user id: " + userId);
        }
    }

    private boolean matchesValue(String value, String searchFilter) {
        return value != null && value.toLowerCase(Locale.ROOT).contains(searchFilter);
    }

    private List<User> sortIfRequested(Stream<User> users, String sortBy, String sortOrder) {
        if (StringUtils.isBlank(sortBy)) {
            if (!StringUtils.isBlank(sortOrder)) {
                throw new InvalidParameterCombinationException("sortOrder cannot be provided without sortBy");
            }
            return users.toList();
        }

        return users.sorted(buildComparator(sortBy, sortOrder)).toList();
    }

    private Comparator<User> buildComparator(String sortBy, String sortOrder) {
        String normalizedSortBy = sortBy.trim().toLowerCase(Locale.ROOT);
        String normalizedSortOrder = StringUtils.isBlank(sortOrder) ? SORT_ORDER_ASC
            : sortOrder.trim().toLowerCase(Locale.ROOT);

        if (!SORT_ORDER_ASC.equals(normalizedSortOrder) && !SORT_ORDER_DESC.equals(normalizedSortOrder)) {
            throw new InvalidParameterCombinationException("sortOrder must be one of: asc, desc");
        }

        if (!SORT_BY_LAST_LOGIN.equals(normalizedSortBy)) {
            throw new InvalidParameterCombinationException("sortBy must be one of: lastLogin");
        }

        Comparator<User> comparator = Comparator
            .comparing(
                User::getLastLogin,
                Comparator.nullsLast(Comparator.naturalOrder())
            )
            .thenComparing(User::getEmail, Comparator.nullsLast(Comparator.naturalOrder()))
            .thenComparing(User::getSsoId, Comparator.nullsLast(Comparator.naturalOrder()));

        return SORT_ORDER_DESC.equals(normalizedSortOrder) ? comparator.reversed() : comparator;
    }

    private Page<User> page(List<User> users, int pageNumber, int pageSize) {
        int fromIndex = Math.min(pageNumber * pageSize, users.size());
        int toIndex = Math.min(fromIndex + pageSize, users.size());

        return new PageImpl<>(
            users.subList(fromIndex, toIndex),
            PageRequest.of(pageNumber, pageSize),
            users.size()
        );
    }
}
