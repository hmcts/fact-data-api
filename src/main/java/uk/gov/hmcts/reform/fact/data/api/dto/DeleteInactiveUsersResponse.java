package uk.gov.hmcts.reform.fact.data.api.dto;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor(access = AccessLevel.PUBLIC)
public class DeleteInactiveUsersResponse {

    private int deletedUsers;
    private String message;
}

