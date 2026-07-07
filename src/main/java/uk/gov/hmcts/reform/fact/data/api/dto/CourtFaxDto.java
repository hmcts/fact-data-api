package uk.gov.hmcts.reform.fact.data.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import uk.gov.hmcts.reform.fact.data.api.entities.CourtFax;
import uk.gov.hmcts.reform.fact.data.api.entities.validation.ValidationConstants;

@Data
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@NoArgsConstructor
@Builder
public class CourtFaxDto {

    @Schema(description = "Contact fax number", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "The contact fax number must be specified")
    @Size(max = ValidationConstants.PHONE_NO_MAX_LENGTH, message = ValidationConstants.PHONE_NO_MAX_LENGTH_MESSAGE)
    @Pattern(regexp = ValidationConstants.PHONE_NO_REGEX, message = ValidationConstants.PHONE_NO_REGEX_MESSAGE)
    private String faxNumber;

    @Schema(description = "Fax description")
    @Size(max = 250, message = "Fax description must be {max} characters or fewer")
    @Pattern(regexp = ValidationConstants.GENERIC_DESCRIPTION_REGEX,
        message = ValidationConstants.GENERIC_DESCRIPTION_REGEX_MESSAGE)
    private String description;

    @Schema(description = "Welsh language fax description")
    @Size(max = 250, message = "Welsh fax description must be {max} characters or fewer")
    @Pattern(regexp = ValidationConstants.GENERIC_DESCRIPTION_REGEX,
        message = ValidationConstants.GENERIC_DESCRIPTION_REGEX_MESSAGE)
    private String descriptionCy;

    @AssertTrue(message = "Fax description and Welsh fax description must be provided together")
    public boolean isDescriptionCyPresentWhenDescriptionProvided() {
        return StringUtils.isBlank(description) == StringUtils.isBlank(descriptionCy);
    }

    public static CourtFaxDto fromEntity(CourtFax entity) {
        return CourtFaxDto.builder()
            .faxNumber(entity.getFaxNumber())
            .description(entity.getDescription())
            .descriptionCy(entity.getDescriptionCy())
            .build();
    }
}
