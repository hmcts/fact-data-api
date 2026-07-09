package uk.gov.hmcts.reform.fact.data.api.services;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import uk.gov.hmcts.reform.fact.data.api.entities.Court;
import uk.gov.hmcts.reform.fact.data.api.entities.User;
import uk.gov.hmcts.reform.fact.data.api.entities.types.UserRole;
import uk.gov.hmcts.reform.fact.data.api.errorhandling.exceptions.InvalidParameterCombinationException;
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
        user.setRole(UserRole.ADMIN);
        ZonedDateTime beforeSave = ZonedDateTime.now();

        when(userRepository.save(user)).thenReturn(user);

        User result = userService.createOrUpdateLastLoginUser(user);

        assertThat(result.getLastLogin()).isNotNull();
        assertThat(result.getLastLogin()).isAfterOrEqualTo(beforeSave);
    }

    @Test
    void createOrUpdateUserShouldThrowExceptionWhenSaveFails() {
        User user = new User();
        user.setRole(UserRole.ADMIN);
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
        existingUser.setRole(UserRole.ADMIN);
        existingUser.setFavouriteCourts(new ArrayList<>());

        User updatedUser = new User();
        updatedUser.setId(userId);
        updatedUser.setEmail("new@email.com");
        updatedUser.setSsoId(UUID.randomUUID());
        updatedUser.setRole(UserRole.SUPER_ADMIN);

        when(userRepository.findById(userId)).thenReturn(Optional.of(existingUser));

        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User savedUser = invocation.getArgument(0);
            assertThat(savedUser.getId()).isEqualTo(userId);
            assertThat(savedUser.getEmail()).isEqualTo("existing@email.com");
            assertThat(savedUser.getSsoId()).isEqualTo(existingSsoId);
            assertThat(savedUser.getRole()).isEqualTo(UserRole.SUPER_ADMIN);
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
        existingUser.setRole(UserRole.ADMIN);
        existingUser.setFavouriteCourts(new ArrayList<>());

        User updatedUser = new User();
        updatedUser.setId(userId);
        updatedUser.setEmail("new@email.com");
        updatedUser.setSsoId(UUID.randomUUID());
        updatedUser.setRole(UserRole.SUPER_ADMIN);

        when(userRepository.findById(userId)).thenReturn(Optional.of(existingUser));

        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User savedUser = invocation.getArgument(0);
            assertThat(savedUser.getId()).isEqualTo(userId);
            assertThat(savedUser.getEmail()).isEqualTo(existingEmail);
            assertThat(savedUser.getSsoId()).isEqualTo(existingSsoId);
            assertThat(savedUser.getRole()).isEqualTo(UserRole.SUPER_ADMIN);
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
        existingUser.setRole(UserRole.ADMIN);
        existingUser.setFavouriteCourts(new ArrayList<>());

        User updatedUser = new User();
        updatedUser.setEmail(existingEmail);
        updatedUser.setSsoId(UUID.randomUUID());
        updatedUser.setRole(UserRole.SUPER_ADMIN);

        when(userRepository.findByEmail(existingEmail)).thenReturn(Optional.of(existingUser));

        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User savedUser = invocation.getArgument(0);
            assertThat(savedUser.getId()).isEqualTo(existingId);
            assertThat(savedUser.getEmail()).isEqualTo(existingEmail);
            assertThat(savedUser.getSsoId()).isEqualTo(existingSsoId);
            assertThat(savedUser.getRole()).isEqualTo(UserRole.SUPER_ADMIN);
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
        existingUser.setRole(UserRole.ADMIN);
        existingUser.setFavouriteCourts(new ArrayList<>());

        User updatedUser = new User();
        updatedUser.setSsoId(existingSsoId);
        updatedUser.setEmail("new@email.com");
        updatedUser.setRole(UserRole.SUPER_ADMIN);

        when(userRepository.findBySsoId(existingSsoId)).thenReturn(Optional.of(existingUser));

        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User savedUser = invocation.getArgument(0);
            assertThat(savedUser.getId()).isEqualTo(existingId);
            assertThat(savedUser.getEmail()).isEqualTo(existingEmail);
            assertThat(savedUser.getSsoId()).isEqualTo(existingSsoId);
            assertThat(savedUser.getRole()).isEqualTo(UserRole.SUPER_ADMIN);
            assertThat(savedUser.getFavouriteCourts()).isEqualTo(existingUser.getFavouriteCourts());
            assertThat(savedUser.getLastLogin()).isNotNull();
            return savedUser;
        });

        userService.createOrUpdateLastLoginUser(updatedUser);
    }

    @Test
    void getFilteredAndPaginatedUsersShouldReturnPagedUsers() {
        User firstUser = createUser("first@justice.gov.uk", UUID.randomUUID(), UserRole.ADMIN,
                                    ZonedDateTime.now().minusDays(2));
        User secondUser = createUser("second@justice.gov.uk", UUID.randomUUID(), UserRole.SUPER_ADMIN,
                                     ZonedDateTime.now().minusDays(1));
        when(userRepository.findAll()).thenReturn(List.of(firstUser, secondUser));

        Page<User> result = userService.getFilteredAndPaginatedUsers(0, 1, null, null, null);

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().getFirst().getEmail()).isEqualTo("first@justice.gov.uk");
        assertThat(result.getContent().getFirst().getRole()).isEqualTo(UserRole.ADMIN);
        assertThat(result.getTotalElements()).isEqualTo(2);
    }

    @Test
    void getFilteredAndPaginatedUsersShouldSearchByEmailOrSsoId() {
        UUID matchingSsoId = UUID.fromString("123e4567-e89b-12d3-a456-426614174000");
        User emailMatch = createUser("match@justice.gov.uk", UUID.randomUUID(), UserRole.ADMIN,
                                     ZonedDateTime.now().minusDays(2));
        User ssoMatch = createUser("other@justice.gov.uk", matchingSsoId, UserRole.SUPER_ADMIN,
                                   ZonedDateTime.now().minusDays(1));
        User nonMatch = createUser("ignored@justice.gov.uk", UUID.randomUUID(), UserRole.ADMIN,
                                   ZonedDateTime.now());
        when(userRepository.findAll()).thenReturn(List.of(emailMatch, ssoMatch, nonMatch));

        Page<User> emailResult = userService.getFilteredAndPaginatedUsers(0, 25, "MATCH", null, null);
        Page<User> ssoResult = userService.getFilteredAndPaginatedUsers(0, 25, "123e4567", null, null);

        assertThat(emailResult.getContent()).extracting(User::getEmail)
            .containsExactly("match@justice.gov.uk");
        assertThat(ssoResult.getContent()).extracting(User::getEmail)
            .containsExactly("other@justice.gov.uk");
    }

    @Test
    void getFilteredAndPaginatedUsersShouldSortByLastLogin() {
        User olderUser = createUser("older@justice.gov.uk", UUID.randomUUID(), UserRole.ADMIN,
                                    ZonedDateTime.now().minusDays(2));
        User newerUser = createUser("newer@justice.gov.uk", UUID.randomUUID(), UserRole.SUPER_ADMIN,
                                    ZonedDateTime.now().minusDays(1));
        when(userRepository.findAll()).thenReturn(List.of(olderUser, newerUser));

        Page<User> ascendingResult =
            userService.getFilteredAndPaginatedUsers(0, 25, null, "lastLogin", "asc");
        Page<User> descendingResult =
            userService.getFilteredAndPaginatedUsers(0, 25, null, "lastLogin", "desc");

        assertThat(ascendingResult.getContent()).extracting(User::getEmail)
            .containsExactly("older@justice.gov.uk", "newer@justice.gov.uk");
        assertThat(descendingResult.getContent()).extracting(User::getEmail)
            .containsExactly("newer@justice.gov.uk", "older@justice.gov.uk");
    }

    @Test
    void getFilteredAndPaginatedUsersShouldRejectInvalidSortParameters() {
        InvalidParameterCombinationException missingSortBy = assertThrows(
            InvalidParameterCombinationException.class,
            () -> userService.getFilteredAndPaginatedUsers(0, 25, null, null, "asc")
        );
        InvalidParameterCombinationException invalidSortBy = assertThrows(
            InvalidParameterCombinationException.class,
            () -> userService.getFilteredAndPaginatedUsers(0, 25, null, "email", "asc")
        );
        InvalidParameterCombinationException invalidSortOrder = assertThrows(
            InvalidParameterCombinationException.class,
            () -> userService.getFilteredAndPaginatedUsers(0, 25, null, "lastLogin", "sideways")
        );

        assertThat(missingSortBy).hasMessage("sortOrder cannot be provided without sortBy");
        assertThat(invalidSortBy).hasMessage("sortBy must be one of: lastLogin");
        assertThat(invalidSortOrder).hasMessage("sortOrder must be one of: asc, desc");
    }

    @Test
    void deleteInactiveUsersShouldRemoveUsersNotLoggedInWithinRetentionPeriod() {
        List<User> inactiveUsers = List.of(new User());

        when(userRepository.findAllByLastLoginBefore(any())).thenReturn(inactiveUsers);

        userService.deleteInactiveUsers();

        verify(userRepository).deleteAll(inactiveUsers);
    }

    private User createUser(String email, UUID ssoId, UserRole role, ZonedDateTime lastLogin) {
        User user = new User();
        user.setEmail(email);
        user.setSsoId(ssoId);
        user.setRole(role);
        user.setLastLogin(lastLogin);
        return user;
    }
}
