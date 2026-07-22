package uk.gov.hmcts.reform.fact.data.api.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor(access = AccessLevel.PUBLIC)
public class FavouriteStatusRequest {

    @NotEmpty(message = "subjects must contain at least one item")
    @Size(max = 1000, message = "subjects must contain no more than 1000 items")
    private List<@Valid FavouriteReference> subjects;
}
