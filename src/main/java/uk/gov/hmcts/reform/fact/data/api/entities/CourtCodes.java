package uk.gov.hmcts.reform.fact.data.api.entities;

import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonIgnore;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
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
@Table(name = "court_codes")
public class CourtCodes {

    @Schema(
        description = "The internal ID - assigned by the server during creation",
        accessMode = Schema.AccessMode.READ_ONLY
    )
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @Schema(description = "The ID of the associated Court", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull
    @Column(name = "court_id")
    private UUID courtId;

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "court_id", insertable = false, updatable = false)
    private Court court;

    @Schema(description = "The Magistrate Court code")
    @Digits(integer = 6, fraction = 0, message = "Magistrates' court code must be at most {integer} digits")
    private Integer magistrateCourtCode;

    @Schema(description = "The Family Court code")
    @Digits(integer = 6, fraction = 0, message = "Family court code must be at most {integer} digits")
    private Integer familyCourtCode;

    @Schema(description = "The Tribunal Court code")
    @Digits(integer = 6, fraction = 0, message = "Tribunal court code must be at most {integer} digits")
    private Integer tribunalCode;

    @Schema(description = "The County Court code")
    @Digits(integer = 6, fraction = 0, message = "County court code must be at most {integer} digits")
    private Integer countyCourtCode;

    @Schema(description = "The Crown Court code")
    @Digits(integer = 6, fraction = 0, message = "Crown court code must be at most {integer} digits")
    private Integer crownCourtCode;

    @Schema(description = "The GBS code")
    @Size(max = 10)
    @Pattern(regexp = "^[A-Za-z0-9 ]*$", message = "GBS code contains invalid characters")
    @Column(length = 10)
    private String gbs;

}
