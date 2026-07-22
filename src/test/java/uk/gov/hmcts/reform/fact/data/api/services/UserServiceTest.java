package uk.gov.hmcts.reform.fact.data.api.services;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import uk.gov.hmcts.reform.fact.data.api.dto.AllLocation;
import uk.gov.hmcts.reform.fact.data.api.dto.FavouriteReference;
import uk.gov.hmcts.reform.fact.data.api.dto.FavouriteStatus;
import uk.gov.hmcts.reform.fact.data.api.entities.Court;
import uk.gov.hmcts.reform.fact.data.api.entities.ServiceCentre;
import uk.gov.hmcts.reform.fact.data.api.entities.User;
import uk.gov.hmcts.reform.fact.data.api.entities.types.SubjectType;
import uk.gov.hmcts.reform.fact.data.api.entities.types.UserRole;
import uk.gov.hmcts.reform.fact.data.api.errorhandling.exceptions.InvalidParameterCombinationException;
import uk.gov.hmcts.reform.fact.data.api.errorhandling.exceptions.NotFoundException;
import uk.gov.hmcts.reform.fact.data.api.repositories.CourtRepository;
import uk.gov.hmcts.reform.fact.data.api.repositories.ServiceCentreRepository;
import uk.gov.hmcts.reform.fact.data.api.repositories.UserRepository;
import uk.gov.hmcts.reform.fact.data.api.repositories.UserRepository.FavouriteLocationReference;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    private static final UUID USER_ID = UUID.randomUUID();
    private static final UUID COURT_ID = UUID.randomUUID();
    private static final UUID SERVICE_CENTRE_ID = UUID.randomUUID();

    @Mock
    private UserRepository userRepository;

    @Mock
    private CourtRepository courtRepository;

    @Mock
    private ServiceCentreRepository serviceCentreRepository;

    @InjectMocks
    private UserService userService;

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
    void createOrUpdateUserShouldSaveViewerRole() {
        User user = new User();
        user.setRole(UserRole.VIEWER);

        when(userRepository.save(user)).thenReturn(user);

        User result = userService.createOrUpdateLastLoginUser(user);

        assertThat(result.getRole()).isEqualTo(UserRole.VIEWER);
        verify(userRepository).save(user);
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
        List<UUID> favouriteCourts = List.of(UUID.randomUUID());
        List<UUID> favouriteServiceCentres = List.of(UUID.randomUUID());
        existingUser.setFavouriteCourts(favouriteCourts);
        existingUser.setFavouriteServiceCentres(favouriteServiceCentres);

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
            assertThat(savedUser.getFavouriteCourts()).isEqualTo(favouriteCourts);
            assertThat(savedUser.getFavouriteServiceCentres()).isEqualTo(favouriteServiceCentres);
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
    void getFavouritesHydratesMixedLocationsInRepositoryOrder() {
        when(userRepository.existsById(USER_ID)).thenReturn(true);
        FavouriteLocationReference serviceCentreReference = reference(
            SERVICE_CENTRE_ID,
            SubjectType.SERVICE_CENTRE
        );
        FavouriteLocationReference courtReference = reference(COURT_ID, SubjectType.COURT);
        PageRequest pageable = PageRequest.of(0, 25);
        when(userRepository.findFavouriteLocationsByUserId(USER_ID, pageable))
            .thenReturn(new PageImpl<>(List.of(serviceCentreReference, courtReference), pageable, 2));
        when(courtRepository.findAllById(List.of(COURT_ID))).thenReturn(List.of(Court.builder()
            .id(COURT_ID)
            .name("Beta Court")
            .build()));
        when(serviceCentreRepository.findAllById(List.of(SERVICE_CENTRE_ID)))
            .thenReturn(List.of(ServiceCentre.builder()
                .id(SERVICE_CENTRE_ID)
                .name("Alpha Service Centre")
                .build()));

        Page<AllLocation> result = userService.getFavourites(USER_ID, 0, 25);

        assertThat(result.getContent())
            .extracting(AllLocation::getId)
            .containsExactly(SERVICE_CENTRE_ID, COURT_ID);
        assertThat(result.getTotalElements()).isEqualTo(2);
    }

    @Test
    void getFavouritesDefensivelyOmitsADeletedLocation() {
        when(userRepository.existsById(USER_ID)).thenReturn(true);
        FavouriteLocationReference courtReference = reference(COURT_ID, SubjectType.COURT);
        PageRequest pageable = PageRequest.of(0, 25);
        when(userRepository.findFavouriteLocationsByUserId(USER_ID, pageable))
            .thenReturn(new PageImpl<>(List.of(courtReference), pageable, 1));
        when(courtRepository.findAllById(List.of(COURT_ID))).thenReturn(List.of());
        when(serviceCentreRepository.findAllById(List.of())).thenReturn(List.of());

        assertThat(userService.getFavourites(USER_ID, 0, 25).getContent()).isEmpty();
    }

    @Test
    void getFavouriteStatusesReturnsInputOrderAndFalseForUnmatchedSubjects() {
        when(userRepository.existsById(USER_ID)).thenReturn(true);
        FavouriteReference court = new FavouriteReference(COURT_ID, SubjectType.COURT);
        FavouriteReference serviceCentre = new FavouriteReference(
            SERVICE_CENTRE_ID,
            SubjectType.SERVICE_CENTRE
        );
        when(userRepository.findExistingFavouriteReferences(
            USER_ID,
            List.of(SERVICE_CENTRE_ID, COURT_ID)
        )).thenReturn(List.of(reference(COURT_ID, SubjectType.COURT)));

        List<FavouriteStatus> result = userService.getFavouriteStatuses(USER_ID, List.of(serviceCentre, court));

        assertThat(result).containsExactly(
            new FavouriteStatus(SERVICE_CENTRE_ID, SubjectType.SERVICE_CENTRE, false),
            new FavouriteStatus(COURT_ID, SubjectType.COURT, true)
        );
    }

    @Test
    void getFavouriteStatusesDoesNotQueryForAnEmptyList() {
        when(userRepository.existsById(USER_ID)).thenReturn(true);
        assertThat(userService.getFavouriteStatuses(USER_ID, List.of())).isEmpty();
        verify(userRepository, never()).findExistingFavouriteReferences(any(), any());
    }

    @Test
    void addFavouriteValidatesCourtAndUsesIdempotentInsert() {
        when(userRepository.existsById(USER_ID)).thenReturn(true);
        when(courtRepository.existsById(COURT_ID)).thenReturn(true);

        userService.addFavourite(USER_ID, new FavouriteReference(COURT_ID, SubjectType.COURT));

        verify(userRepository).addFavouriteCourtIfAbsent(USER_ID, COURT_ID);
    }

    @Test
    void addFavouriteValidatesServiceCentre() {
        when(userRepository.existsById(USER_ID)).thenReturn(true);
        when(serviceCentreRepository.existsById(SERVICE_CENTRE_ID)).thenReturn(true);

        userService.addFavourite(
            USER_ID,
            new FavouriteReference(SERVICE_CENTRE_ID, SubjectType.SERVICE_CENTRE)
        );

        verify(userRepository).addFavouriteServiceCentreIfAbsent(USER_ID, SERVICE_CENTRE_ID);
    }

    @Test
    void addFavouriteRejectsUnknownSubject() {
        when(userRepository.existsById(USER_ID)).thenReturn(true);
        when(courtRepository.existsById(COURT_ID)).thenReturn(false);

        assertThatThrownBy(() -> userService.addFavourite(
            USER_ID,
            new FavouriteReference(COURT_ID, SubjectType.COURT)
        )).isInstanceOf(NotFoundException.class)
            .hasMessage("Court not found, ID: " + COURT_ID);

        verify(userRepository, never()).addFavouriteCourtIfAbsent(any(), any());
    }

    @Test
    void removeFavouriteIsIdempotentAfterValidatingSubject() {
        when(userRepository.existsById(USER_ID)).thenReturn(true);
        when(serviceCentreRepository.existsById(SERVICE_CENTRE_ID)).thenReturn(true);

        userService.removeFavourite(USER_ID, SERVICE_CENTRE_ID, SubjectType.SERVICE_CENTRE);

        verify(userRepository).removeFavouriteServiceCentre(USER_ID, SERVICE_CENTRE_ID);
    }

    @Test
    void getFavouritesRejectsUnknownUser() {
        when(userRepository.existsById(USER_ID)).thenReturn(false);

        assertThatThrownBy(() -> userService.getFavourites(USER_ID, 0, 25))
            .isInstanceOf(NotFoundException.class)
            .hasMessage("No user found for user id: " + USER_ID);

        verify(userRepository, never()).findFavouriteLocationsByUserId(any(), any());
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

    private FavouriteLocationReference reference(UUID subjectId, SubjectType subjectType) {
        return new FavouriteLocationReference() {
            @Override
            public UUID getSubjectId() {
                return subjectId;
            }

            @Override
            public String getSubjectType() {
                return subjectType.name();
            }
        };
    }
}
