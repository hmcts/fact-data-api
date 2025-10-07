package uk.gov.hmcts.reform.fact.data.api.entities;

import java.util.UUID;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "region")
public class Region {

    @Schema(
        description = "The internal ID of the Region - assigned during creation",
        accessMode = Schema.AccessMode.READ_ONLY
    )
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(unique = true, nullable = false, insertable = false, updatable = false)
    private UUID id;

    @Schema(description = "The name of the Region")
    @NotBlank(message = "Name must be specified")
    @Size(min = 5, max = 250, message = "Name should be less than 255 chars")
    private String name;

    @Schema(description = "The Region's country")
    @Size(min = 5, max = 250, message = "Country should be less than 255 chars")
    private String country;
}
