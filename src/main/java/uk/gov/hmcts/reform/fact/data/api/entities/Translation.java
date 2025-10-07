package uk.gov.hmcts.reform.fact.data.api.entities;

import java.util.Optional;
import java.util.UUID;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.PostLoad;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "court_translation")
public class Translation {

    @Schema(
        description = "The internal ID of the Court Translation - assigned during creation",
        accessMode = Schema.AccessMode.READ_ONLY
    )
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(unique = true, nullable = false, insertable = false, updatable = false)
    private UUID id;

    @Schema(description = "The associated Court", accessMode = Schema.AccessMode.READ_ONLY)
    // TODO: ideally this would be tagged with @NotNull, but that breaks the JPA validation process
    @OneToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "court_id")
    private Court court;

    @Schema(description = "The email address for Translation services")
    @Size(max = 254, message = "Email address should be no more than 254 characters")
    private String email;

    @Schema(description = "The phone number for Translation services")
    @Size(max = 20, message = "Phone number should be no more than 20 characters")
    private String phoneNumber;

    // DTO+DAO specific handling

    @Schema(description = "The ID of the associated Court", requiredMode = Schema.RequiredMode.REQUIRED)
    // TODO: ideally this would be tagged with @NotNull, but that breaks the JPA validation process
    @Transient // column only used by DTO
    private UUID courtId;

    @PostLoad
    public void postLoad() {
        this.courtId = Optional.ofNullable(this.getCourt()).map(Court::getId).orElse(null);
    }
}
