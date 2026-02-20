package uk.gov.hmcts.reform.fact.data.api.migration.entities;

import java.time.ZonedDateTime;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.TimeZoneStorage;
import org.hibernate.annotations.TimeZoneStorageType;

@Entity
@Table(name = "legacy_court_mapping")
@Getter
@Builder
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@NoArgsConstructor
public class LegacyCourtMapping {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @Column(name = "court_id", nullable = false)
    private UUID courtId;

    @Column(name = "legacy_court_id", nullable = false)
    private Long legacyCourtId;

    @CreationTimestamp
    @TimeZoneStorage(TimeZoneStorageType.NORMALIZE_UTC)
    @Column(name = "created_at", nullable = false, updatable = false)
    private ZonedDateTime createdAt;
}
