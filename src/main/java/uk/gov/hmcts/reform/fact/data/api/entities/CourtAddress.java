package uk.gov.hmcts.reform.fact.data.api.entities;

import jakarta.validation.constraints.NotBlank;
import uk.gov.hmcts.reform.fact.data.api.entities.types.AddressType;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonIgnore;
import io.hypersistence.utils.hibernate.type.array.ListArrayType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Type;
import uk.gov.hmcts.reform.fact.data.api.validation.annotations.ValidPostcode;

@Data
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@NoArgsConstructor
@Builder
@Entity
@Table(name = "court_address")
public class CourtAddress {

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

    @Schema(description = "The first address line")
    @Size(max = 255, message = "Address line should be 255 characters or less")
    @Column(name = "address_line_1")
    private String addressLine1;

    @Schema(description = "The second address line")
    @Size(max = 255, message = "Address line should be 255 characters or less")
    @Column(name = "address_line_2")
    private String addressLine2;

    @Schema(description = "The town/city")
    @Size(max = 100, message = "Town/City name should be 100 characters or less")
    private String townCity;

    @Schema(description = "The county")
    @Size(max = 100, message = "County name should be 100 characters or less")
    private String county;

    @Schema(description = "The postcode")
    @NotNull
    @ValidPostcode
    private String postcode;

    @Schema(description = "The EPIM ID")
    @Size(max = 10, message = "EPIM ID should be 10 characters or less")
    private String epimId;

    @Schema(description = "The latitude coordinate")
    private BigDecimal lat;

    @Schema(description = "The longitude coordinate")
    private BigDecimal lon;

    @Schema(description = "The address type")
    @NotBlank(message = "The postcode must be specified")
    @Enumerated(EnumType.STRING)
    private AddressType addressType;

    @Schema(description = "the list of associated Area of Law IDs")
    @Type(ListArrayType.class)
    @Column(columnDefinition = "uuid[]")
    private List<UUID> areasOfLaw;

    @Schema(description = "the list of associated Court Type IDs")
    @Type(ListArrayType.class)
    @Column(columnDefinition = "uuid[]")
    private List<UUID> courtTypes;

}
