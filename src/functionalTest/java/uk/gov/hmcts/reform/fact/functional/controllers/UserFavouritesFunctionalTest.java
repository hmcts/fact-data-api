package uk.gov.hmcts.reform.fact.functional.controllers;

import io.qameta.allure.Feature;
import io.restassured.response.Response;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import uk.gov.hmcts.reform.fact.data.api.dto.FavouriteReference;
import uk.gov.hmcts.reform.fact.data.api.dto.FavouriteStatusRequest;
import uk.gov.hmcts.reform.fact.data.api.entities.types.SubjectType;
import uk.gov.hmcts.reform.fact.functional.helpers.TestDataHelper;
import uk.gov.hmcts.reform.fact.functional.http.HttpClient;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.HttpStatus.CREATED;
import static org.springframework.http.HttpStatus.NO_CONTENT;
import static org.springframework.http.HttpStatus.OK;

@Feature("Current User Favourites")
@DisplayName("Current User Favourites")
public final class UserFavouritesFunctionalTest {

    private static final String COURT_PREFIX = "Functional Favourite Court";
    private static final String SERVICE_CENTRE_PREFIX = "Functional Favourite Service Centre";
    private static final HttpClient http = new HttpClient();

    @Test
    void adminCanAddListCheckAndIdempotentlyRemoveBothLocationTypes() {
        final UUID userId = TestDataHelper.createUser(http, "functional.favourite.admin");
        final UUID courtId = TestDataHelper.createCourt(http, COURT_PREFIX + " Zulu");
        final UUID serviceCentreId = TestDataHelper.createServiceCentre(http, SERVICE_CENTRE_PREFIX + " Alpha");
        final FavouriteReference court = new FavouriteReference(courtId, SubjectType.COURT);
        final FavouriteReference serviceCentre = new FavouriteReference(
            serviceCentreId,
            SubjectType.SERVICE_CENTRE
        );

        assertCreated(add(userId, court));
        assertCreated(add(userId, serviceCentre));
        assertCreated(add(userId, court));

        final Response listResponse = http.doGetAsUser(
            "/user/v1/favourites?pageNumber=0&pageSize=25",
            HttpClient.getAdminBearerToken(),
            userId
        );
        assertThat(listResponse.statusCode()).isEqualTo(OK.value());
        assertThat(listResponse.jsonPath().getLong("page.totalElements")).isEqualTo(2);
        assertThat(listResponse.jsonPath().getList("content.id", String.class))
            .containsExactly(courtId.toString(), serviceCentreId.toString());

        final Response statusResponse = http.doPostAsUser(
            "/user/v1/favourites/status",
            new FavouriteStatusRequest(List.of(serviceCentre, court)),
            HttpClient.getAdminBearerToken(),
            userId
        );
        assertThat(statusResponse.statusCode()).isEqualTo(OK.value());
        assertThat(statusResponse.jsonPath().getList("favourite", Boolean.class)).containsExactly(true, true);

        assertNoContent(remove(userId, serviceCentre));
        assertNoContent(remove(userId, serviceCentre));
        assertThat(http.doGetAsUser(
            "/user/v1/favourites",
            HttpClient.getAdminBearerToken(),
            userId
        ).jsonPath().getList("content.id", String.class)).containsExactly(courtId.toString());
    }

    @Test
    void viewerCanMutateOnlyTheirOwnFavourites() {
        final UUID viewerId = UUID.fromString(http.getFactViewerUserId());
        final UUID courtId = TestDataHelper.createCourt(http, COURT_PREFIX + " Viewer");
        final FavouriteReference court = new FavouriteReference(courtId, SubjectType.COURT);

        assertThat(http.doPostAsUser(
            "/user/v1/favourites",
            court,
            HttpClient.getViewerBearerToken(),
            viewerId
        ).statusCode()).isEqualTo(CREATED.value());
        assertThat(http.doGetAsUser(
            "/user/v1/favourites",
            HttpClient.getViewerBearerToken(),
            viewerId
        ).jsonPath().getList("content.id", String.class)).contains(courtId.toString());
        assertThat(http.doDeleteAsUser(
            "/user/v1/favourites/COURT/" + courtId,
            HttpClient.getViewerBearerToken(),
            viewerId
        ).statusCode()).isEqualTo(NO_CONTENT.value());
    }

    @Test
    void favouritesAreIsolatedBetweenTwoUsers() {
        final UUID firstUser = TestDataHelper.createUser(http, "functional.favourite.first");
        final UUID secondUser = TestDataHelper.createUser(http, "functional.favourite.second");
        final UUID courtId = TestDataHelper.createCourt(http, COURT_PREFIX + " Isolation");
        final FavouriteReference court = new FavouriteReference(courtId, SubjectType.COURT);
        assertCreated(add(firstUser, court));

        assertThat(http.doGetAsUser(
            "/user/v1/favourites",
            HttpClient.getAdminBearerToken(),
            secondUser
        ).jsonPath().getList("content")).isEmpty();

        final Response statuses = http.doPostAsUser(
            "/user/v1/favourites/status",
            new FavouriteStatusRequest(List.of(court)),
            HttpClient.getAdminBearerToken(),
            secondUser
        );
        assertThat(statuses.jsonPath().getBoolean("[0].favourite")).isFalse();
    }

    @Test
    void favouritesAreDatabasePaginatedBeyondTwentyFiveAndSortedByName() {
        final UUID userId = TestDataHelper.createUser(http, "functional.favourite.pagination");

        for (int index = 0; index < 26; index++) {
            final UUID courtId = TestDataHelper.createCourt(http, COURT_PREFIX + " Page " + toLetters(index));
            assertCreated(add(userId, new FavouriteReference(courtId, SubjectType.COURT)));
        }

        final Response firstPage = http.doGetAsUser(
            "/user/v1/favourites?pageNumber=0&pageSize=25",
            HttpClient.getAdminBearerToken(),
            userId
        );
        final Response secondPage = http.doGetAsUser(
            "/user/v1/favourites?pageNumber=1&pageSize=25",
            HttpClient.getAdminBearerToken(),
            userId
        );

        assertThat(firstPage.jsonPath().getLong("page.totalElements")).isEqualTo(26);
        assertThat(firstPage.jsonPath().getList("content")).hasSize(25);
        assertThat(secondPage.jsonPath().getList("content")).hasSize(1);

        final List<String> names = new ArrayList<>(firstPage.jsonPath().getList("content.name", String.class));
        names.addAll(secondPage.jsonPath().getList("content.name", String.class));
        final List<String> sortedNames = names.stream()
            .sorted(Comparator.comparing(name -> name.toLowerCase(Locale.ROOT)))
            .toList();
        assertThat(names).isEqualTo(sortedNames);
    }

    private Response add(UUID userId, FavouriteReference favourite) {
        return http.doPostAsUser(
            "/user/v1/favourites",
            favourite,
            HttpClient.getAdminBearerToken(),
            userId
        );
    }

    private Response remove(UUID userId, FavouriteReference favourite) {
        return http.doDeleteAsUser(
            "/user/v1/favourites/" + favourite.getSubjectType() + "/" + favourite.getSubjectId(),
            HttpClient.getAdminBearerToken(),
            userId
        );
    }

    private void assertCreated(Response response) {
        assertThat(response.statusCode()).isEqualTo(CREATED.value());
    }

    private void assertNoContent(Response response) {
        assertThat(response.statusCode()).isEqualTo(NO_CONTENT.value());
    }

    private String toLetters(int value) {
        return String.valueOf((char) ('a' + value / 26)) + (char) ('a' + value % 26);
    }

    @AfterAll
    static void cleanUpTestData() {
        final Map<String, String> cleanupPaths = Map.of(
            COURT_PREFIX, "/testing-support/courts/name-prefix/",
            SERVICE_CENTRE_PREFIX, "/testing-support/service-centres/name-prefix/"
        );
        cleanupPaths.forEach((prefix, path) ->
            assertThat(http.doDelete(path + prefix).statusCode()).isEqualTo(OK.value())
        );
    }
}
