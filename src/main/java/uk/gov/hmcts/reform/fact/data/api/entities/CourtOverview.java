package uk.gov.hmcts.reform.fact.data.api.entities;

import java.util.List;

import javax.annotation.concurrent.Immutable;

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
public class CourtOverview extends AbstractCourtEntity {

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "region_id", insertable = false, updatable = false)
    private Region region;

    @OneToMany(fetch = FetchType.EAGER)
    @JoinColumn(name = "court_id", insertable = false, updatable = false)
    List<CourtAddress> courtAddresses;

    @OneToMany(fetch = FetchType.EAGER)
    @JoinColumn(name = "court_id", insertable = false, updatable = false)
    List<CourtOpeningHours> courtOpeningHours;

    @OneToMany(fetch = FetchType.EAGER)
    @JoinColumn(name = "court_id", insertable = false, updatable = false)
    List<CourtContactDetails> courtContactDetails;

    @OneToMany(fetch = FetchType.EAGER)
    @JoinColumn(name = "court_id", insertable = false, updatable = false)
    List<CourtTranslation> courtTranslations;

    @OneToMany(fetch = FetchType.EAGER)
    @JoinColumn(name = "court_id", insertable = false, updatable = false)
    List<CourtAccessibilityOptions> courtAccessibilityOptions;

    @OneToMany(fetch = FetchType.EAGER)
    @JoinColumn(name = "court_id", insertable = false, updatable = false)
    List<CourtFacilities> courtFacilities;

    @OneToMany(fetch = FetchType.EAGER)
    @JoinColumn(name = "court_id", insertable = false, updatable = false)
    List<CourtProfessionalInformation> courtProfessionalInformation;
}
