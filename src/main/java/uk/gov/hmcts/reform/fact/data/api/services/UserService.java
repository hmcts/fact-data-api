package uk.gov.hmcts.reform.fact.data.api.services;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.hmcts.reform.fact.data.api.entities.Court;
import uk.gov.hmcts.reform.fact.data.api.entities.User;
import uk.gov.hmcts.reform.fact.data.api.errorhandling.exceptions.NotFoundException;
import uk.gov.hmcts.reform.fact.data.api.repositories.CourtLockRepository;
import uk.gov.hmcts.reform.fact.data.api.repositories.CourtRepository;
import uk.gov.hmcts.reform.fact.data.api.repositories.UserRepository;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.UUID;

@Service
public class UserService {
    private static final long RETENTION_PERIOD_DAYS = 365;

    private final UserRepository userRepository;
    private final CourtRepository courtRepository;
    private final CourtLockRepository courtLockRepository;

    public UserService(UserRepository userRepository, CourtRepository courtRepository,
                       CourtLockRepository courtLockRepository) {
        this.userRepository = userRepository;
        this.courtRepository = courtRepository;
        this.courtLockRepository = courtLockRepository;
    }

    /**
     * Get user favourite courts by user id.
     *
     * @param userId The user id to find the favourite courts for.
     * @return A list of favourite courts or an empty list if none exist for the user.
     * @throws NotFoundException if no user record exists for the court.
     */
    public List<Court> getUsersFavouriteCourts(UUID userId) {
        List<UUID> favouriteCourtIds = userRepository.findById(userId)
            .map(User::getFavouriteCourts)
            .orElseThrow(() -> new NotFoundException("No user found for user id: " + userId));

        return courtRepository.findAllById(favouriteCourtIds);
    }


    /**
     * Adds multiple courts to a user's favourite courts list.
     *
     * @param userId   The user id to add favourite courts for
     * @param courtIds List of court IDs to add as favourites
     * @throws IllegalArgumentException if the list of court Ids is null or empty
     * @throws NotFoundException if no user or court record exists
     */
    public void addFavouriteCourt(UUID userId, List<UUID> courtIds) {
        if (courtIds == null || courtIds.isEmpty()) {
            throw new IllegalArgumentException("List of Court Ids cannot be empty");
        }
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new NotFoundException("No user found for user id: " + userId));
        for (UUID courtId : courtIds) {
            Court court = courtRepository.findById(courtId)
                .orElseThrow(() -> new NotFoundException("No court found for court id: " + courtId));
            user.getFavouriteCourts().add(court.getId());
        }
        userRepository.save(user);
    }

    /**
     * Removes a court from a user's favourite courts list.
     *
     * @param userId The user id to remove the favourite court from
     * @param favouriteCourtIds The court id to remove from favourites
     * @throws NotFoundException if no user record exists
     */
    public void removeFavouriteCourt(UUID userId, UUID favouriteCourtIds) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new NotFoundException("No user found for user id: " + userId));

        user.getFavouriteCourts().remove(favouriteCourtIds);
        userRepository.save(user);
    }

    /**
     * Delete all locks for a user during logout.
     *
     * @param userId The user id to clear locks for.
     * @throws NotFoundException if no user record exists.
     */
    @Transactional
    public void clearUserLocks(UUID userId) {
        userRepository.findById(userId)
            .orElseThrow(() -> new NotFoundException("No user found for user id: " + userId));
        courtLockRepository.deleteAllByUserId(userId);
    }

    /**
     * Creates a new user or updates an existing user's details.
     * Updates the last login time to the current timestamp.
     *
     * @param user The user entity to create or update
     * @return The saved user entity
     */
    public User createOrUpdateUser(User user) {
        user.setLastLogin(ZonedDateTime.now());
        return userRepository.save(user);
    }

    /**
     * Deletes all users who haven't logged in within the retention period.
     * Users are considered inactive if their last login was more than RETENTION_PERIOD_DAYS ago.
     */
    @Transactional
    public void deleteInactiveUsers() {
        ZonedDateTime cutoffDate = ZonedDateTime.now().minusDays(RETENTION_PERIOD_DAYS);
        List<User> inactiveUsers = userRepository.findAllByLastLoginBefore(cutoffDate);
        userRepository.deleteAll(inactiveUsers);
    }
}
