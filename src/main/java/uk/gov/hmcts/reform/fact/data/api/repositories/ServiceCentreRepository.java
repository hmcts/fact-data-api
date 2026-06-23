package uk.gov.hmcts.reform.fact.data.api.repositories;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import uk.gov.hmcts.reform.fact.data.api.dto.ServiceCentreWithDistance;
import uk.gov.hmcts.reform.fact.data.api.entities.ServiceCentre;
import uk.gov.hmcts.reform.fact.data.api.entities.types.CatchmentType;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ServiceCentreRepository extends JpaRepository<ServiceCentre, UUID> {

    Optional<ServiceCentre> findByName(String name);

    boolean existsBySlug(String slug);

    List<ServiceCentre> findByNameStartingWithIgnoreCase(String namePrefix);

    @Query(
        value = "SELECT * FROM service_centre sc WHERE CAST(:serviceAreaId AS uuid) = ANY(sc.service_area_ids)",
        nativeQuery = true
    )
    List<ServiceCentre> findByServiceAreaId(@Param("serviceAreaId") UUID serviceAreaId);

    @Query(
        value = """
            SELECT EXISTS (
                SELECT
                    sc.id
                FROM
                    service_centre sc
                WHERE
                    CAST(:serviceAreaId AS uuid) = ANY(sc.service_area_ids)
                AND
                    sc.catchment_type IN (:#{#catchmentTypes.![name()]})
                LIMIT 1
            )
            """,
        nativeQuery = true
    )
    boolean existsByServiceAreaIdAndCatchmentTypeIn(
        @Param("serviceAreaId") UUID serviceAreaId,
        @Param("catchmentTypes") List<CatchmentType> catchmentTypes
    );

    @Query(
        value = """
            SELECT *
            FROM (
                SELECT DISTINCT ON (sc.id)
                    sc.id AS serviceCentreId,
                    sc.name AS serviceCentreName,
                    sc.slug AS serviceCentreSlug,
                    (
                        point(CAST(sca.lon AS float8), CAST(sca.lat AS float8))
                        <@>
                        point(CAST(:lon AS float8), CAST(:lat AS float8))
                    ) AS distance
                FROM service_centre sc
                JOIN service_centre_address sca
                    ON sca.service_centre_id = sc.id
                JOIN service_centre_areas_of_law scaol
                    ON scaol.service_centre_id = sc.id
                WHERE sc.open = true
                    AND CAST(:serviceAreaId AS uuid) = ANY(sc.service_area_ids)
                    AND CAST(:areaOfLawId AS uuid) = ANY(scaol.areas_of_law)
                    AND sc.catchment_type IN (:#{#catchmentTypes.![name()]})
                    AND sca.address_type IN ('VISIT_US', 'VISIT_OR_CONTACT_US')
                    AND sca.lat IS NOT NULL
                    AND sca.lon IS NOT NULL
                ORDER BY sc.id, distance
            ) x
            ORDER BY x.distance
            LIMIT :limit
            """,
        nativeQuery = true
    )
    List<ServiceCentreWithDistance> findNearestByServiceAreaAndAreaOfLawAndCatchmentTypeIn(
        @Param("serviceAreaId") UUID serviceAreaId,
        @Param("areaOfLawId") UUID areaOfLawId,
        @Param("catchmentTypes") List<CatchmentType> catchmentTypes,
        @Param("lat") double lat,
        @Param("lon") double lon,
        @Param("limit") int limit
    );
}
