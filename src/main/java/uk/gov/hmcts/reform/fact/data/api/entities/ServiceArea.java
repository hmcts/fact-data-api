package uk.gov.hmcts.reform.fact.data.api.entities;

import uk.gov.hmcts.reform.fact.data.api.entities.types.CatchmentMethod;
import uk.gov.hmcts.reform.fact.data.api.entities.types.ServiceAreaType;

import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonIgnore;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Entity
@Table(name = "service_area", schema = "public")
public class ServiceArea extends IdBasedEntityWithName {

    @Schema(description = "The description")
    private String description;

    @Schema(description = "The Welsh language description")
    private String descriptionCy;

    @Schema(description = "The online URL")
    private String onlineUrl;

    @Schema(description = "The short description for the online URL")
    private String onlineText;

    @Schema(description = "The Welsh language short description for the online URL")
    private String onlineTextCy;

    @Schema(description = "The description for the online URL")
    private String text;

    @Schema(description = "The Welsh language description for the online URL")
    private String textCy;

    @Schema(description = "The catchment method")
    // conversion is handled by a custom Converter implementation
    private CatchmentMethod catchmentMethod;

    @Schema(description = "The ID of the associated Area of Law")
    @NotNull
    @Column(name = "area_of_law_id")
    private UUID areaOfLawId;

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "area_of_law_id", insertable = false, updatable = false)
    private AreaOfLawType areaOfLaw;

    @Schema(description = "The service area type")
    // conversion is handled by a custom Converter implementation
    private ServiceAreaType type;

    @Schema(description = "Sort order priority")
    private Integer sortOrder;
}
