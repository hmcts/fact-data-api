package uk.gov.hmcts.reform.fact.data.api.migration.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@NoArgsConstructor
@Builder
@Entity
@Table(name = "migration_audit")
public class MigrationAudit {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @Column(name = "migration_name", nullable = false, unique = true)
    private String migrationName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private MigrationStatus status;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}
