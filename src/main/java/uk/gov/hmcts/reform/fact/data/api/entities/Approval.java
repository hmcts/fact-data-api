package uk.gov.hmcts.reform.fact.data.api.entities;

import uk.gov.hmcts.reform.fact.data.api.entities.types.SubjectType;

import java.time.ZonedDateTime;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonIgnore;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.TimeZoneStorage;
import org.hibernate.annotations.TimeZoneStorageType;
import org.hibernate.annotations.UpdateTimestamp;

@Data
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@NoArgsConstructor
@Builder
@Entity
@Table(name = "approvals")
public class Approval {

    @Schema(
        description = "The internal ID - assigned by the server during creation",
        accessMode = Schema.AccessMode.READ_ONLY
    )
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @Schema(description = "The ID of the subject being approved", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull
    @Column(name = "subject_id")
    private UUID subjectId;

    @Schema(description = "The subject type being approved", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull
    @Enumerated(EnumType.STRING)
    private SubjectType subjectType;

    @Schema(description = "The ID of the associated User", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull
    @Column(name = "user_id")
    private UUID userId;

    @Schema(description = "The associated User")
    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", referencedColumnName = "id", insertable = false, updatable = false)
    private User user;

    @Schema(description = "The last update date/time", accessMode = Schema.AccessMode.READ_ONLY)
    @TimeZoneStorage(TimeZoneStorageType.NORMALIZE_UTC)
    @UpdateTimestamp
    @Setter(AccessLevel.NONE)
    private ZonedDateTime lastUpdatedAt;
}
