package uk.gov.hmcts.reform.fact.data.api.models;

import java.util.List;
import java.util.UUID;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import uk.gov.hmcts.reform.fact.data.api.entities.LocalAuthorityType;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LocalAuthoritySelectionDto {

    @Schema(description = "The ID of the Local Authority", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "Local authority id must be provided")
    private UUID id;

    @Schema(description = "The name of the Local Authority", accessMode = Schema.AccessMode.READ_ONLY)
    private String name;

    @Schema(description = "Whether the Local Authority is selected for the area of law")
    @NotNull(message = "Selected flag must be provided")
    private Boolean selected;

    /**
     * Build a DTO for a local authority selection.
     *
     * @param la The local authority.
     * @param selectedIds The list of selected local authority IDs.
     * @return The DTO representing the local authority selection.
     */
    public static LocalAuthoritySelectionDto from(LocalAuthorityType la, List<UUID> selectedIds) {
        return LocalAuthoritySelectionDto.builder()
            .id(la.getId())
            .name(la.getName())
            .selected(selectedIds.contains(la.getId()))
            .build();
    }
}
