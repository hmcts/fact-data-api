package uk.gov.hmcts.reform.fact.data.api.controllers;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import uk.gov.hmcts.reform.fact.data.api.dto.AllLocation;
import uk.gov.hmcts.reform.fact.data.api.dto.FavouriteReference;
import uk.gov.hmcts.reform.fact.data.api.dto.FavouriteStatus;
import uk.gov.hmcts.reform.fact.data.api.dto.FavouriteStatusRequest;
import uk.gov.hmcts.reform.fact.data.api.entities.User;
import uk.gov.hmcts.reform.fact.data.api.entities.types.SubjectType;
import uk.gov.hmcts.reform.fact.data.api.errorhandling.exceptions.NotFoundException;
import uk.gov.hmcts.reform.fact.data.api.services.LockService;
import uk.gov.hmcts.reform.fact.data.api.services.UserService;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserControllerTest {

    private static final UUID USER_ID = UUID.randomUUID();
    private static final UUID UNKNOWN_USER_ID = UUID.randomUUID();
    private static final String INVALID_UUID = "abcde";

    private static final String RESPONSE_STATUS_MESSAGE = "Response status does not match";
    private static final String RESPONSE_BODY_MESSAGE = "Response body does not match";

    @Mock
    private UserService userService;

    @Mock
    private LockService lockService;

    @InjectMocks
    private UserController userController;

    @Test
    void getFilteredAndPaginatedUsersReturns200() {
        Page<User> users = new PageImpl<>(List.of(new User()));
        when(userService.getFilteredAndPaginatedUsers(0, 25, "admin", "lastLogin", "desc")).thenReturn(users);

        ResponseEntity<Page<User>> response =
            userController.getFilteredAndPaginatedUsers(0, 25, "admin", "lastLogin", "desc");

        assertThat(response.getStatusCode()).as(RESPONSE_STATUS_MESSAGE).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).as(RESPONSE_BODY_MESSAGE).isEqualTo(users);
    }

    @Test
    void getFavouritesUsesCurrentUser() {
        Page<AllLocation> favourites = new PageImpl<>(List.of(new AllLocation()));
        when(userService.getFavourites(USER_ID, 0, 25)).thenReturn(favourites);

        ResponseEntity<Page<AllLocation>> response = userController.getFavourites(USER_ID, 0, 25);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo(favourites);
    }

    @Test
    void addFavouriteUsesCurrentUser() {
        FavouriteReference favourite = new FavouriteReference(USER_ID, SubjectType.COURT);
        assertThat(userController.addFavourite(USER_ID, favourite).getStatusCode()).isEqualTo(HttpStatus.CREATED);
        verify(userService).addFavourite(USER_ID, favourite);
    }

    @Test
    void getStatusesUsesCurrentUser() {
        FavouriteReference favourite = new FavouriteReference(USER_ID, SubjectType.COURT);
        List<FavouriteStatus> statuses = List.of(new FavouriteStatus(USER_ID, SubjectType.COURT, true));
        when(userService.getFavouriteStatuses(USER_ID, List.of(favourite))).thenReturn(statuses);

        ResponseEntity<List<FavouriteStatus>> response = userController.getStatuses(
            USER_ID,
            new FavouriteStatusRequest(List.of(favourite))
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo(statuses);
    }

    @Test
    void removeFavouriteUsesCurrentUser() {
        assertThat(userController.removeFavourite(USER_ID, SubjectType.COURT, USER_ID.toString()).getStatusCode())
            .isEqualTo(HttpStatus.NO_CONTENT);
        verify(userService).removeFavourite(USER_ID, USER_ID, SubjectType.COURT);
    }

    @Test
    void clearUserLocksReturns204() {
        ResponseEntity<Void> response = userController.clearUserLocks(USER_ID.toString());

        assertThat(response.getStatusCode()).as(RESPONSE_STATUS_MESSAGE).isEqualTo(HttpStatus.NO_CONTENT);
    }

    @Test
    void clearUserLocksThrowsNotFoundExceptionWhenUserNotFound() {
        doThrow(new NotFoundException("User not found"))
            .when(lockService).clearUserLocks(UNKNOWN_USER_ID);

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
