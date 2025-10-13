package uk.gov.hmcts.reform.fact.data.api.entities;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Entity
@Table(name = "court_dxcodes")
public class CourtDxCode extends IdBasedEntityWithCourt {

    @Schema(description = "The DX Code")
    private Integer dxCode;

    @Schema(description = "The explanation")
    private String explanation;

}
