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
@Table(name = "court_codes")
public class CourtCode extends BaseCourtEntity {

    @Schema(description = "The Magistrate Court code")
    private Integer magistrateCourtCode;

    @Schema(description = "The Family Court code")
    private Integer familyCourtCode;

    @Schema(description = "The Tribunal Court code")
    private Integer tribunalCode;

    @Schema(description = "The County Court code")
    private Integer countyCourtCode;

    @Schema(description = "The Crown Court code")
    private Integer crownCourtCode;

    @Schema(description = "The GBS code")
    private Integer gbs;

}
