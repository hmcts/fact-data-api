package uk.gov.hmcts.reform.fact.data.api.entities;

import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonView;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import uk.gov.hmcts.reform.fact.data.api.audit.AuditableCourtEntityListener;
import uk.gov.hmcts.reform.fact.data.api.entities.validation.ValidationConstants;
import uk.gov.hmcts.reform.fact.data.api.controllers.CourtController.CourtDetailsView;

@Data
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@NoArgsConstructor
@Builder
@Entity
@EntityListeners(AuditableCourtEntityListener.class)
@JsonView(CourtDetailsView.class)
@Table(name = "court_professional_information")
public class CourtProfessionalInformation implements AuditableCourtEntity {

    private static final String INTERVIEW_ROOM_COUNT_MESSAGE =
        "Interview room count must be between 1 and 150 when interview rooms are available; otherwise omit or set to 0";

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

    @Schema(description = "Interview room availability status")
    @NotNull
    private Boolean interviewRooms;

    @Schema(description = "Number of available interview rooms")
    @Min(0)
    @Max(150)
    private Integer interviewRoomCount;

    @Column(name = "interview_phone_number", length = Integer.MAX_VALUE)
    @Size(max = ValidationConstants.PHONE_NO_MAX_LENGTH, message = ValidationConstants.PHONE_NO_MAX_LENGTH_MESSAGE)
    @Pattern(regexp = ValidationConstants.PHONE_NO_REGEX, message = ValidationConstants.PHONE_NO_REGEX_MESSAGE)
    private String interviewPhoneNumber;

    @Schema(description = "Video hearing capability status")
    @NotNull
    private Boolean videoHearings;

    @Schema(description = "Common platform status")
    @NotNull
    private Boolean commonPlatform;

    @Schema(description = "Access scheme status")
    @NotNull
    private Boolean accessScheme;
}
