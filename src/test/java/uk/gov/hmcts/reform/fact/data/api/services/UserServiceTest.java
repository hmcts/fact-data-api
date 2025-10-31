package uk.gov.hmcts.reform.fact.data.api.services;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.fact.data.api.entities.Court;
import uk.gov.hmcts.reform.fact.data.api.entities.User;
import uk.gov.hmcts.reform.fact.data.api.errorhandling.exceptions.NotFoundException;
import uk.gov.hmcts.reform.fact.data.api.repositories.CourtLockRepository;
import uk.gov.hmcts.reform.fact.data.api.repositories.CourtRepository;
import uk.gov.hmcts.reform.fact.data.api.repositories.UserRepository;

import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private CourtRepository courtRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private CourtLockRepository courtLockRepository;

    @Mock
    private CourtService courtService;

    @InjectMocks
    private UserService userService;

    @Test
    void getUsersFavouriteCourtsShouldReturnListOfCourts() {
        UUID userId = UUID.randomUUID();
        UUID courtId = UUID.randomUUID();
        User user = new User();
        user.setFavouriteCourts(List.of(courtId));
        Court court = new Court();
        court.setId(courtId);

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(courtRepository.findAllById(List.of(courtId))).thenReturn(List.of(court));

        List<Court> result = userService.getUsersFavouriteCourts(userId);

        assertThat(result).hasSize(1);
        assertThat(result.getFirst().getId()).isEqualTo(courtId);
    }

    @Test
    void getUsersFavouriteCourtsShouldThrowNotFoundExceptionWhenUserDoesNotExist() {
        UUID userId = UUID.randomUUID();
        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> userService.getUsersFavouriteCourts(userId));
    }

    @Test
    void addFavouriteCourtShouldAddCourtsToUsersFavourites() {
        UUID userId = UUID.randomUUID();
        UUID courtId = UUID.randomUUID();
        User user = new User();
        user.setFavouriteCourts(new ArrayList<>());
        Court court = new Court();
        court.setId(courtId);

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(courtService.getCourtById(courtId)).thenReturn(court);

        userService.addFavouriteCourt(userId, List.of(courtId));

        assertThat(user.getFavouriteCourts()).contains(courtId);
    }

    @Test
    void addFavouriteCourtShouldThrowNotFoundExceptionWhenUserDoesNotExist() {
        UUID userId = UUID.randomUUID();
        UUID courtId = UUID.randomUUID();
        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> userService.addFavouriteCourt(userId, List.of(courtId)));
    }

    @Test
    void addFavouriteCourtShouldThrowNotFoundExceptionWhenCourtDoesNotExist() {
        UUID userId = UUID.randomUUID();
        UUID courtId = UUID.randomUUID();
        User user = new User();
        user.setFavouriteCourts(new ArrayList<>());

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(courtService.getCourtById(courtId)).thenThrow(new NotFoundException("Court not found"));

        assertThrows(NotFoundException.class, () ->
            userService.addFavouriteCourt(userId, List.of(courtId))
        );
    }

    @Test
    void addFavouriteCourtShouldThrowIllegalArgumentExceptionWhenCourtIdsIsEmpty() {
        UUID userId = UUID.randomUUID();

        assertThrows(IllegalArgumentException.class, () -> userService.addFavouriteCourt(userId, List.of()));
    }

    @Test
    void removeFavouriteCourtShouldRemoveCourtFromUsersFavourites() {
        UUID userId = UUID.randomUUID();
        UUID courtId = UUID.randomUUID();
        User user = new User();
        user.setFavouriteCourts(new ArrayList<>(List.of(courtId)));

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));

        userService.removeFavouriteCourt(userId, courtId);

        assertThat(user.getFavouriteCourts()).doesNotContain(courtId);
    }

    @Test
    void removeFavouriteCourtShouldThrowNotFoundExceptionWhenUserDoesNotExist() {
        UUID userId = UUID.randomUUID();
        UUID courtId = UUID.randomUUID();
        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> userService.removeFavouriteCourt(userId, courtId));
    }

    @Test
    void clearUserLocksShouldDeleteAllLocksForUser() {
        UUID userId = UUID.randomUUID();
        User user = new User();

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));

        userService.clearUserLocks(userId);

        verify(courtLockRepository).deleteAllByUserId(userId);
    }

    @Test
    void clearUserLocksShouldThrowNotFoundExceptionWhenUserDoesNotExist() {
        UUID userId = UUID.randomUUID();
        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> userService.clearUserLocks(userId));
    }

    @Test
    void createOrUpdateUserShouldSaveUserWithCurrentLoginTime() {
        User user = new User();
        ZonedDateTime beforeSave = ZonedDateTime.now();

        when(userRepository.save(user)).thenReturn(user);

        User result = userService.createOrUpdateUser(user);

        assertThat(result.getLastLogin()).isNotNull();
        assertThat(result.getLastLogin()).isAfterOrEqualTo(beforeSave);
    }

    @Test
    void createOrUpdateUserShouldThrowExceptionWhenSaveFails() {
        User user = new User();
        when(userRepository.save(any())).thenThrow(new RuntimeException("Database error"));

        assertThrows(RuntimeException.class, () -> userService.createOrUpdateUser(user));
    }

    @Test
    void deleteInactiveUsersShouldRemoveUsersNotLoggedInWithinRetentionPeriod() {
        List<User> inactiveUsers = List.of(new User());

        when(userRepository.findAllByLastLoginBefore(any())).thenReturn(inactiveUsers);

        userService.deleteInactiveUsers();

        verify(userRepository).deleteAll(inactiveUsers);
    }
}

