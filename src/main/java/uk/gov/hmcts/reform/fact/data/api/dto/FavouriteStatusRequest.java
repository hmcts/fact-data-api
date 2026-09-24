package uk.gov.hmcts.reform.fact.data.api.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import uk.gov.hmcts.reform.fact.data.api.entities.validation.ValidationConstants;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor(access = AccessLevel.PUBLIC)
public class FavouriteStatusRequest {

    @NotEmpty(message = "subjects must contain at least one item")
    @Size(max = ValidationConstants.FAVOURITE_SUBJECTS_MAX_SIZE,
        message = ValidationConstants.FAVOURITE_SUBJECTS_MAX_SIZE_MESSAGE
    )
    private List<@Valid FavouriteReference> subjects;
}
