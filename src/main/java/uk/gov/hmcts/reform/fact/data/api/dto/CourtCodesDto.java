package uk.gov.hmcts.reform.fact.data.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.Optional;
import uk.gov.hmcts.reform.fact.data.api.entities.CourtCodes;
import uk.gov.hmcts.reform.fact.data.api.entities.validation.ValidationConstants;

@Data
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@NoArgsConstructor
@Builder
public class CourtCodesDto {

    @Schema(description = "Magistrates' court code")
    @Digits(integer = ValidationConstants.COURT_CODE_MAX_DIGITS,
        fraction = ValidationConstants.COURT_CODE_FRACTION_DIGITS,
        message = "Magistrates' " + ValidationConstants.COURT_CODE_DIGITS_MESSAGE)
    private Integer magistrateCourtCode;

    @Schema(description = "Family court code")
    @Digits(integer = ValidationConstants.COURT_CODE_MAX_DIGITS,
        fraction = ValidationConstants.COURT_CODE_FRACTION_DIGITS,
        message = "Family " + ValidationConstants.COURT_CODE_DIGITS_MESSAGE)
    private Integer familyCourtCode;

    @Schema(description = "Tribunal court code")
    @Digits(integer = ValidationConstants.COURT_CODE_MAX_DIGITS,
        fraction = ValidationConstants.COURT_CODE_FRACTION_DIGITS,
        message = "Tribunal " + ValidationConstants.COURT_CODE_DIGITS_MESSAGE)
    private Integer tribunalCode;

    @Schema(description = "County court code")
    @Digits(integer = ValidationConstants.COURT_CODE_MAX_DIGITS,
        fraction = ValidationConstants.COURT_CODE_FRACTION_DIGITS,
        message = "County " + ValidationConstants.COURT_CODE_DIGITS_MESSAGE)
    private Integer countyCourtCode;

    @Schema(description = "Crown court code")
    @Digits(integer = ValidationConstants.COURT_CODE_MAX_DIGITS,
        fraction = ValidationConstants.COURT_CODE_FRACTION_DIGITS,
        message = "Crown " + ValidationConstants.COURT_CODE_DIGITS_MESSAGE)
    private Integer crownCourtCode;

    @Schema(description = "GBS code")
    @Size(max = ValidationConstants.GBS_CODE_MAX_LENGTH, message = ValidationConstants.GBS_CODE_MAX_LENGTH_MESSAGE)
    @Pattern(regexp = ValidationConstants.GBS_CODE_REGEX, message = ValidationConstants.GBS_CODE_REGEX_MESSAGE)
    private String gbs;

    public static CourtCodesDto fromEntity(CourtCodes entity) {
        return Optional.ofNullable(entity)
            .map(value -> CourtCodesDto.builder()
                .magistrateCourtCode(value.getMagistrateCourtCode())
                .familyCourtCode(value.getFamilyCourtCode())
                .tribunalCode(value.getTribunalCode())
                .countyCourtCode(value.getCountyCourtCode())
                .crownCourtCode(value.getCrownCourtCode())
                .gbs(value.getGbs())
                .build())
            .orElse(null);
    }
}
