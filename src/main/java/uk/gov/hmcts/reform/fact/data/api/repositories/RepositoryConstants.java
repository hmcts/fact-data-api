package uk.gov.hmcts.reform.fact.data.api.repositories;

import lombok.experimental.UtilityClass;

@UtilityClass
public class RepositoryConstants {

    // The namespace for the lock table used by the fact data api.
    public static final int FACT_DATA_API_LOCK_NAMESPACE = 1701;
    // The lock table mutation value used by the fact data api.
    public static final int LOCK_TABLE_MUTATION = 1;
}
