package uk.gov.hmcts.reform.fact.data.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import uk.gov.hmcts.reform.fact.data.api.entities.CourtDxCode;
import uk.gov.hmcts.reform.fact.data.api.entities.validation.ValidationConstants;

@Data
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@NoArgsConstructor
@Builder
public class CourtDxCodeDto {

    @Schema(description = "DX code value", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank
    @Size(max = 200, message = "DX code must be {max} characters or fewer")
    @Pattern(regexp = ValidationConstants.GENERIC_DESCRIPTION_REGEX,
        message = ValidationConstants.GENERIC_DESCRIPTION_REGEX_MESSAGE)
    private String dxCode;

    @Schema(description = "Explanation of the DX code")
    @Size(max = 250, message = "DX explanation must be {max} characters or fewer")
    @Pattern(regexp = ValidationConstants.GENERIC_DESCRIPTION_REGEX,
        message = ValidationConstants.GENERIC_DESCRIPTION_REGEX_MESSAGE)
    private String explanation;

    public static CourtDxCodeDto fromEntity(CourtDxCode entity) {
        return CourtDxCodeDto.builder()
            .dxCode(entity.getDxCode())
            .explanation(entity.getExplanation())
            .build();
    }
}
