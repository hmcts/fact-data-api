package uk.gov.hmcts.reform.fact.data.api.repositories;

import jakarta.validation.constraints.NotBlank;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import uk.gov.hmcts.reform.fact.data.api.entities.ServiceArea;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ServiceAreaRepository extends JpaRepository<ServiceArea, UUID> {
    Optional<ServiceArea> findByNameIgnoreCase(
        @NotBlank(message = "The name must be specified") String name
    );

    @Query(value = """
        select sa.*
        from service s
        join lateral unnest(s.service_areas) as x(service_area_id) on true
        join service_area sa on sa.id = x.service_area_id
        where lower(s.name) = lower(:serviceName)
        order by sa.sort_order nulls last, sa.name
        """, nativeQuery = true)
    List<ServiceArea> findAllByServiceName(@Param("serviceName") String serviceName);
}
