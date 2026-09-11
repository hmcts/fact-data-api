package uk.gov.hmcts.reform.fact.data.api.entities;

import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonView;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
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

import uk.gov.hmcts.reform.fact.data.api.audit.AuditableCourtEntityListener;
import uk.gov.hmcts.reform.fact.data.api.controllers.CourtController.CourtDetailsView;
import uk.gov.hmcts.reform.fact.data.api.entities.validation.ValidationConstants;

@Data
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@NoArgsConstructor
@Builder
@Entity
@EntityListeners(AuditableCourtEntityListener.class)
@JsonView(CourtDetailsView.class)
@Table(name = "court_codes")
public class CourtCodes implements AuditableCourtEntity {

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
    @Digits(integer = ValidationConstants.COURT_CODE_MAX_DIGITS,
        fraction = ValidationConstants.COURT_CODE_FRACTION_DIGITS,
        message = "The magistrate " + ValidationConstants.COURT_CODE_DIGITS_MESSAGE)
    private Integer magistrateCourtCode;

    @Schema(description = "The Family Court code")
    @Digits(integer = ValidationConstants.COURT_CODE_MAX_DIGITS,
        fraction = ValidationConstants.COURT_CODE_FRACTION_DIGITS,
        message = "The Family " + ValidationConstants.COURT_CODE_DIGITS_MESSAGE)
    private Integer familyCourtCode;

    @Schema(description = "The Tribunal Court code")
    @Digits(integer = ValidationConstants.COURT_CODE_MAX_DIGITS,
        fraction = ValidationConstants.COURT_CODE_FRACTION_DIGITS,
        message = "The Tribunal " + ValidationConstants.COURT_CODE_DIGITS_MESSAGE)
    private Integer tribunalCode;

    @Schema(description = "The County Court code")
    @Digits(integer = ValidationConstants.COURT_CODE_MAX_DIGITS,
        fraction = ValidationConstants.COURT_CODE_FRACTION_DIGITS,
        message = "The County " + ValidationConstants.COURT_CODE_DIGITS_MESSAGE)
    private Integer countyCourtCode;

    @Schema(description = "The Crown Court code")
    @Digits(integer = ValidationConstants.COURT_CODE_MAX_DIGITS,
        fraction = ValidationConstants.COURT_CODE_FRACTION_DIGITS,
        message = "The Crown " + ValidationConstants.COURT_CODE_DIGITS_MESSAGE)
    private Integer crownCourtCode;

    @Schema(description = "The GBS code")
    @Size(max = ValidationConstants.GBS_CODE_MAX_LENGTH, message = ValidationConstants.GBS_CODE_MAX_LENGTH_MESSAGE)
    @Pattern(regexp = ValidationConstants.GBS_CODE_REGEX, message = ValidationConstants.GBS_CODE_REGEX_MESSAGE)
    @Column(length = ValidationConstants.GBS_CODE_MAX_LENGTH)
    private String gbs;

}
