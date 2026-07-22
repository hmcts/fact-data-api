package uk.gov.hmcts.reform.fact.data.api.controllers;

import tools.jackson.databind.ObjectMapper;
import io.qameta.allure.Feature;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import uk.gov.hmcts.reform.fact.data.api.dto.AllLocation;
import uk.gov.hmcts.reform.fact.data.api.dto.FavouriteReference;
import uk.gov.hmcts.reform.fact.data.api.dto.FavouriteStatus;
import uk.gov.hmcts.reform.fact.data.api.dto.FavouriteStatusRequest;
import uk.gov.hmcts.reform.fact.data.api.entities.User;
import uk.gov.hmcts.reform.fact.data.api.entities.types.SubjectType;
import uk.gov.hmcts.reform.fact.data.api.entities.types.UserRole;
import uk.gov.hmcts.reform.fact.data.api.errorhandling.exceptions.NotFoundException;
import uk.gov.hmcts.reform.fact.data.api.security.AuthService;
import uk.gov.hmcts.reform.fact.data.api.services.LockService;
import uk.gov.hmcts.reform.fact.data.api.services.UserService;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.IntStream;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Feature("User Controller")
@DisplayName("User Controller")
@WebMvcTest(UserController.class)
@AutoConfigureMockMvc(addFilters = false)
class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private UserService userService;

    @MockitoBean
    private LockService lockService;

    @MockitoBean
    private AuthService authService;

    private final UUID userId = UUID.fromString("123e4567-e89b-12d3-a456-426614174000");
    private final UUID subjectId = UUID.fromString("223e4567-e89b-12d3-a456-426614174000");
    private final UUID nonExistentUserId = UUID.fromString("11111111-1111-1111-1111-111111111111");

    @Test
    @DisplayName("GET /user/v1 returns filtered and paginated users successfully")
    void getFilteredAndPaginatedUsersReturnsSuccessfully() throws Exception {
        UUID ssoId = UUID.fromString("333e4567-e89b-12d3-a456-426614174000");
        User user = User.builder()
            .email("admin@justice.gov.uk")
            .ssoId(ssoId)
            .lastLogin(ZonedDateTime.parse("2026-07-08T10:15:30Z"))
            .role(UserRole.ADMIN)
            .build();
        when(userService.getFilteredAndPaginatedUsers(0, 25, "admin", "lastLogin", "desc"))
            .thenReturn(new PageImpl<>(List.of(user)));

        mockMvc.perform(get("/user/v1")
                            .param("pageNumber", "0")
                            .param("pageSize", "25")
                            .param("search", "admin")
                            .param("sortBy", "lastLogin")
                            .param("sortOrder", "desc"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content[0].email").value("admin@justice.gov.uk"))
            .andExpect(jsonPath("$.content[0].ssoId").value(ssoId.toString()))
            .andExpect(jsonPath("$.content[0].role").value("Admin"));

        verify(userService).getFilteredAndPaginatedUsers(0, 25, "admin", "lastLogin", "desc");
    }

    @Test
    void getFavouritesReturnsPageMetadataForCurrentUser() throws Exception {
        AllLocation location = AllLocation.builder()
            .id(subjectId)
            .name("Example Court")
            .locationType(SubjectType.COURT.name())
            .build();
        when(userService.getFavourites(userId, 1, 25)).thenReturn(
            new PageImpl<>(List.of(location), PageRequest.of(1, 25), 30)
        );

        mockMvc.perform(get("/user/v1/favourites")
                            .header("X-User-Id", userId)
                            .param("pageNumber", "1")
                            .param("pageSize", "25"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content[0].id").value(subjectId.toString()))
            .andExpect(jsonPath("$.content[0].locationType").value("COURT"))
            .andExpect(jsonPath("$.page.number").value(1))
            .andExpect(jsonPath("$.page.totalElements").value(26));
    }

    @Test
    void addFavouriteReturnsCreated() throws Exception {
        FavouriteReference request = new FavouriteReference(subjectId, SubjectType.SERVICE_CENTRE);
        mockMvc.perform(post("/user/v1/favourites")
                            .header("X-User-Id", userId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isCreated());

        verify(userService).addFavourite(userId, request);
    }

    @Test
    void statusReturnsFavouriteBooleans() throws Exception {
        FavouriteReference subject = new FavouriteReference(subjectId, SubjectType.COURT);
        FavouriteStatusRequest request = new FavouriteStatusRequest(List.of(subject));
        when(userService.getFavouriteStatuses(userId, request.getSubjects())).thenReturn(List.of(
            new FavouriteStatus(subjectId, SubjectType.COURT, true)
        ));

        mockMvc.perform(post("/user/v1/favourites/status")
                            .header("X-User-Id", userId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].subjectId").value(subjectId.toString()))
            .andExpect(jsonPath("$[0].subjectType").value("COURT"))
            .andExpect(jsonPath("$[0].favourite").value(true));
    }

    @Test
    void statusRejectsEmptySubjects() throws Exception {
        mockMvc.perform(post("/user/v1/favourites/status")
                            .header("X-User-Id", userId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"subjects\":[]}"))
            .andExpect(status().isBadRequest());
    }

    @Test
    void statusRejectsMoreThanOneThousandSubjects() throws Exception {
        FavouriteReference subject = new FavouriteReference(subjectId, SubjectType.COURT);
        FavouriteStatusRequest request = new FavouriteStatusRequest(
            IntStream.rangeClosed(0, 1000).mapToObj(ignored -> subject).toList()
        );

        mockMvc.perform(post("/user/v1/favourites/status")
                            .header("X-User-Id", userId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isBadRequest());
    }

    @Test
    void getFavouritesRejectsPageSizesAboveOneThousand() throws Exception {
        mockMvc.perform(get("/user/v1/favourites")
                            .header("X-User-Id", userId)
                            .param("pageSize", "1001"))
            .andExpect(status().isBadRequest());
    }

    @Test
    void addRejectsUnknownSubjectType() throws Exception {
        mockMvc.perform(post("/user/v1/favourites")
                            .header("X-User-Id", userId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"subjectId\":\"" + subjectId + "\",\"subjectType\":\"BUILDING\"}"))
            .andExpect(status().isBadRequest());
    }

    @Test
    void removeFavouriteReturnsNoContent() throws Exception {
        mockMvc.perform(delete("/user/v1/favourites/COURT/{subjectId}", subjectId)
                            .header("X-User-Id", userId))
            .andExpect(status().isNoContent());

        verify(userService).removeFavourite(userId, subjectId, SubjectType.COURT);
    }

    @Test
    void removeRejectsInvalidSubjectId() throws Exception {
        mockMvc.perform(delete("/user/v1/favourites/COURT/not-a-uuid")
                            .header("X-User-Id", userId))
            .andExpect(status().isBadRequest());
    }

    @Test
    void missingCurrentUserHeaderIsBadRequest() throws Exception {
        mockMvc.perform(get("/user/v1/favourites"))
            .andExpect(status().isBadRequest());
    }

    @Test
    void legacyUserIdPathNoLongerExists() throws Exception {
        mockMvc.perform(get("/user/v1/{userId}/favourites", userId))
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
            .when(lockService).clearUserLocks(nonExistentUserId);

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
        user.setRole(UserRole.ADMIN);
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
