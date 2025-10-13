package uk.gov.hmcts.reform.fact.data.api.entities;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Entity
@Table(name = "region")
public class Region extends BaseEntity {

    @Schema(description = "The name of the Region")
    @NotBlank(message = "The name of the Region must be specified")
    @Size(min = 5, max = 250, message = "Name should be less than 255 chars")
    private String name;

    @Schema(description = "The Region's country")
    @Size(min = 5, max = 250, message = "Country should be less than 255 chars")
    private String country;

}
