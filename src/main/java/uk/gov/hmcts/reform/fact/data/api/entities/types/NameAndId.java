package uk.gov.hmcts.reform.fact.data.api.entities.types;

import java.util.UUID;

/**
 * re-usable Name and Id pair object for use in repository results.
 *
 * @param name the entity Name
 * @param id the entity Id
 */
public record NameAndId(String name, UUID id) {}
