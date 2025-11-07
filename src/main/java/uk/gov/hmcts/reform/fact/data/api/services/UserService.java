package uk.gov.hmcts.reform.fact.data.api.services;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.hmcts.reform.fact.data.api.entities.Court;
import uk.gov.hmcts.reform.fact.data.api.entities.User;
import uk.gov.hmcts.reform.fact.data.api.errorhandling.exceptions.NotFoundException;
import uk.gov.hmcts.reform.fact.data.api.repositories.UserRepository;

import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class UserService {

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
