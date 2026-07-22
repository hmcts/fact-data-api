package uk.gov.hmcts.reform.fact.data.api.controllers;

import uk.gov.hmcts.reform.fact.data.api.dto.AllLocation;
import uk.gov.hmcts.reform.fact.data.api.dto.FavouriteReference;
import uk.gov.hmcts.reform.fact.data.api.dto.FavouriteStatus;
import uk.gov.hmcts.reform.fact.data.api.dto.FavouriteStatusRequest;
import uk.gov.hmcts.reform.fact.data.api.entities.User;
import uk.gov.hmcts.reform.fact.data.api.entities.types.SubjectType;
import uk.gov.hmcts.reform.fact.data.api.security.SecuredFactRestController;
import uk.gov.hmcts.reform.fact.data.api.services.LockService;
import uk.gov.hmcts.reform.fact.data.api.services.UserService;
import uk.gov.hmcts.reform.fact.data.api.validation.annotations.ValidUUID;

import java.util.List;
import java.util.UUID;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

@SecuredFactRestController(
    name = "User",
    description = "Operations related to Users"
)
@RequestMapping("/user")
@SuppressWarnings("java:S4684")
public class UserController {

    private static final String USER_ID_HEADER = "X-User-Id";

    private final UserService userService;
    private final LockService lockService;

    public UserController(
        UserService userService,
        LockService lockService
    ) {
        this.userService = userService;
        this.lockService = lockService;
    }

    @GetMapping("/v1")
    @Operation(summary = "Get filtered and paginated users")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Successfully retrieved users"),
        @ApiResponse(responseCode = "400", description = "Invalid request parameters supplied")
    })
    @PreAuthorize("@authService.isAdmin()")
    public ResponseEntity<Page<User>> getFilteredAndPaginatedUsers(
        @RequestParam(name = "pageNumber", defaultValue = "0")
        @PositiveOrZero(message = "pageNumber must be greater than or equal to 0") int pageNumber,
        @RequestParam(name = "pageSize", defaultValue = "25")
        @Positive(message = "pageSize must be greater than 0") int pageSize,
        @RequestParam(name = "search", required = false)
        @Size(max = 250, message = "Search must be less than 250 characters")
        String search,
        @RequestParam(name = "sortBy", required = false) String sortBy,
        @RequestParam(name = "sortOrder", required = false) String sortOrder) {
        return ResponseEntity.ok(userService.getFilteredAndPaginatedUsers(
            pageNumber,
            pageSize,
            search,
            sortBy,
            sortOrder
        ));
    }

    @GetMapping("/v1/favourites")
    @Operation(summary = "Get the current user's paginated favourite locations")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Successfully retrieved favourite locations"),
        @ApiResponse(responseCode = "400", description = "Invalid request"),
        @ApiResponse(responseCode = "404", description = "Current user not found")
    })
    public ResponseEntity<Page<AllLocation>> getFavourites(
        @Parameter(hidden = true) @RequestHeader(USER_ID_HEADER) UUID userId,
        @RequestParam(name = "pageNumber", defaultValue = "0")
        @PositiveOrZero(message = "pageNumber must be greater than or equal to 0") int pageNumber,
        @RequestParam(name = "pageSize", defaultValue = "25")
        @Max(value = 1000, message = "pageSize must be no greater than 1000")
        @Positive(message = "pageSize must be greater than 0") int pageSize
    ) {
        return ResponseEntity.ok(userService.getFavourites(userId, pageNumber, pageSize));
    }

    @PostMapping("/v1/favourites")
    @Operation(summary = "Add a favourite location for the current user")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "201", description = "Favourite added or already present"),
        @ApiResponse(responseCode = "400", description = "Invalid request"),
        @ApiResponse(responseCode = "404", description = "Current user or subject not found")
    })
    public ResponseEntity<Void> addFavourite(
        @Parameter(hidden = true) @RequestHeader(USER_ID_HEADER) UUID userId,
        @Valid @RequestBody FavouriteReference favourite
    ) {
        userService.addFavourite(userId, favourite);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @PostMapping("/v1/favourites/status")
    @Operation(summary = "Get favourite status for the current user's supplied locations")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Successfully retrieved favourite statuses"),
        @ApiResponse(responseCode = "400", description = "Invalid request"),
        @ApiResponse(responseCode = "404", description = "Current user not found")
    })
    public ResponseEntity<List<FavouriteStatus>> getStatuses(
        @Parameter(hidden = true) @RequestHeader(USER_ID_HEADER) UUID userId,
        @Valid @RequestBody FavouriteStatusRequest request
    ) {
        return ResponseEntity.ok(userService.getFavouriteStatuses(userId, request.getSubjects()));
    }

    @DeleteMapping("/v1/favourites/{subjectType}/{subjectId}")
    @Operation(summary = "Remove a favourite location for the current user")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "204", description = "Favourite removed or already absent"),
        @ApiResponse(responseCode = "400", description = "Invalid request"),
        @ApiResponse(responseCode = "404", description = "Current user or subject not found")
    })
    public ResponseEntity<Void> removeFavourite(
        @Parameter(hidden = true) @RequestHeader(USER_ID_HEADER) UUID userId,
        @PathVariable SubjectType subjectType,
        @ValidUUID @PathVariable String subjectId
    ) {
        userService.removeFavourite(userId, UUID.fromString(subjectId), subjectType);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/v1/{userId}/locks")
    @Operation(summary = "Clear all locks for a user during logout")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "204", description = "Successfully cleared user locks"),
        @ApiResponse(responseCode = "400", description = "Invalid user ID supplied"),
        @ApiResponse(responseCode = "404", description = "User not found")
    })
    @PreAuthorize("@authService.isAdmin()")
    public ResponseEntity<Void> clearUserLocks(
        @Parameter(description = "UUID of the user", required = true) @ValidUUID @PathVariable String userId) {
        lockService.clearUserLocks(UUID.fromString(userId));
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/v1")
    @Operation(summary = "Create or update user record")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "201", description = "Successfully created/updated user"),
        @ApiResponse(responseCode = "400", description = "Invalid request")
    })
    @PreAuthorize("@authService.isAdmin()")
    public ResponseEntity<User> createOrUpdateLastLoginUser(@Valid @RequestBody User user) {
        return ResponseEntity.status(HttpStatus.CREATED).body(userService.createOrUpdateLastLoginUser(user));
    }

    @DeleteMapping("/v1/retention")
    @Operation(summary = "Delete inactive users")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "204", description = "Successfully processed inactive users")
    })
    @PreAuthorize("@authService.isAdmin()")
    public ResponseEntity<Void> deleteInactiveUsers() {
        userService.deleteInactiveUsers();
        return ResponseEntity.noContent().build();
    }
}
