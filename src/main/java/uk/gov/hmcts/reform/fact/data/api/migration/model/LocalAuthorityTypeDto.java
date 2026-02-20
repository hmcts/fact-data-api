package uk.gov.hmcts.reform.fact.data.api.migration.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@JsonIgnoreProperties(ignoreUnknown = true)
@Data
@NoArgsConstructor
@AllArgsConstructor
public class LocalAuthorityTypeDto {
    private Integer id;
    private String name;
}
