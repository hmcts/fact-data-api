package uk.gov.hmcts.reform.fact.data.api.controllers;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import uk.gov.hmcts.reform.fact.data.api.errorhandling.exceptions.CourtResourceNotFoundException;
import uk.gov.hmcts.reform.fact.data.api.errorhandling.exceptions.NotFoundException;
import uk.gov.hmcts.reform.fact.data.api.models.CourtLocalAuthorityDto;
import uk.gov.hmcts.reform.fact.data.api.models.LocalAuthoritySelectionDto;
import uk.gov.hmcts.reform.fact.data.api.services.CourtLocalAuthoritiesService;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CourtLocalAuthoritiesControllerTest {

    private static final UUID COURT_ID = UUID.randomUUID();
    private static final String INVALID_UUID = "invalid-uuid";
    private static final UUID AREA_OF_LAW_ID = UUID.randomUUID();
    private static final UUID LOCAL_AUTHORITY_ID = UUID.randomUUID();

    private static final String RESPONSE_STATUS_MESSAGE = "Response status does not match";
    private static final String RESPONSE_BODY_MESSAGE = "Response body does not match";

    @Mock
    private CourtLocalAuthoritiesService courtLocalAuthoritiesService;

    @InjectMocks
    private CourtLocalAuthoritiesController courtLocalAuthoritiesController;

    @Test
    void shouldReturnLocalAuthorities() {
        CourtLocalAuthorityDto response = CourtLocalAuthorityDto.builder()
            .areaOfLawId(AREA_OF_LAW_ID)
            .localAuthorities(List.of(LocalAuthoritySelectionDto.builder()
                .id(LOCAL_AUTHORITY_ID)
                .selected(true)
                .build()))
            .build();

        when(courtLocalAuthoritiesService.getCourtLocalAuthorities(COURT_ID)).thenReturn(List.of(response));

        ResponseEntity<List<CourtLocalAuthorityDto>> result =
            courtLocalAuthoritiesController.getCourtLocalAuthorities(COURT_ID.toString());

        assertThat(result.getStatusCode()).as(RESPONSE_STATUS_MESSAGE).isEqualTo(HttpStatus.OK);
        assertThat(result.getBody()).as(RESPONSE_BODY_MESSAGE).containsExactly(response);
    }

    @Test
    void shouldUpdateLocalAuthorities() {
        List<CourtLocalAuthorityDto> request = List.of(CourtLocalAuthorityDto.builder()
            .areaOfLawId(AREA_OF_LAW_ID)
            .localAuthorities(List.of())
            .build());

        ResponseEntity<String> result =
            courtLocalAuthoritiesController.updateCourtLocalAuthorities(COURT_ID.toString(), request);

        verify(courtLocalAuthoritiesService).setCourtLocalAuthorities(COURT_ID, request);
        assertThat(result.getStatusCode()).as(RESPONSE_STATUS_MESSAGE).isEqualTo(HttpStatus.OK);
        assertThat(result.getBody()).as(RESPONSE_BODY_MESSAGE)
            .isEqualTo("Update successful for court ID " + COURT_ID);
    }

    @Test
    void shouldThrowNotFoundFromService() {
        when(courtLocalAuthoritiesService.getCourtLocalAuthorities(COURT_ID))
            .thenThrow(new NotFoundException("Court not found"));

        String courtId = COURT_ID.toString();

        assertThrows(NotFoundException.class,
            () -> courtLocalAuthoritiesController.getCourtLocalAuthorities(courtId));
    }

    @Test
    void shouldThrowCourtResourceNotFoundFromService() {
        when(courtLocalAuthoritiesService.getCourtLocalAuthorities(COURT_ID))
            .thenThrow(new CourtResourceNotFoundException("No areas of law"));

        String courtId = COURT_ID.toString();

        assertThrows(CourtResourceNotFoundException.class,
            () -> courtLocalAuthoritiesController.getCourtLocalAuthorities(courtId));
    }

    @Test
    void shouldPropagateExceptionsFromUpdate() {
        List<CourtLocalAuthorityDto> request = List.of(CourtLocalAuthorityDto.builder()
            .areaOfLawId(AREA_OF_LAW_ID)
            .localAuthorities(List.of())
            .build());

        doThrow(new IllegalArgumentException("Missing area of law")).when(courtLocalAuthoritiesService)
            .setCourtLocalAuthorities(COURT_ID, request);

        String courtId = COURT_ID.toString();

        assertThrows(IllegalArgumentException.class, () ->
            courtLocalAuthoritiesController.updateCourtLocalAuthorities(courtId, request));
    }

    @Test
    void shouldRejectInvalidCourtId() {
        assertThrows(IllegalArgumentException.class, () ->
            courtLocalAuthoritiesController.getCourtLocalAuthorities(INVALID_UUID));
    }
}
