package uk.gov.hmcts.reform.fact.data.api.entities;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Entity
@Table(name = "contact_description_types")
public class ContactDescriptionType extends IdBasedEntityWithName {
}
