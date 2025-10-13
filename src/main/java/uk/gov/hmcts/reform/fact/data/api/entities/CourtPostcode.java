package uk.gov.hmcts.reform.fact.data.api.entities;

import uk.gov.hmcts.reform.fact.data.api.entities.validation.ValidationConstants;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Entity
@Table(name = "court_postcodes")
public class CourtPostcode extends BaseCourtEntity {

    @Schema(description = "The postcode", minLength = 1)
    @NotBlank(message = "The postcode must be specified")
    @Size(max = ValidationConstants.POSTCODE_MAX_LENGTH, message = ValidationConstants.POSTCODE_MAX_LENGTH_MESSAGE)
    @Pattern(regexp = ValidationConstants.POSTCODE_REGEX, message = ValidationConstants.POSTCODE_REGEX_MESSAGE)
    private String postcode;

}
