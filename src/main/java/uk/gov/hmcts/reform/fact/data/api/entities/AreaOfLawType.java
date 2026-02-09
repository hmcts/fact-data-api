package uk.gov.hmcts.reform.fact.data.api.entities;

import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonView;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import uk.gov.hmcts.reform.fact.data.api.controllers.CourtController.CourtDetailsView;

@Data
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@NoArgsConstructor
@Builder
@Entity
@Table(name = "area_of_law_types")
@JsonView(CourtDetailsView.class)
public class AreaOfLawType {

    @Schema(
        description = "The internal ID - assigned by the server during creation",
        accessMode = Schema.AccessMode.READ_ONLY
    )
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @Schema(description = "The name")
    @NotBlank(message = "The name must be specified")
    private String name;

    @Schema(description = "The Welsh language name")
    @NotBlank(message = "The Welsh language name must be specified")
    private String nameCy;

    @Column(name = "external_link")
    private String externalLink;

    @Column(name = "external_link_cy")
    private String externalLinkCy;

    @Column(name = "display_name")
    private String displayName;

    @Column(name = "display_name_cy")
    private String displayNameCy;

}
