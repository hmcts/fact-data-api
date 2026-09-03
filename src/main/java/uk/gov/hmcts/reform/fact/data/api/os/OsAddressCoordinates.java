package uk.gov.hmcts.reform.fact.data.api.os;

import lombok.Value;

/**
 * Coordinates taken from the OS record selected in the admin address picker.
 */
@Value
public class OsAddressCoordinates {
    double latitude;
    double longitude;
}
