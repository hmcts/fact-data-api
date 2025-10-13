package uk.gov.hmcts.reform.fact.data.api.entities;

import java.time.ZonedDateTime;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonIgnore;
import io.hypersistence.utils.hibernate.type.json.JsonBinaryType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import lombok.AccessLevel;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.Setter;
import lombok.ToString;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.TimeZoneStorage;
import org.hibernate.annotations.TimeZoneStorageType;
import org.hibernate.annotations.Type;
import org.hibernate.type.SqlTypes;

@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Entity
@Table(name = "audit")
public class Audit extends IdBasedEntityWithCourt {

    @Schema(description = "The ID of the associated User")
    @NotNull
    @Column(name = "user_id")
    private UUID userId;

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", insertable = false, updatable = false)
    private User user;

    @Schema(description = "The action type")
    private String actionType;

    @Schema(description = "The previous data state")
    @Type(JsonBinaryType.class)
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private Object actionDataBefore;

    @Schema(description = "The new data state")
    @Type(JsonBinaryType.class)
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private Object actionDataAfter;

    @Schema(description = "The created date/time of the Audit record", accessMode = Schema.AccessMode.READ_ONLY)
    @TimeZoneStorage(TimeZoneStorageType.NORMALIZE_UTC)
    @CreationTimestamp
    @Setter(AccessLevel.NONE)
    private ZonedDateTime createdAt;

}
