package uk.gov.hmcts.reform.fact.data.api.controllers.search;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import uk.gov.hmcts.reform.fact.data.api.os.OsData;
import uk.gov.hmcts.reform.fact.data.api.os.OsDpa;
import uk.gov.hmcts.reform.fact.data.api.os.OsResult;
import uk.gov.hmcts.reform.fact.data.api.services.OsService;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SearchAddressControllerTest {

    @Mock
    private OsService osService;

    @InjectMocks
    private SearchAddressController controller;

    @Test
    void getAddressByPostcodeShouldReturnOk() {
        OsData osData = OsData.builder()
            .results(List.of(OsResult.builder()
                .dpa(OsDpa.builder()
                    .lat(51.501)
                    .lng(-0.141)
                    .build())
                .build()))
            .build();

        when(osService.getOsAddressByFullPostcode("SW1A 1AA")).thenReturn(osData);

        ResponseEntity<OsData> response = controller.getAddressByPostcode("SW1A 1AA");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo(osData);
        verify(osService).getOsAddressByFullPostcode("SW1A 1AA");
    }
}
