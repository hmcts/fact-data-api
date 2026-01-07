package uk.gov.hmcts.reform.fact.data.api.entities;

import java.util.List;

import javax.annotation.concurrent.Immutable;

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

@Data
@EqualsAndHashCode(callSuper = true)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@NoArgsConstructor
@SuperBuilder
@Entity
@Immutable
@Table(name = "court")
public class CourtDetails extends AbstractCourtEntity {

    @Schema(description = "The Region")
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "region_id", insertable = false, updatable = false)
    private Region region;

    @Schema(description = "The Addresses for the Court")
    @OneToMany(fetch = FetchType.EAGER)
    @JoinColumn(name = "court_id", insertable = false, updatable = false)
    List<CourtAddress> courtAddresses;

    @Schema(description = "The Opening Hours for the Court")
    @OneToMany(fetch = FetchType.EAGER)
    @JoinColumn(name = "court_id", insertable = false, updatable = false)
    List<CourtOpeningHours> courtOpeningHours;

    @Schema(description = "The Contact Details for the Court")
    @OneToMany(fetch = FetchType.EAGER)
    @JoinColumn(name = "court_id", insertable = false, updatable = false)
    List<CourtContactDetails> courtContactDetails;

    @Schema(description = "The Translation Services available at the Court")
    @OneToMany(fetch = FetchType.EAGER)
    @JoinColumn(name = "court_id", insertable = false, updatable = false)
    List<CourtTranslation> courtTranslations;

    @Schema(description = "The Accessibility Options available at the Court")
    @OneToMany(fetch = FetchType.EAGER)
    @JoinColumn(name = "court_id", insertable = false, updatable = false)
    List<CourtAccessibilityOptions> courtAccessibilityOptions;

    @Schema(description = "The Facilities available at the Court")
    @OneToMany(fetch = FetchType.EAGER)
    @JoinColumn(name = "court_id", insertable = false, updatable = false)
    List<CourtFacilities> courtFacilities;

    @Schema(description = "The Profession Information for the Court")
    @OneToMany(fetch = FetchType.EAGER)
    @JoinColumn(name = "court_id", insertable = false, updatable = false)
    List<CourtProfessionalInformation> courtProfessionalInformation;
}
