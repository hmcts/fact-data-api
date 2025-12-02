package uk.gov.hmcts.reform.fact.data.api.controllers;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import uk.gov.hmcts.reform.fact.data.api.entities.Court;
import uk.gov.hmcts.reform.fact.data.api.entities.User;
import uk.gov.hmcts.reform.fact.data.api.errorhandling.exceptions.NotFoundException;
import uk.gov.hmcts.reform.fact.data.api.services.CourtLockService;
import uk.gov.hmcts.reform.fact.data.api.services.UserService;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserControllerTest {

    private static final UUID USER_ID = UUID.randomUUID();
    private static final UUID UNKNOWN_USER_ID = UUID.randomUUID();
    private static final UUID COURT_ID = UUID.randomUUID();
    private static final UUID FAVOURITE_ID = UUID.randomUUID();
    private static final String INVALID_UUID = "abcde";

    private static final String RESPONSE_STATUS_MESSAGE = "Response status does not match";
    private static final String RESPONSE_BODY_MESSAGE = "Response body does not match";

    @Mock
    private UserService userService;

    @Mock
    private CourtLockService courtLockService;

    @InjectMocks
    private UserController userController;

    @Test
    void getUserFavoritesReturns200() {
        List<Court> courts = List.of(new Court());
        when(userService.getUsersFavouriteCourts(USER_ID)).thenReturn(courts);

        ResponseEntity<List<Court>> response = userController.getUserFavorites(USER_ID.toString());

        assertThat(response.getStatusCode()).as(RESPONSE_STATUS_MESSAGE).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).as(RESPONSE_BODY_MESSAGE).isEqualTo(courts);
    }

    @Test
    void getUserFavoritesReturnsEmptyList() {
        when(userService.getUsersFavouriteCourts(USER_ID)).thenReturn(List.of());

        ResponseEntity<List<Court>> response = userController.getUserFavorites(USER_ID.toString());

        assertThat(response.getStatusCode()).as(RESPONSE_STATUS_MESSAGE).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).as(RESPONSE_BODY_MESSAGE).isEmpty();
    }

    @Test
    void getUserFavoritesThrowsNotFoundException() {
        when(userService.getUsersFavouriteCourts(UNKNOWN_USER_ID))
            .thenThrow(new NotFoundException("User not found"));

        assertThrows(
            NotFoundException.class, () ->
                userController.getUserFavorites(UNKNOWN_USER_ID.toString())
        );
    }

    @Test
    void getUserFavoritesThrowsIllegalArgumentExceptionForInvalidUUID() {
        assertThrows(
            IllegalArgumentException.class, () ->
                userController.getUserFavorites(INVALID_UUID)
        );
    }

    @Test
    void addUserFavoritesReturns201() {
        List<UUID> courtIds = List.of(COURT_ID);

        ResponseEntity<Void> response = userController.addUserFavorites(USER_ID.toString(), courtIds);

        assertThat(response.getStatusCode()).as(RESPONSE_STATUS_MESSAGE).isEqualTo(HttpStatus.CREATED);
    }

    @Test
    void addUserFavoritesThrowsNotFoundExceptionWhenUserNotFound() {
        List<UUID> courtIds = List.of(COURT_ID);
        doThrow(new NotFoundException("User not found"))
            .when(userService).addFavouriteCourts(UNKNOWN_USER_ID, courtIds);

        assertThrows(
            NotFoundException.class, () ->
                userController.addUserFavorites(UNKNOWN_USER_ID.toString(), courtIds)
        );
    }

    @Test
    void addUserFavoritesThrowsIllegalArgumentExceptionForInvalidUUID() {
        List<UUID> courtIds = List.of(COURT_ID);

        assertThrows(
            IllegalArgumentException.class, () ->
                userController.addUserFavorites(INVALID_UUID, courtIds)
        );
    }

    @Test
    void deleteUserFavoriteReturns204() {
        ResponseEntity<Void> response = userController.deleteUserFavorite(USER_ID.toString(), FAVOURITE_ID.toString());

        assertThat(response.getStatusCode()).as(RESPONSE_STATUS_MESSAGE).isEqualTo(HttpStatus.NO_CONTENT);
    }

    @Test
    void deleteUserFavoriteThrowsNotFoundExceptionWhenUserNotFound() {
        doThrow(new NotFoundException("User not found"))
            .when(userService).removeFavouriteCourt(UNKNOWN_USER_ID, FAVOURITE_ID);

        assertThrows(
            NotFoundException.class, () ->
                userController.deleteUserFavorite(UNKNOWN_USER_ID.toString(), FAVOURITE_ID.toString())
        );
    }

    @Test
    void deleteUserFavoriteThrowsIllegalArgumentExceptionForInvalidUUID() {
        assertThrows(
            IllegalArgumentException.class, () ->
                userController.deleteUserFavorite(INVALID_UUID, FAVOURITE_ID.toString())
        );
    }

    @Test
    void clearUserLocksReturns204() {
        ResponseEntity<Void> response = userController.clearUserLocks(USER_ID.toString());

        assertThat(response.getStatusCode()).as(RESPONSE_STATUS_MESSAGE).isEqualTo(HttpStatus.NO_CONTENT);
    }

    @Test
    void clearUserLocksThrowsNotFoundExceptionWhenUserNotFound() {
        doThrow(new NotFoundException("User not found"))
            .when(courtLockService).clearUserLocks(UNKNOWN_USER_ID);

        assertThrows(
            NotFoundException.class, () ->
                userController.clearUserLocks(UNKNOWN_USER_ID.toString())
        );
    }

    @Test
    void clearUserLocksThrowsIllegalArgumentExceptionForInvalidUUID() {
        assertThrows(
            IllegalArgumentException.class, () ->
                userController.clearUserLocks(INVALID_UUID)
        );
    }

    @Test
    void createOrUpdateUserReturns201() {
        User user = new User();
        when(userService.createOrUpdateLastLoginUser(user)).thenReturn(user);

        ResponseEntity<User> response = userController.createOrUpdateLastLoginUser(user);

        assertThat(response.getStatusCode()).as(RESPONSE_STATUS_MESSAGE).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).as(RESPONSE_BODY_MESSAGE).isEqualTo(user);
    }

    @Test
    void deleteInactiveUsersReturns204() {
        ResponseEntity<Void> response = userController.deleteInactiveUsers();

        assertThat(response.getStatusCode()).as(RESPONSE_STATUS_MESSAGE).isEqualTo(HttpStatus.NO_CONTENT);
    }
}
