package uk.gov.hmcts.reform.fact.data.api.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import uk.gov.hmcts.reform.fact.data.api.entities.types.SubjectType;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor(access = AccessLevel.PUBLIC)
public class FavouriteReference {

    @NotNull
    private UUID subjectId;

    @NotNull
    private SubjectType subjectType;
}
