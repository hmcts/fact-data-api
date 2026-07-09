package uk.gov.hmcts.reform.fact.data.api.services;

import org.apache.commons.lang3.StringUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.hmcts.reform.fact.data.api.entities.Court;
import uk.gov.hmcts.reform.fact.data.api.entities.User;
import uk.gov.hmcts.reform.fact.data.api.errorhandling.exceptions.InvalidParameterCombinationException;
import uk.gov.hmcts.reform.fact.data.api.errorhandling.exceptions.NotFoundException;
import uk.gov.hmcts.reform.fact.data.api.repositories.UserRepository;

import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;

@Service
public class UserService {

    private static final String SORT_BY_LAST_LOGIN = "lastlogin";
    private static final String SORT_ORDER_ASC = "asc";
    private static final String SORT_ORDER_DESC = "desc";

    @Value("${user.retention-period}")
    private long retentionPeriod;

    private final UserRepository userRepository;
    private final CourtService courtService;

    public UserService(UserRepository userRepository, CourtService courtService) {
        this.userRepository = userRepository;
        this.courtService = courtService;
    }

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

    public Page<User> getFilteredAndPaginatedUsers(int pageNumber, int pageSize, String search,
                                                   String sortBy, String sortOrder) {
        String searchFilter = StringUtils.defaultString(search).toLowerCase(Locale.ROOT);
        Stream<User> users = userRepository.findAll().stream()
            .filter(user -> matchesSearch(user, searchFilter));

        return page(sortIfRequested(users, sortBy, sortOrder), pageNumber, pageSize);
    }

    /**
     * Get user favourite courts by user id.
     *
     * @param userId The user id to find the favourite courts for.
     * @return A list of favourite courts or an empty list if none exist for the user.
     * @throws NotFoundException if no user record exists for the court.
     */
    public List<Court> getUsersFavouriteCourts(UUID userId) {
        return courtService.getAllCourtsByIds(
            Optional.ofNullable(this.getUserById(userId).getFavouriteCourts()).orElse(List.of()));
    }

    /**
     * Adds multiple courts to a user's favourite courts list.
     *
     * @param userId   The user id to add favourite courts for
     * @param courtIds List of court IDs to add as favourites
     * @throws NotFoundException if no user or court record exists
     */
    public void addFavouriteCourts(UUID userId, List<UUID> courtIds) {
        User user = this.getUserById(userId);

        List<UUID> validCourtIds = courtService.getAllCourtsByIds(courtIds).stream()
            .map(Court::getId)
            .toList();

        List<UUID> favouriteCourtIds = new ArrayList<>(
            Optional.ofNullable(user.getFavouriteCourts()).orElse(List.of())
        );

        // Add only court Ids that don't already exist in the user's favourites
        validCourtIds.stream()
            .filter(courtId -> !favouriteCourtIds.contains(courtId))
            .forEach(favouriteCourtIds::add);

        user.setFavouriteCourts(favouriteCourtIds);
        userRepository.save(user);
    }

    /**
     * Removes a court from a user's favourite courts list.
     *
     * @param userId The user id to remove the favourite court from
     * @param favouriteCourtId The court id to remove from favourites
     * @throws NotFoundException if no user record exists
     */
    public void removeFavouriteCourt(UUID userId, UUID favouriteCourtId) {
        User user = this.getUserById(userId);

        user.getFavouriteCourts().remove(favouriteCourtId);
        userRepository.save(user);
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
