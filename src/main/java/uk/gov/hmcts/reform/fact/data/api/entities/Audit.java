package uk.gov.hmcts.reform.fact.data.api.entities;

import uk.gov.hmcts.reform.fact.data.api.entities.types.AuditActionType;
import uk.gov.hmcts.reform.fact.data.api.entities.types.Change;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonIgnore;
import io.hypersistence.utils.hibernate.type.json.JsonBinaryType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.TimeZoneStorage;
import org.hibernate.annotations.TimeZoneStorageType;
import org.hibernate.annotations.Type;
import org.hibernate.type.SqlTypes;

@Data
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@NoArgsConstructor
@Builder
@Entity
@Table(name = "audit")
@Schema(accessMode = Schema.AccessMode.READ_ONLY)
public class Audit {

    @Schema(
        description = "The internal ID - assigned by the server during creation"
    )
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @JsonIgnore
    @NotNull
    @Column(name = "court_id")
    private UUID courtId;

    @Schema(description = "The associated Court")
    @ManyToOne
    @JoinColumn(name = "court_id", insertable = false, updatable = false)
    private Court court;

    @JsonIgnore
    @NotNull
    @Column(name = "user_id")
    private UUID userId;

    @Schema(description = "The associated User")
    @ManyToOne
    @JoinColumn(name = "user_id", insertable = false, updatable = false)
    private User user;

    @Schema(description = "The action type")
    @Enumerated(EnumType.STRING)
    private AuditActionType actionType;

    @Schema(description = "The action entity")
    private String actionEntity;

    @Schema(description = "The new data state")
    @Type(JsonBinaryType.class)
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private List<Change> actionDataDiff;

    @Schema(description = "The created date/time of the Audit record")
    @TimeZoneStorage(TimeZoneStorageType.NORMALIZE_UTC)
    @CreationTimestamp
    @Setter(AccessLevel.NONE)
    private ZonedDateTime createdAt;

}
