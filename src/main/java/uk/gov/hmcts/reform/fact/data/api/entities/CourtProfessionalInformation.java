package uk.gov.hmcts.reform.fact.data.api.entities;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Entity
@Table(name = "court_professional_information")
public class CourtProfessionalInformation extends BaseCourtEntity {

    @Schema(description = "Interview room availability status")
    @NotNull
    private Boolean interviewRooms;

    @Schema(description = "Number of available interview rooms")
    @Min(1)
    private Integer interviewRoomCount;

    @Column(name = "interview_phone_number", length = Integer.MAX_VALUE)
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
