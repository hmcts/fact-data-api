package uk.gov.hmcts.reform.fact.data.api.services;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.hmcts.reform.fact.data.api.entities.Court;
import uk.gov.hmcts.reform.fact.data.api.entities.User;
import uk.gov.hmcts.reform.fact.data.api.errorhandling.exceptions.NotFoundException;
import uk.gov.hmcts.reform.fact.data.api.repositories.UserRepository;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class UserService {

    private final CourtLockService courtLockService;
    @Value("${user.retention-period:365}")
    private long retentionPeriod;

    private final UserRepository userRepository;
    private final CourtService courtService;

    public UserService(UserRepository userRepository, CourtService courtService, CourtLockService courtLockService) {
        this.userRepository = userRepository;
        this.courtService = courtService;
        this.courtLockService = courtLockService;
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

        return courtService.getAllCourtsByIds(favouriteCourtIds);
    }

    /**
     * Adds multiple courts to a user's favourite courts list.
     *
     * @param userId   The user id to add favourite courts for
     * @param courtIds List of court IDs to add as favourites
     * @throws NotFoundException if no user or court record exists
     */
    public void addFavouriteCourt(UUID userId, List<UUID> courtIds) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new NotFoundException("No user found for user id: " + userId));

        List<UUID> validCourtIds = courtIds.stream()
            .map(courtId -> courtService.getCourtById(courtId).getId())
            .toList();

        user.getFavouriteCourts().addAll(validCourtIds);
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
        courtLockService.deleteLocksByUserId(userId);
    }

    /**
     * Creates a new user or updates an existing user's details.
     * Updates the last login time to the current timestamp.
     *
     * @param user The user entity to create or update
     * @return The saved user entity
     */
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
}
