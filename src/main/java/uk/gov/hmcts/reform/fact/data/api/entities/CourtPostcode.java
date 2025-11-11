package uk.gov.hmcts.reform.fact.data.api.entities;

import uk.gov.hmcts.reform.fact.data.api.entities.validation.ValidationConstants;

import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonIgnore;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@NoArgsConstructor
@Entity
@Table(name = "court_postcodes")
public class CourtPostcode {

    @Schema(
        description = "The internal ID - assigned by the server during creation",
        accessMode = Schema.AccessMode.READ_ONLY
    )
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @Schema(description = "The ID of the associated Court", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull
    @Column(name = "court_id")
    private UUID courtId;

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "court_id", insertable = false, updatable = false)
    private Court court;

    @Schema(description = "The postcode", minLength = 1)
    @NotBlank(message = "The postcode must be specified")
    @Size(max = ValidationConstants.POSTCODE_MAX_LENGTH, message = ValidationConstants.POSTCODE_MAX_LENGTH_MESSAGE)
    @Pattern(regexp = ValidationConstants.POSTCODE_REGEX, message = ValidationConstants.COURT_POSTCODE_REGEX_MESSAGE)
    private String postcode;

    public static CourtPostcodeBuilder builder() {
        return new CourtPostcodeBuilder();
    }

    public static class CourtPostcodeBuilder {
        private UUID id;
        private UUID courtId;
        private Court court;
        private String postcode;

        CourtPostcodeBuilder() {
        }

        public CourtPostcodeBuilder id(UUID id) {
            this.id = id;
            return this;
        }

        public CourtPostcodeBuilder courtId(UUID courtId) {
            this.courtId = courtId;
            return this;
        }

        public CourtPostcodeBuilder court(Court court) {
            this.court = court;
            return this;
        }

        public CourtPostcodeBuilder postcode(String postcode) {
            this.postcode = postcode;
            return this;
        }

        public CourtPostcode build() {
            CourtPostcode courtPostcode = new CourtPostcode();
            courtPostcode.setId(id);
            courtPostcode.setCourtId(courtId);
            courtPostcode.setCourt(court);
            courtPostcode.setPostcode(postcode);
            return courtPostcode;
        }
    }
}
