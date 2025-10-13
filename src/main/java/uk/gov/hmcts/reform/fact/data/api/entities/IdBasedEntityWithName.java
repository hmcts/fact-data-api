package uk.gov.hmcts.reform.fact.data.api.entities;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.MappedSuperclass;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@MappedSuperclass
public class IdBasedEntityWithName extends IdBasedEntity {

    @Schema(description = "The name")
    @NotBlank(message = "The name must be specified")
    private String name;

    @Schema(description = "The Welsh language name")
    @NotBlank(message = "The Welsh language name must be specified")
    private String nameCy;

}
