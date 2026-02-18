package uk.gov.hmcts.reform.fact.data.api.models;

import uk.gov.hmcts.reform.fact.data.api.entities.AreaOfLawType;

import java.util.UUID;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AreaOfLawSelectionDto {
    @Schema(description = "The ID of the Area of Law Type", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "ID of the Area of Law Type must be provided")
    private UUID id;

    @Schema(description = "The name of the Area of Law Type", accessMode = Schema.AccessMode.READ_ONLY)
    private String name;

    @Schema(description = "The Welsh language name of the Area of Law Type", accessMode = Schema.AccessMode.READ_ONLY)
    private String nameCy;

    @Schema(description = "Whether the Area of Law Type is selected for inclusion",
        requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "Selected flag must be provided")
    @Builder.Default
    private Boolean selected = Boolean.FALSE;

    /**
     * Maps an {@link AreaOfLawType} to an {@link AreaOfLawSelectionDto} with the selected flag set to
     * {@link Boolean#FALSE}.
     *
     * @param areaOfLawType the source {@link AreaOfLawType}
     * @return the mapped {@link AreaOfLawSelectionDto}
     */
    public static AreaOfLawSelectionDto from(AreaOfLawType areaOfLawType) {
        return AreaOfLawSelectionDto.builder()
            .id(areaOfLawType.getId())
            .name(areaOfLawType.getName())
            .nameCy(areaOfLawType.getNameCy())
            .build();
    }

    /**
     * Maps an {@link AreaOfLawType} to an {@link AreaOfLawSelectionDto} with the selected flag set
     * to {@link Boolean#TRUE}.
     *
     * @param areaOfLawType the source {@link AreaOfLawType}
     * @return the mapped {@link AreaOfLawSelectionDto}
     */
    public static AreaOfLawSelectionDto asSelected(AreaOfLawType areaOfLawType) {
        AreaOfLawSelectionDto dto = from(areaOfLawType);
        dto.setSelected(true);
        return dto;
    }

    /**
     * Alias for {@link AreaOfLawSelectionDto#from(AreaOfLawType)}.
     *
     * <p>
     * Added for readability in downstream code.
     *
     * @param areaOfLawType the source {@link AreaOfLawType}
     * @return the mapped {@link AreaOfLawSelectionDto}
     */
    public static AreaOfLawSelectionDto asUnselected(AreaOfLawType areaOfLawType) {
        return from(areaOfLawType);
    }
}
