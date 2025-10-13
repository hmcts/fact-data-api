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
@Table(name = "area_of_law_types")
public class AreaOfLawType extends IdBasedEntityWithName {
}
