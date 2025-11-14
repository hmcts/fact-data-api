package uk.gov.hmcts.reform.fact.data.api.entities.types;

/**
 * Action type enum for audited Entity actions.
 */
public enum AuditActionType {
    /**
     * Indicates creation of a new entity.
     */
    INSERT,
    /**
     * Indicates modification of an existing entity.
     */
    UPDATE,
    /**
     * Indicates removal of an entity.
     */
    DELETE
}
