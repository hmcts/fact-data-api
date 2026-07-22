package uk.gov.hmcts.reform.fact.data.api.repositories;

import uk.gov.hmcts.reform.fact.data.api.entities.User;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface UserRepository extends JpaRepository<User, UUID> {
    List<User> findAllByLastLoginBefore(ZonedDateTime zonedDateTime);

    Optional<User> findByEmail(String email);

    Optional<User> findBySsoId(UUID ssoId);

    @Query(
        value = """
            SELECT
                favourite_locations.subject_id AS "subjectId",
                favourite_locations.subject_type AS "subjectType"
            FROM (
                SELECT court.id AS subject_id, 'COURT' AS subject_type, court.name
                FROM users
                CROSS JOIN LATERAL unnest(users.favourite_courts) AS favourite(subject_id)
                INNER JOIN court ON court.id = favourite.subject_id
                WHERE users.id = :userId

                UNION ALL

                SELECT service_centre.id AS subject_id, 'SERVICE_CENTRE' AS subject_type, service_centre.name
                FROM users
                CROSS JOIN LATERAL unnest(users.favourite_service_centres) AS favourite(subject_id)
                INNER JOIN service_centre ON service_centre.id = favourite.subject_id
                WHERE users.id = :userId
            ) favourite_locations
            ORDER BY
                LOWER(favourite_locations.name),
                favourite_locations.subject_type,
                favourite_locations.subject_id
            """,
        countQuery = """
            SELECT COUNT(*)
            FROM (
                SELECT court.id
                FROM users
                CROSS JOIN LATERAL unnest(users.favourite_courts) AS favourite(subject_id)
                INNER JOIN court ON court.id = favourite.subject_id
                WHERE users.id = :userId

                UNION ALL

                SELECT service_centre.id
                FROM users
                CROSS JOIN LATERAL unnest(users.favourite_service_centres) AS favourite(subject_id)
                INNER JOIN service_centre ON service_centre.id = favourite.subject_id
                WHERE users.id = :userId
            ) favourite_locations
            """,
        nativeQuery = true
    )
    Page<FavouriteLocationReference> findFavouriteLocationsByUserId(
        @Param("userId") UUID userId,
        Pageable pageable
    );

    @Query(
        value = """
            SELECT favourite.subject_id AS "subjectId", 'COURT' AS "subjectType"
            FROM users
            CROSS JOIN LATERAL unnest(users.favourite_courts) AS favourite(subject_id)
            INNER JOIN court ON court.id = favourite.subject_id
            WHERE users.id = :userId
              AND favourite.subject_id IN (:subjectIds)

            UNION ALL

            SELECT favourite.subject_id AS "subjectId", 'SERVICE_CENTRE' AS "subjectType"
            FROM users
            CROSS JOIN LATERAL unnest(users.favourite_service_centres) AS favourite(subject_id)
            INNER JOIN service_centre ON service_centre.id = favourite.subject_id
            WHERE users.id = :userId
              AND favourite.subject_id IN (:subjectIds)
            """,
        nativeQuery = true
    )
    List<FavouriteLocationReference> findExistingFavouriteReferences(
        @Param("userId") UUID userId,
        @Param("subjectIds") List<UUID> subjectIds
    );

    @Modifying
    @Query(
        value = """
            UPDATE users
            SET favourite_courts = array_append(favourite_courts, :subjectId)
            WHERE id = :userId
              AND NOT (:subjectId = ANY (favourite_courts))
            """,
        nativeQuery = true
    )
    int addFavouriteCourtIfAbsent(@Param("userId") UUID userId, @Param("subjectId") UUID subjectId);

    @Modifying
    @Query(
        value = """
            UPDATE users
            SET favourite_service_centres = array_append(favourite_service_centres, :subjectId)
            WHERE id = :userId
              AND NOT (:subjectId = ANY (favourite_service_centres))
            """,
        nativeQuery = true
    )
    int addFavouriteServiceCentreIfAbsent(@Param("userId") UUID userId, @Param("subjectId") UUID subjectId);

    @Modifying
    @Query(
        value = """
            UPDATE users
            SET favourite_courts = array_remove(favourite_courts, :subjectId)
            WHERE id = :userId
              AND :subjectId = ANY (favourite_courts)
            """,
        nativeQuery = true
    )
    int removeFavouriteCourt(@Param("userId") UUID userId, @Param("subjectId") UUID subjectId);

    @Modifying
    @Query(
        value = """
            UPDATE users
            SET favourite_service_centres = array_remove(favourite_service_centres, :subjectId)
            WHERE id = :userId
              AND :subjectId = ANY (favourite_service_centres)
            """,
        nativeQuery = true
    )
    int removeFavouriteServiceCentre(@Param("userId") UUID userId, @Param("subjectId") UUID subjectId);

    @Modifying
    @Query(
        value = """
            UPDATE users
            SET favourite_courts = array_remove(favourite_courts, :subjectId)
            WHERE favourite_courts @> ARRAY[:subjectId]::UUID[]
            """,
        nativeQuery = true
    )
    int removeCourtFromAllFavourites(@Param("subjectId") UUID subjectId);

    @Modifying
    @Query(
        value = """
            UPDATE users
            SET favourite_service_centres = array_remove(favourite_service_centres, :subjectId)
            WHERE favourite_service_centres @> ARRAY[:subjectId]::UUID[]
            """,
        nativeQuery = true
    )
    int removeServiceCentreFromAllFavourites(@Param("subjectId") UUID subjectId);

    interface FavouriteLocationReference {
        UUID getSubjectId();

        String getSubjectType();
    }
}
