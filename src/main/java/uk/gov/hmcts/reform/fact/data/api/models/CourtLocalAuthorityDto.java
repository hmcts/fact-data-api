package uk.gov.hmcts.reform.fact.data.api.models;

import java.util.List;
import java.util.UUID;

import io.swagger.v3.oas.annotations.media.Schema;
import uk.gov.hmcts.reform.fact.data.api.entities.AreaOfLawType;
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
public class CourtLocalAuthorityDto {

    @Schema(description = "The ID of the area of law", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "Area of law id must be provided")
    private UUID areaOfLawId;

    @Schema(description = "The name of the area of law", accessMode = Schema.AccessMode.READ_ONLY)
    private String areaOfLawName;

    @Schema(description = "Local authorities mapped to this area of law")
    @Valid
    @NotNull(message = "Local authorities must be provided")
    private List<LocalAuthoritySelectionDto> localAuthorities;

    /**
     * Build a DTO for an area of law with its local authority selections.
     *
     * @param area        the area of law
     * @param authorities the mapped local authorities
     * @return the DTO representing the area of law mapping
     */
    public static CourtLocalAuthorityDto from(AreaOfLawType area, List<LocalAuthoritySelectionDto> authorities) {
        return CourtLocalAuthorityDto.builder()
            .areaOfLawId(area.getId())
            .areaOfLawName(area.getName())
            .localAuthorities(authorities)
            .build();
    }
}
