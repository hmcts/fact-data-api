package uk.gov.hmcts.reform.fact.data.api.entities;

import java.util.List;

import javax.annotation.concurrent.Immutable;

import com.fasterxml.jackson.annotation.JsonView;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import uk.gov.hmcts.reform.fact.data.api.controllers.CourtController.CourtDetailsView;

@Data
@EqualsAndHashCode(callSuper = true)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@NoArgsConstructor
@SuperBuilder
@Entity
@Immutable
@JsonView(CourtDetailsView.class)
@Table(name = "court")
public class CourtDetails extends AbstractCourtEntity {

    @Schema(description = "The Region")
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "region_id", insertable = false, updatable = false)
    private Region region;

    @Schema(description = "The Dx Code for the Court")
    @OneToMany(fetch = FetchType.EAGER)
    @JoinColumn(name = "court_id", unique = true, insertable = false, updatable = false)
    private List<CourtDxCode> courtDxCodes;

    @Schema(description = "The Court Codes for the Court")
    @OneToMany(fetch = FetchType.EAGER)
    @JoinColumn(name = "court_id", insertable = false, updatable = false)
    private List<CourtCodes> courtCodes;

    @Schema(description = "The Fax Numbers for the Court")
    @OneToMany(fetch = FetchType.EAGER)
    @JoinColumn(name = "court_id", insertable = false, updatable = false)
    private List<CourtFax> courtFaxNumbers;

    @Schema(description = "The Addresses for the Court")
    @OneToMany(fetch = FetchType.EAGER)
    @JoinColumn(name = "court_id", insertable = false, updatable = false)
    private List<CourtAddress> courtAddresses;

    @Schema(description = "The Opening Hours for the Court")
    @OneToMany(fetch = FetchType.EAGER)
    @JoinColumn(name = "court_id", insertable = false, updatable = false)
    private List<CourtOpeningHours> courtOpeningHours;

    @Schema(description = "The Counter Service Opening Hours for the Court")
    @OneToMany(fetch = FetchType.EAGER)
    @JoinColumn(name = "court_id", insertable = false, updatable = false)
    private List<CourtCounterServiceOpeningHours> courtCounterServiceOpeningHours;

    @Schema(description = "The Contact Details for the Court")
    @OneToMany(fetch = FetchType.EAGER)
    @JoinColumn(name = "court_id", insertable = false, updatable = false)
    private List<CourtContactDetails> courtContactDetails;

    @Schema(description = "The Translation Services available at the Court")
    @OneToMany(fetch = FetchType.EAGER)
    @JoinColumn(name = "court_id", insertable = false, updatable = false)
    private List<CourtTranslation> courtTranslations;

    @Schema(description = "The Accessibility Options available at the Court")
    @OneToMany(fetch = FetchType.EAGER)
    @JoinColumn(name = "court_id", insertable = false, updatable = false)
    private List<CourtAccessibilityOptions> courtAccessibilityOptions;

    @Schema(description = "The Facilities available at the Court")
    @OneToMany(fetch = FetchType.EAGER)
    @JoinColumn(name = "court_id", insertable = false, updatable = false)
    private List<CourtFacilities> courtFacilities;

    @Schema(description = "The Profession Information for the Court")
    @OneToMany(fetch = FetchType.EAGER)
    @JoinColumn(name = "court_id", insertable = false, updatable = false)
    private List<CourtProfessionalInformation> courtProfessionalInformation;

    @Schema(description = "The Areas of Law for the Court")
    @OneToMany(fetch = FetchType.EAGER)
    @JoinColumn(name = "court_id", insertable = false, updatable = false)
    private List<CourtAreasOfLaw> courtAreasOfLaw;

    @Schema(description = "The photo for the Court")
    @OneToMany(fetch = FetchType.EAGER)
    @JoinColumn(name = "court_id", insertable = false, updatable = false)
    private List<CourtPhoto> courtPhotos;
}
