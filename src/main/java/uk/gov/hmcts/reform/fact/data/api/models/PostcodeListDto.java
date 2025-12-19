package uk.gov.hmcts.reform.fact.data.api.models;

import uk.gov.hmcts.reform.fact.data.api.entities.validation.ValidationConstants;

import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PostcodeListDto {
    @Schema(description = "The postcode list")
    @NotEmpty
    private List<
        @NotBlank(message = "The postcode must be specified")
        @Size(max = ValidationConstants.POSTCODE_MAX_LENGTH, message = ValidationConstants.POSTCODE_MAX_LENGTH_MESSAGE)
        @Pattern(regexp = ValidationConstants.COURT_POSTCODE_REGEX,
            message = ValidationConstants.COURT_POSTCODE_REGEX_MESSAGE)
            String
        > postcodes;
}
