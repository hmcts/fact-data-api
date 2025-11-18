package uk.gov.hmcts.reform.fact.data.api.repositories;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import uk.gov.hmcts.reform.fact.data.api.entities.Court;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CourtRepository extends JpaRepository<Court, UUID> {
    Page<Court> findByRegionIdInAndNameContainingIgnoreCase(
        List<UUID> regionIds,
        String name,
        Pageable pageable
    );

    Page<Court> findByRegionIdInAndOpenTrueAndNameContainingIgnoreCase(
        List<UUID> regionIds,
        String name,
        Pageable pageable
    );

    boolean existsBySlug(String slug);

    List<Court> findByNameStartingWithIgnoreCase(String namePrefix);

    Optional<Court> findByMrdId(String mrdId);
}
