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
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import uk.gov.hmcts.reform.fact.data.api.entities.validation.ValidationConstants;

@Data
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@NoArgsConstructor
@Builder
@Entity
@Table(name = "court_dxcodes")
public class CourtDxCode {

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

    @Schema(description = "The DX Code")
    @NotBlank
    @Size(max = 200)
    @Pattern(regexp = ValidationConstants.GENERIC_DESCRIPTION_REGEX,
        message = ValidationConstants.GENERIC_DESCRIPTION_REGEX_MESSAGE)
    @Column(length = 200)
    private String dxCode;

    @Schema(description = "The explanation")
    @Size(max = 250)
    @Pattern(regexp = ValidationConstants.GENERIC_DESCRIPTION_REGEX,
        message = ValidationConstants.GENERIC_DESCRIPTION_REGEX_MESSAGE)
    @Column(length = 250)
    private String explanation;

}
