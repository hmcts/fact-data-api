package uk.gov.hmcts.reform.fact.data.api.entities;

import com.fasterxml.jackson.annotation.JsonIgnore;
import uk.gov.hmcts.reform.fact.data.api.entities.validation.ValidationConstants;
import uk.gov.hmcts.reform.fact.data.api.entities.types.UserRole;

import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.TimeZoneStorage;
import org.hibernate.annotations.TimeZoneStorageType;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Data
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@NoArgsConstructor
@Builder
@Entity
@Table(name = "users")
public class User {

    @Schema(
        description = "The internal ID - assigned by the server during creation",
        accessMode = Schema.AccessMode.READ_ONLY
    )
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @Schema(description = "The User's email address", requiredMode = Schema.RequiredMode.REQUIRED, minLength = 1)
    @NotBlank(message = "The User's email address must be specified")
    @Size(max = ValidationConstants.EMAIL_MAX_LENGTH, message = ValidationConstants.EMAIL_MAX_LENGTH_MESSAGE)
    @Pattern(regexp = ValidationConstants.JUSTICE_EMAIL_REGEX,
        message = ValidationConstants.JUSTICE_EMAIL_REGEX_MESSAGE)
    private String email;

    @Schema(description = "The User's SSO ID", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull
    private UUID ssoId;

    @Schema(description = "The User's role", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull
    @Enumerated(EnumType.STRING)
    private UserRole role;

    @JsonIgnore
    @Schema(hidden = true)
    @Builder.Default
    @JdbcTypeCode(SqlTypes.ARRAY)
    @Column(name = "favourite_courts", columnDefinition = "uuid[]", nullable = false)
    private List<UUID> favouriteCourts = new ArrayList<>();

    @JsonIgnore
    @Schema(hidden = true)
    @Builder.Default
    @JdbcTypeCode(SqlTypes.ARRAY)
    @Column(name = "favourite_service_centres", columnDefinition = "uuid[]", nullable = false)
    private List<UUID> favouriteServiceCentres = new ArrayList<>();

    @Schema(description = "The User's last login date/time")
    @TimeZoneStorage(TimeZoneStorageType.NORMALIZE_UTC)
    private ZonedDateTime lastLogin;
}
