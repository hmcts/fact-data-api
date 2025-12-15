package uk.gov.hmcts.reform.fact.data.api.models;

import java.util.UUID;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PostcodeMoveDto {

    @Schema(description = "The ID of the source Court", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull
    private UUID sourceCourtId;

    @Schema(description = "The ID of the destination Court", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull
    private UUID destinationCourtId;

    @Schema(description = "The set of Postcode to migrate", requiredMode = Schema.RequiredMode.REQUIRED)
    @Valid
    @NotNull
    PostcodeListDto postcodeList;
}
