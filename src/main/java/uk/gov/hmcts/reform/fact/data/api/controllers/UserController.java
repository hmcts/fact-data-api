package uk.gov.hmcts.reform.fact.data.api.controllers;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import uk.gov.hmcts.reform.fact.data.api.entities.Court;
import uk.gov.hmcts.reform.fact.data.api.entities.User;
import uk.gov.hmcts.reform.fact.data.api.services.UserService;
import uk.gov.hmcts.reform.fact.data.api.validation.annotations.ValidUUID;

import java.util.List;
import java.util.UUID;

@Tag(name = "User", description = "Operations related to Users")
@RestController
@Validated
@RequestMapping("/user")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/v1/{userId}/favourites")
    @Operation(summary = "Get favourite courts by user ID",
        description = "Fetch favourite courts for a given user."
            + "Returns empty list if user has no favourite courts.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Successfully retrieved favourite courts"),
        @ApiResponse(responseCode = "400", description = "Invalid user ID supplied"),
        @ApiResponse(responseCode = "404", description = "User not found")
    })
    public ResponseEntity<List<Court>> getUserFavorites(
        @Parameter(description = "UUID of the user", required = true) @ValidUUID @PathVariable String userId) {
        return ResponseEntity.ok(userService.getUsersFavouriteCourts(UUID.fromString(userId)));
    }

    @PostMapping("/v1/{userId}/favourites")
    @Operation(summary = "Add a new favourite court for user")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "201", description = "Successfully added favourite courts"),
        @ApiResponse(responseCode = "400", description = "Invalid request"),
        @ApiResponse(responseCode = "404", description = "User or court not found")
    })
    public ResponseEntity<Void> addUserFavorite(
        @Parameter(description = "UUID of the user", required = true) @ValidUUID @PathVariable String userId,
        @Parameter(description = "UUIDs of the courts", required = true) @Valid @RequestBody List<UUID> courtIds) {
        userService.addFavouriteCourt(UUID.fromString(userId), courtIds);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @DeleteMapping("/v1/{userId}/favourites/{favouriteId}")
    @Operation(summary = "Delete a favourite court for user")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "204", description = "Successfully deleted favourite court"),
        @ApiResponse(responseCode = "400", description = "Invalid request"),
        @ApiResponse(responseCode = "404", description = "User or favourite not found")
    })
    public ResponseEntity<Void> deleteUserFavorite(
        @Parameter(description = "UUID of the user", required = true) @ValidUUID @PathVariable String userId,
        @Parameter(description = "UUID of the favourite court", required = true)
        @ValidUUID @PathVariable String favouriteId) {
        userService.removeFavouriteCourt(UUID.fromString(userId), UUID.fromString(favouriteId));
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/v1/{userId}/locks")
    @Operation(summary = "Clear all locks for a user during logout")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "204", description = "Successfully cleared user locks"),
        @ApiResponse(responseCode = "400", description = "Invalid user ID supplied"),
        @ApiResponse(responseCode = "404", description = "User not found")
    })
    public ResponseEntity<Void> clearUserLocks(
        @Parameter(description = "UUID of the user", required = true) @ValidUUID @PathVariable String userId) {
        userService.clearUserLocks(UUID.fromString(userId));
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/v1")
    @Operation(summary = "Create or update user record")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "201", description = "Successfully created/updated user"),
        @ApiResponse(responseCode = "400", description = "Invalid request")
    })
    public ResponseEntity<User> createOrUpdateLastLoginUser(@Valid @RequestBody User user) {
        return ResponseEntity.status(HttpStatus.CREATED).body(userService.createOrUpdateLastLoginUser(user));
    }

    @DeleteMapping("/v1/retention")
    @Operation(summary = "Delete inactive users")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "204", description = "Successfully processed inactive users")
    })
    public ResponseEntity<Void> deleteInactiveUsers() {
        userService.deleteInactiveUsers();
        return ResponseEntity.noContent().build();
    }
}
