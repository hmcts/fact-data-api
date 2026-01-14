package uk.gov.hmcts.reform.fact.data.api.services;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.fact.data.api.entities.Court;
import uk.gov.hmcts.reform.fact.data.api.entities.User;
import uk.gov.hmcts.reform.fact.data.api.errorhandling.exceptions.NotFoundException;
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
    private UserRepository userRepository;

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
        when(courtService.getAllCourtsByIds(List.of(courtId))).thenReturn(List.of(court));

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
    void addFavouriteCourtsShouldAddCourtsToUsersFavourites() {
        UUID userId = UUID.randomUUID();
        UUID courtId = UUID.randomUUID();
        User user = new User();
        user.setFavouriteCourts(new ArrayList<>());
        Court court = new Court();
        court.setId(courtId);

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(courtService.getAllCourtsByIds(List.of(courtId))).thenReturn(List.of(court));

        userService.addFavouriteCourts(userId, List.of(courtId));

        assertThat(user.getFavouriteCourts()).contains(courtId);
    }

    @Test
    void addFavouriteCourtsShouldThrowNotFoundExceptionWhenUserDoesNotExist() {
        UUID userId = UUID.randomUUID();
        UUID courtId = UUID.randomUUID();
        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> userService.addFavouriteCourts(userId, List.of(courtId)));
    }

    @Test
    void addFavouriteCourtsShouldNotAddWhenCourtDoesNotExist() {
        UUID userId = UUID.randomUUID();
        User user = new User();
        user.setId(userId);
        user.setFavouriteCourts(new ArrayList<>());
        UUID courtId = UUID.randomUUID();

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(courtService.getAllCourtsByIds(List.of(courtId))).thenReturn(List.of());

        userService.addFavouriteCourts(userId, List.of(courtId));

        assertThat(user.getFavouriteCourts()).doesNotContain(courtId);
    }

    @Test
    void removeFavouriteCourtShouldRemoveCourtFromUsersFavourites() {
        UUID userId = UUID.randomUUID();
        UUID courtId = UUID.randomUUID();
        User user = new User();
        user.setId(userId);
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
    void createOrUpdateUserShouldSaveUserWithCurrentLoginTime() {
        User user = new User();
        ZonedDateTime beforeSave = ZonedDateTime.now();

        when(userRepository.save(user)).thenReturn(user);

        User result = userService.createOrUpdateLastLoginUser(user);

        assertThat(result.getLastLogin()).isNotNull();
        assertThat(result.getLastLogin()).isAfterOrEqualTo(beforeSave);
    }

    @Test
    void createOrUpdateUserShouldThrowExceptionWhenSaveFails() {
        User user = new User();
        when(userRepository.save(any())).thenThrow(new RuntimeException("Database error"));

        assertThrows(RuntimeException.class, () -> userService.createOrUpdateLastLoginUser(user));
    }

    @Test
    void updateExistingUserShouldPreserveEmailAndSsoidButUpdateLastLogin() {
        UUID userId = UUID.randomUUID();
        UUID existingSsoId = UUID.randomUUID();

        User existingUser = new User();
        existingUser.setId(userId);
        existingUser.setEmail("existing@email.com");
        existingUser.setSsoId(existingSsoId);
        existingUser.setFavouriteCourts(new ArrayList<>());

        User updatedUser = new User();
        updatedUser.setId(userId);
        updatedUser.setEmail("new@email.com");
        updatedUser.setSsoId(UUID.randomUUID());

        when(userRepository.findById(userId)).thenReturn(Optional.of(existingUser));

        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User savedUser = invocation.getArgument(0);
            assertThat(savedUser.getId()).isEqualTo(userId);
            assertThat(savedUser.getEmail()).isEqualTo("existing@email.com");
            assertThat(savedUser.getSsoId()).isEqualTo(existingSsoId);
            assertThat(savedUser.getFavouriteCourts()).isEqualTo(existingUser.getFavouriteCourts());
            assertThat(savedUser.getLastLogin()).isNotNull();
            return savedUser;
        });

        userService.createOrUpdateLastLoginUser(updatedUser);
    }

    @Test
    void updateExistingUserShouldPreserveEmailAndUpdateLastLoginWhenIdExists() {
        UUID userId = UUID.randomUUID();
        String existingEmail = "existing@email.com";
        UUID existingSsoId = UUID.randomUUID();

        User existingUser = new User();
        existingUser.setId(userId);
        existingUser.setEmail(existingEmail);
        existingUser.setSsoId(existingSsoId);
        existingUser.setFavouriteCourts(new ArrayList<>());

        User updatedUser = new User();
        updatedUser.setId(userId);
        updatedUser.setEmail("new@email.com");
        updatedUser.setSsoId(UUID.randomUUID());

        when(userRepository.findById(userId)).thenReturn(Optional.of(existingUser));

        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User savedUser = invocation.getArgument(0);
            assertThat(savedUser.getId()).isEqualTo(userId);
            assertThat(savedUser.getEmail()).isEqualTo(existingEmail);
            assertThat(savedUser.getSsoId()).isEqualTo(existingSsoId);
            assertThat(savedUser.getFavouriteCourts()).isEqualTo(existingUser.getFavouriteCourts());
            assertThat(savedUser.getLastLogin()).isNotNull();
            return savedUser;
        });

        userService.createOrUpdateLastLoginUser(updatedUser);
    }

    @Test
    void updateExistingUserShouldPreserveEmailAndUpdateLastLoginWhenEmailExists() {
        UUID existingId = UUID.randomUUID();
        String existingEmail = "existing@email.com";
        UUID existingSsoId = UUID.randomUUID();

        User existingUser = new User();
        existingUser.setId(existingId);
        existingUser.setEmail(existingEmail);
        existingUser.setSsoId(existingSsoId);
        existingUser.setFavouriteCourts(new ArrayList<>());

        User updatedUser = new User();
        updatedUser.setEmail(existingEmail);
        updatedUser.setSsoId(UUID.randomUUID());

        when(userRepository.findByEmail(existingEmail)).thenReturn(Optional.of(existingUser));

        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User savedUser = invocation.getArgument(0);
            assertThat(savedUser.getId()).isEqualTo(existingId);
            assertThat(savedUser.getEmail()).isEqualTo(existingEmail);
            assertThat(savedUser.getSsoId()).isEqualTo(existingSsoId);
            assertThat(savedUser.getFavouriteCourts()).isEqualTo(existingUser.getFavouriteCourts());
            assertThat(savedUser.getLastLogin()).isNotNull();
            return savedUser;
        });

        userService.createOrUpdateLastLoginUser(updatedUser);
    }

    @Test
    void updateExistingUserShouldPreserveSsoIdAndUpdateLastLoginWhenSsoIdExists() {
        UUID existingId = UUID.randomUUID();
        String existingEmail = "existing@email.com";
        UUID existingSsoId = UUID.randomUUID();

        User existingUser = new User();
        existingUser.setId(existingId);
        existingUser.setEmail(existingEmail);
        existingUser.setSsoId(existingSsoId);
        existingUser.setFavouriteCourts(new ArrayList<>());

        User updatedUser = new User();
        updatedUser.setSsoId(existingSsoId);
        updatedUser.setEmail("new@email.com");

        when(userRepository.findBySsoId(existingSsoId)).thenReturn(Optional.of(existingUser));

        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User savedUser = invocation.getArgument(0);
            assertThat(savedUser.getId()).isEqualTo(existingId);
            assertThat(savedUser.getEmail()).isEqualTo(existingEmail);
            assertThat(savedUser.getSsoId()).isEqualTo(existingSsoId);
            assertThat(savedUser.getFavouriteCourts()).isEqualTo(existingUser.getFavouriteCourts());
            assertThat(savedUser.getLastLogin()).isNotNull();
            return savedUser;
        });

        userService.createOrUpdateLastLoginUser(updatedUser);
    }

    @Test
    void deleteInactiveUsersShouldRemoveUsersNotLoggedInWithinRetentionPeriod() {
        List<User> inactiveUsers = List.of(new User());

        when(userRepository.findAllByLastLoginBefore(any())).thenReturn(inactiveUsers);

        userService.deleteInactiveUsers();

        verify(userRepository).deleteAll(inactiveUsers);
    }
}

