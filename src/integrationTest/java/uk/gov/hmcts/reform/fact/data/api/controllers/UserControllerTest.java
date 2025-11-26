package uk.gov.hmcts.reform.fact.data.api.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import uk.gov.hmcts.reform.fact.data.api.entities.Court;
import uk.gov.hmcts.reform.fact.data.api.entities.User;
import uk.gov.hmcts.reform.fact.data.api.errorhandling.exceptions.NotFoundException;
import uk.gov.hmcts.reform.fact.data.api.services.CourtLockService;
import uk.gov.hmcts.reform.fact.data.api.services.UserService;

import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(UserController.class)
class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private UserService userService;

    @MockitoBean
    private CourtLockService courtLockService;

    private final UUID userId = UUID.fromString("123e4567-e89b-12d3-a456-426614174000");
    private final UUID nonExistentUserId = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private final UUID courtId = UUID.fromString("222e4567-e89b-12d3-a456-426614174000");

    @Test
    @DisplayName("GET /user/v1/{userId}/favourites returns favourite courts successfully")
    void getUserFavoritesReturnsSuccessfully() throws Exception {
        List<Court> courts = List.of(new Court());
        when(userService.getUsersFavouriteCourts(userId)).thenReturn(courts);

        mockMvc.perform(get("/user/v1/{userId}/favourites", userId))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$").isArray());
    }

    @Test
    @DisplayName("GET /user/v1/{userId}/favourites returns 404 if user does not exist")
    void getUserFavoritesNonExistentReturnsNotFound() throws Exception {
        when(userService.getUsersFavouriteCourts(nonExistentUserId))
            .thenThrow(new NotFoundException("User not found"));

        mockMvc.perform(get("/user/v1/{userId}/favourites", nonExistentUserId))
            .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("GET /user/v1/{userId}/favourites returns 400 for invalid UUID")
    void getUserFavoritesInvalidUUID() throws Exception {
        mockMvc.perform(get("/user/v1/{userId}/favourites", "invalid-uuid"))
            .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /user/v1/{userId}/favourites adds favourite courts successfully")
    void addUserFavoriteSuccessfully() throws Exception {
        List<UUID> courtIds = List.of(courtId);

        mockMvc.perform(post("/user/v1/{userId}/favourites", userId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(courtIds)))
            .andExpect(status().isCreated());
    }

    @Test
    @DisplayName("POST /user/v1/{userId}/favourites returns 404 if user does not exist")
    void addUserFavoriteNonExistentUserReturnsNotFound() throws Exception {
        List<UUID> courtIds = List.of(courtId);
        doThrow(new NotFoundException("User not found"))
            .when(userService).addFavouriteCourts(nonExistentUserId, courtIds);

        mockMvc.perform(post("/user/v1/{userId}/favourites", nonExistentUserId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(courtIds)))
            .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("DELETE /user/v1/{userId}/favourites/{favouriteId} removes favourite court successfully")
    void deleteUserFavoriteSuccessfully() throws Exception {
        mockMvc.perform(delete("/user/v1/{userId}/favourites/{favouriteId}", userId, courtId))
            .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("DELETE /user/v1/{userId}/favourites/{favouriteId} returns 404 if user not found")
    void deleteUserFavoriteNonExistentUserReturnsNotFound() throws Exception {
        doThrow(new NotFoundException("User not found"))
            .when(userService).removeFavouriteCourt(nonExistentUserId, courtId);

        mockMvc.perform(delete("/user/v1/{userId}/favourites/{favouriteId}", nonExistentUserId, courtId))
            .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("DELETE /user/v1/{userId}/locks clears user locks successfully")
    void clearUserLocksSuccessfully() throws Exception {
        mockMvc.perform(delete("/user/v1/{userId}/locks", userId))
            .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("DELETE /user/v1/{userId}/locks returns 404 if user not found")
    void clearUserLocksNonExistentUserReturnsNotFound() throws Exception {
        doThrow(new NotFoundException("User not found"))
            .when(courtLockService).clearUserLocks(nonExistentUserId);

        mockMvc.perform(delete("/user/v1/{userId}/locks", nonExistentUserId))
            .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("POST /user/v1 creates user successfully")
    void createUserSuccessfully() throws Exception {
        User user = new User();
        user.setId(userId);
        user.setEmail("email@justice.gov.uk");
        user.setSsoId(UUID.randomUUID());
        when(userService.createOrUpdateLastLoginUser(any(User.class))).thenReturn(user);

        mockMvc.perform(post("/user/v1")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(user)))
            .andExpect(status().isCreated());
    }

    @Test
    @DisplayName("DELETE /user/v1/retention deletes inactive users successfully")
    void deleteInactiveUsersSuccessfully() throws Exception {
        mockMvc.perform(delete("/user/v1/retention"))
            .andExpect(status().isNoContent());
    }
}
