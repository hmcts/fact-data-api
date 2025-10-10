package uk.gov.hmcts.reform.fact.data.api.entities;

import java.util.UUID;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
@Entity
@Table(name = "contact_description_types")
public class ContactDescriptionType {

    @Schema(
        description = "The internal ID - assigned by the server during creation",
        accessMode = Schema.AccessMode.READ_ONLY
    )
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @Schema(description = "The name of the Contact Description Type")
    @NotBlank(message = "Contact Description Type name must be specified")
    private String name;

    @Schema(description = "The Welsh name of the Contact Description Type")
    @NotBlank(message = "Contact Description Type Welsh name must be specified")
    private String nameCy;

}
