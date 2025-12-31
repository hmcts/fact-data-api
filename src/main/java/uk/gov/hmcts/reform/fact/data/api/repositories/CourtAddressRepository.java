package uk.gov.hmcts.reform.fact.data.api.repositories;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import uk.gov.hmcts.reform.fact.data.api.entities.CourtAddress;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import uk.gov.hmcts.reform.fact.data.api.dto.CourtWithDistance;

@Repository
public interface CourtAddressRepository extends JpaRepository<CourtAddress, UUID> {

    @Query(value = """
        SELECT
            c.id as courtId,
            c.name as courtName,
            c.slug as courtSlug,
            (point(ca.lon, ca.lat) <@> point(:lon, :lat)) as distance
        FROM court_address ca
        JOIN court c ON c.id = ca.court_id
        WHERE ca.address_type IN ('VISIT_US', 'VISIT_OR_CONTACT_US')
          AND ca.lat IS NOT NULL
          AND ca.lon IS NOT NULL
          AND c.open = true
        ORDER BY distance
        LIMIT :limit
    """, nativeQuery = true)
    List<CourtWithDistance> findNearestCourts(
        @Param("lat") double lat,
        @Param("lon") double lon,
        @Param("limit") int limit
    );

    @Query(value = """
        SELECT *
        FROM (
          SELECT DISTINCT ON (c.id)
            c.id   AS courtId,
            c.name AS courtName,
            c.slug AS courtSlug,
            (
              point(CAST(ca.lon AS float8), CAST(ca.lat AS float8))
              <@>
              point(CAST(:lon AS float8), CAST(:lat AS float8))
            ) AS distance
          FROM court c
          JOIN court_address ca ON ca.court_id = c.id
          JOIN court_areas_of_law coa ON coa.court_id = c.id
          WHERE c.open = true
            AND CAST(:aolId AS uuid) = ANY(coa.areas_of_law)
            AND ca.address_type IN ('VISIT_US', 'VISIT_OR_CONTACT_US')
            AND ca.lat IS NOT NULL
            AND ca.lon IS NOT NULL
          ORDER BY c.id, distance
        ) x
        ORDER BY x.distance
        LIMIT :limit;
    """, nativeQuery = true)
    List<CourtWithDistance> findNearestByAreaOfLaw(
        @Param("lat") double lat,
        @Param("lon") double lon,
        @Param("aolId") UUID aolId,
        @Param("limit") int limit
    );

    @Query(value = """
        WITH aol AS (
            SELECT sa.area_of_law_id AS aol_id
            FROM service_area sa
            WHERE sa.id = CAST(:serviceAreaId AS uuid)
            LIMIT 1
        ),
        base AS (
            SELECT
                c.id   AS courtId,
                c.name AS courtName,
                c.slug AS courtSlug,
                (
                  point(CAST(ca.lon AS float8), CAST(ca.lat AS float8))
                  <@>
                  point(CAST(:lon AS float8), CAST(:lat AS float8))
                ) AS distance,
                UPPER(REPLACE(COALESCE(ca.postcode,''), ' ', '')) AS ca_postcode_nospace
            FROM court c
            JOIN court_address ca
              ON ca.court_id = c.id
            JOIN court_areas_of_law coa
              ON coa.court_id = c.id
            JOIN aol
              ON aol.aol_id = ANY(coa.areas_of_law)
            WHERE c.open = true
              AND ca.address_type IN ('VISIT_US', 'VISIT_OR_CONTACT_US')
              AND ca.lat IS NOT NULL
              AND ca.lon IS NOT NULL
              AND ca.postcode IS NOT NULL
        ),
        tiered AS (
            -- Tier 1: full postcode exact match (no spaces)
            SELECT DISTINCT ON (courtId)
                courtId, courtName, courtSlug, distance, 1 AS tier
            FROM base
            WHERE ca_postcode_nospace = :fullNoSpace
            ORDER BY courtId, distance

            UNION ALL

            -- Tier 2: minus-unit prefix match (e.g. SW1A1)
            SELECT DISTINCT ON (courtId)
                courtId, courtName, courtSlug, distance, 2 AS tier
            FROM base
            WHERE ca_postcode_nospace LIKE (:minusUnitNoSpace || '%')
            ORDER BY courtId, distance

            UNION ALL

            -- Tier 3: outcode prefix match (e.g. SW1A)
            SELECT DISTINCT ON (courtId)
                courtId, courtName, courtSlug, distance, 3 AS tier
            FROM base
            WHERE ca_postcode_nospace LIKE (:outcodeNoSpace || '%')
            ORDER BY courtId, distance

            UNION ALL

            -- Tier 4: area code prefix match (e.g. SW)
            SELECT DISTINCT ON (courtId)
                courtId, courtName, courtSlug, distance, 4 AS tier
            FROM base
            WHERE ca_postcode_nospace LIKE (:areacodeNoSpace || '%')
            ORDER BY courtId, distance
        ),
        best AS (
            SELECT MIN(tier) AS best_tier
            FROM tiered
        )
        SELECT courtId, courtName, courtSlug, distance
        FROM tiered
        WHERE tier = (SELECT best_tier FROM best)
        ORDER BY distance, courtName
        LIMIT :limit
    """, nativeQuery = true)
    List<CourtWithDistance> findCivilByPostcodeLadderBestTier(
        @Param("serviceAreaId") UUID serviceAreaId,
        @Param("lat") double lat,
        @Param("lon") double lon,
        @Param("fullNoSpace") String fullNoSpace,
        @Param("minusUnitNoSpace") String minusUnitNoSpace,
        @Param("outcodeNoSpace") String outcodeNoSpace,
        @Param("areacodeNoSpace") String areacodeNoSpace,
        @Param("limit") int limit
    );

    @Query(value = """
        SELECT DISTINCT ON (c.id)
            c.id as courtId,
            c.name as courtName,
            c.slug as courtSlug,
            (
              point(CAST(ca.lon AS float8), CAST(ca.lat AS float8))
              <@>
              point(CAST(:lon AS float8), CAST(:lat AS float8))
            ) AS distance
        FROM court c
        JOIN court_address ca ON ca.court_id = c.id
        JOIN court_local_authorities cla ON cla.court_id = c.id
        WHERE c.open = true
          AND cla.area_of_law_id = CAST(:aolId AS uuid)
          AND CAST(:localAuthorityId AS uuid) = ANY(cla.local_authority_ids)
          AND ca.address_type IN ('VISIT_US', 'VISIT_OR_CONTACT_US')
          AND ca.lat IS NOT NULL
          AND ca.lon IS NOT NULL
        ORDER BY c.id, distance
        LIMIT :limit
    """, nativeQuery = true)
    List<CourtWithDistance> findFamilyNonRegionalByLocalAuthority(
        @Param("lat") double lat,
        @Param("lon") double lon,
        @Param("aolId") UUID aolId,
        @Param("localAuthorityId") UUID localAuthorityId,
        @Param("limit") int limit
    );

    @Query(value = """
        SELECT DISTINCT ON (c.id)
            c.id as courtId,
            c.name as courtName,
            c.slug as courtSlug,
            (
              point(CAST(ca.lon AS float8), CAST(ca.lat AS float8))
              <@>
              point(CAST(:lon AS float8), CAST(:lat AS float8))
            ) AS distance
        FROM court c
        JOIN court_address ca ON ca.court_id = c.id
        JOIN court_local_authorities cla ON cla.court_id = c.id
        JOIN court_service_areas csa ON csa.court_id = c.id
        WHERE c.open = true
          AND cla.area_of_law_id = CAST(:aolId AS uuid)
          AND CAST(:localAuthorityId AS uuid) = ANY(cla.local_authority_ids)
          AND CAST(:serviceAreaId AS uuid) = ANY(csa.service_area_id)
          AND csa.catchment_type = 'REGIONAL'
          AND ca.address_type IN ('VISIT_US', 'VISIT_OR_CONTACT_US')
          AND ca.lat IS NOT NULL
          AND ca.lon IS NOT NULL
        ORDER BY c.id, distance
        LIMIT 1
    """, nativeQuery = true)
    List<CourtWithDistance> findFamilyRegionalByLocalAuthority(
        @Param("serviceAreaId") UUID serviceAreaId,
        @Param("lat") double lat,
        @Param("lon") double lon,
        @Param("aolId") UUID aolId,
        @Param("localAuthorityId") UUID localAuthorityId
    );

    @Query(value = """
        SELECT DISTINCT ON (c.id)
            c.id as courtId,
            c.name as courtName,
            c.slug as courtSlug,
            (
              point(CAST(ca.lon AS float8), CAST(ca.lat AS float8))
              <@>
              point(CAST(:lon AS float8), CAST(:lat AS float8))
            ) AS distance
        FROM court c
        JOIN court_address ca ON ca.court_id = c.id
        JOIN court_service_areas csa ON csa.court_id = c.id
        JOIN court_areas_of_law coa ON coa.court_id = c.id
        WHERE c.open = true
          AND CAST(:serviceAreaId AS uuid) = ANY(csa.service_area_id)
          AND csa.catchment_type = 'REGIONAL'
          AND CAST(:aolId AS uuid) = ANY(coa.areas_of_law)
          AND ca.address_type IN ('VISIT_US', 'VISIT_OR_CONTACT_US')
          AND ca.lat IS NOT NULL
          AND ca.lon IS NOT NULL
        ORDER BY c.id, distance
        LIMIT 1
    """, nativeQuery = true)
    List<CourtWithDistance> findFamilyRegionalByAol(
        @Param("serviceAreaId") UUID serviceAreaId,
        @Param("lat") double lat,
        @Param("lon") double lon,
        @Param("aolId") UUID aolId
    );
}
