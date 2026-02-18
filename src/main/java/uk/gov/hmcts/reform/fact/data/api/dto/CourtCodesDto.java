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
import uk.gov.hmcts.reform.fact.data.api.entities.CourtCodes;

@Data
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@NoArgsConstructor
@Builder
public class CourtCodesDto {

    @Schema(description = "Magistrates' court code")
    @Digits(integer = 6, fraction = 0, message = "Magistrates' court code must be at most {integer} digits")
    private Integer magistrateCourtCode;

    @Schema(description = "Family court code")
    @Digits(integer = 6, fraction = 0, message = "Family court code must be at most {integer} digits")
    private Integer familyCourtCode;

    @Schema(description = "Tribunal court code")
    @Digits(integer = 6, fraction = 0, message = "Tribunal court code must be at most {integer} digits")
    private Integer tribunalCode;

    @Schema(description = "County court code")
    @Digits(integer = 6, fraction = 0, message = "County court code must be at most {integer} digits")
    private Integer countyCourtCode;

    @Schema(description = "Crown court code")
    @Digits(integer = 6, fraction = 0, message = "Crown court code must be at most {integer} digits")
    private Integer crownCourtCode;

    @Schema(description = "GBS code")
    @Size(max = 10, message = "GBS code must be {max} characters or fewer")
    @Pattern(regexp = "^[A-Za-z0-9 ]*$", message = "GBS code contains invalid characters")
    private String gbs;

    public static CourtCodesDto fromEntity(CourtCodes entity) {
        if (entity == null) {
            return null;
        }
        return CourtCodesDto.builder()
            .magistrateCourtCode(entity.getMagistrateCourtCode())
            .familyCourtCode(entity.getFamilyCourtCode())
            .tribunalCode(entity.getTribunalCode())
            .countyCourtCode(entity.getCountyCourtCode())
            .crownCourtCode(entity.getCrownCourtCode())
            .gbs(entity.getGbs())
            .build();
    }
}
