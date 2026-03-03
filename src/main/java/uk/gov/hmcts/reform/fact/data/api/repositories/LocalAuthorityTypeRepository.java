package uk.gov.hmcts.reform.fact.data.api.repositories;

import org.springframework.data.jpa.repository.Query;
import uk.gov.hmcts.reform.fact.data.api.entities.LocalAuthorityType;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import uk.gov.hmcts.reform.fact.data.api.entities.LocalAuthorityType;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface LocalAuthorityTypeRepository extends JpaRepository<LocalAuthorityType, UUID> {
    
    Optional<LocalAuthorityType> findByName(String name);

    /**
     * Finds the parent or child authority by custodian code.
     *
     * @param code the custodian code
     * @return the matching authority, if found
     */
    @Query(
        value = """
            SELECT *
            FROM local_authority_types
            WHERE custodian_code = :code
               OR :code = ANY(child_custodian_codes)
            ORDER BY
                CASE
                    WHEN :code = ANY(child_custodian_codes) THEN 0
                    ELSE 1
                END
            LIMIT 1
            """,
        nativeQuery = true
    )
    Optional<LocalAuthorityType> findParentOrChildNameByCustodianCode(int code);

    /**
     * Finds an authority by name, case-insensitive.
     *
     * @param name the authority name
     * @return the matching authority, if found
     */
    Optional<LocalAuthorityType> findIdByNameIgnoreCase(String name);
}
