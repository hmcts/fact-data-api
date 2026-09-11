package uk.gov.hmcts.reform.fact.data.api.entities;

import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonView;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import uk.gov.hmcts.reform.fact.data.api.controllers.CourtController.CourtDetailsView;
import uk.gov.hmcts.reform.fact.data.api.entities.validation.ValidationConstants;

@Data
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@NoArgsConstructor
@Builder
@Entity
@JsonView(CourtDetailsView.class)
@Table(name = "region")
public class Region {

    @Schema(
        description = "The internal ID - assigned by the server during creation",
        accessMode = Schema.AccessMode.READ_ONLY
    )
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @Schema(description = "The name of the Region")
    @NotBlank(message = "The name of the Region must be specified")
    @Size(min = ValidationConstants.REGION_NAME_MIN_LENGTH,
        max = ValidationConstants.COMMON_TEXT_MAX_LENGTH,
        message = ValidationConstants.REGION_NAME_LENGTH_MESSAGE)
    private String name;

    @Schema(description = "The Region's country")
    @Size(min = ValidationConstants.REGION_NAME_MIN_LENGTH,
        max = ValidationConstants.COMMON_TEXT_MAX_LENGTH,
        message = ValidationConstants.REGION_COUNTRY_LENGTH_MESSAGE)
    private String country;
}
