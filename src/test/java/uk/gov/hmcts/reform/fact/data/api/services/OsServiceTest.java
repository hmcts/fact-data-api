package uk.gov.hmcts.reform.fact.data.api.services;

import feign.FeignException;
import feign.Request;
import feign.RequestTemplate;
import feign.Response;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.fact.data.api.entities.LocalAuthorityType;
import uk.gov.hmcts.reform.fact.data.api.errorhandling.exceptions.InvalidPostcodeException;
import uk.gov.hmcts.reform.fact.data.api.errorhandling.exceptions.OsProcessException;
import uk.gov.hmcts.reform.fact.data.api.os.OsData;
import uk.gov.hmcts.reform.fact.data.api.os.OsDpa;
import uk.gov.hmcts.reform.fact.data.api.os.OsResult;
import uk.gov.hmcts.reform.fact.data.api.os.OsLocationData;
import uk.gov.hmcts.reform.fact.data.api.os.OsFeignClient;
import uk.gov.hmcts.reform.fact.data.api.repositories.LocalAuthorityTypeRepository;

import java.lang.reflect.Method;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OsServiceTest {

    private static final String AUTHORITY_NAME = "Authority Name";

    @Mock
    private OsFeignClient osFeignClient;

    @Mock
    private LocalAuthorityTypeRepository localAuthorityTypeRepository;

    @InjectMocks
    private OsService osService;

    @Test
    void shouldReturnLocationDataWhenCustodianCodesMatch() {
        OsData osData = createOsData(List.of(123, 123), 51.501, -0.141);
        when(osFeignClient.getOsPostcodeDataWithMaxResultsLimit("SW1A 1", 1)).thenReturn(osData);
        when(localAuthorityTypeRepository.findParentOrChildNameByCustodianCode(123))
            .thenReturn(Optional.of(LocalAuthorityType.builder().name(AUTHORITY_NAME).build()));

        OsLocationData result = osService.getOsLonLatDistrictByPartial("sw1a 1aa");

        assertThat(result.getAuthorityName()).isEqualTo(AUTHORITY_NAME);
        assertThat(result.getLatitude()).isEqualTo(51.501);
        assertThat(result.getLongitude()).isEqualTo(-0.141);
        assertThat(result.getPostcode()).isEqualTo("SW1A 1");
        verify(localAuthorityTypeRepository, times(1)).findParentOrChildNameByCustodianCode(123);
    }

    @Test
    void shouldReturnLocationDataWhenMultipleCodesResolveToSameAuthority() {
        OsData osData = createOsData(List.of(111, 222), 52.1, -1.2);
        when(osFeignClient.getOsPostcodeDataWithMaxResultsLimit("OX14 1", 1)).thenReturn(osData);
        when(localAuthorityTypeRepository.findParentOrChildNameByCustodianCode(111))
            .thenReturn(Optional.of(LocalAuthorityType.builder().name(AUTHORITY_NAME).build()));
        when(localAuthorityTypeRepository.findParentOrChildNameByCustodianCode(222))
            .thenReturn(Optional.of(LocalAuthorityType.builder().name(AUTHORITY_NAME).build()));

        OsLocationData result = osService.getOsLonLatDistrictByPartial("OX14 1ZZ");

        assertThat(result.getAuthorityName()).isEqualTo(AUTHORITY_NAME);
        assertThat(result.getPostcode()).isEqualTo("OX14 1");
        verify(localAuthorityTypeRepository, times(1)).findParentOrChildNameByCustodianCode(111);
        verify(localAuthorityTypeRepository, times(1)).findParentOrChildNameByCustodianCode(222);
    }

    @Test
    void shouldThrowWhenMultipleCodesResolveToDifferentAuthorities() {
        OsData osData = createOsData(List.of(111, 222), 53.0, -2.0);
        when(osFeignClient.getOsPostcodeDataWithMaxResultsLimit("M1 1", 1)).thenReturn(osData);
        when(localAuthorityTypeRepository.findParentOrChildNameByCustodianCode(111))
            .thenReturn(Optional.of(LocalAuthorityType.builder().name("Authority One").build()));
        when(localAuthorityTypeRepository.findParentOrChildNameByCustodianCode(222))
            .thenReturn(Optional.of(LocalAuthorityType.builder().name("Authority Two").build()));

        assertThatThrownBy(() -> osService.getOsLonLatDistrictByPartial("M1 1AA"))
            .isInstanceOf(OsProcessException.class)
            .hasMessageContaining("resolve to different local authorities");
    }

    @Test
    void shouldThrowWhenNoCustodianCodesReturned() {
        OsData osData = createOsData(Collections.singletonList(null), 51.5, -0.1);
        when(osFeignClient.getOsPostcodeDataWithMaxResultsLimit("AB1 2", 1)).thenReturn(osData);

        assertThatThrownBy(() -> osService.getOsLonLatDistrictByPartial("AB1 2CD"))
            .isInstanceOf(OsProcessException.class)
            .hasMessageContaining("No LOCAL_CUSTODIAN_CODE values returned from OS");
    }

    @Test
    void shouldThrowWhenAuthorityMissingForCustodianCode() {
        OsData osData = createOsData(List.of(7655), 54.0, -1.0);
        when(osFeignClient.getOsPostcodeDataWithMaxResultsLimit("B1", 1)).thenReturn(osData);
        when(localAuthorityTypeRepository.findParentOrChildNameByCustodianCode(7655))
            .thenReturn(Optional.empty());

        assertThatThrownBy(() -> osService.getOsLonLatDistrictByPartial("B1"))
            .isInstanceOf(OsProcessException.class)
            .hasMessageContaining("No authority found for custodian code 7655");
    }

    @Test
    void shouldThrowInvalidPostcodeForMalformedLocationLookup() {
        assertThatThrownBy(() -> osService.getOsLonLatDistrictByPartial("INVALID"))
            .isInstanceOf(InvalidPostcodeException.class)
            .hasMessageContaining("Invalid postcode format");
    }

    @Test
    void shouldReturnOsDataForFullPostcode() {
        OsData osData = createOsData(List.of(9876), 50.0, -0.1);
        when(osFeignClient.getOsPostcodeData("SW1A 1AA")).thenReturn(osData);

        OsData result = osService.getOsAddressByFullPostcode("SW1A 1AA");

        assertThat(result).isEqualTo(osData);
    }

    @Test
    void shouldThrowInvalidPostcodeWhenOsReturnsNoResults() {
        OsData osData = OsData.builder().results(List.of()).build();
        when(osFeignClient.getOsPostcodeData("SW1A 1AA")).thenReturn(osData);

        assertThatThrownBy(() -> osService.getOsAddressByFullPostcode("SW1A 1AA"))
            .isInstanceOf(InvalidPostcodeException.class)
            .hasMessageContaining("No address results returned from OS");
    }

    @Test
    void shouldThrowInvalidPostcodeWhenOsReturnsNullResults() {
        OsData osData = OsData.builder().results(null).build();
        when(osFeignClient.getOsPostcodeData("SW1A 1AA")).thenReturn(osData);

        assertThatThrownBy(() -> osService.getOsAddressByFullPostcode("SW1A 1AA"))
            .isInstanceOf(InvalidPostcodeException.class)
            .hasMessageContaining("No address results returned from OS");
    }

    @Test
    void shouldThrowInvalidPostcodeWhenOsRejectsPostcode() {
        when(osFeignClient.getOsPostcodeData("SW1A 1AA"))
            .thenThrow(createFeignException(400));

        assertThatThrownBy(() -> osService.getOsAddressByFullPostcode("SW1A 1AA"))
            .isInstanceOf(InvalidPostcodeException.class)
            .hasMessageContaining("OS rejected postcode SW1A 1AA with status 400");
    }

    @Test
    void shouldThrowOsProcessExceptionWhenOsCallFails() {
        when(osFeignClient.getOsPostcodeData("SW1A 1AA"))
            .thenThrow(createFeignException(503));

        assertThatThrownBy(() -> osService.getOsAddressByFullPostcode("SW1A 1AA"))
            .isInstanceOf(OsProcessException.class)
            .hasMessageContaining("Error calling Ordnance Survey");
    }

    @Test
    void shouldHandleFeignExceptionWithStatusBelowFourHundred() {
        when(osFeignClient.getOsPostcodeData("SW1A 1AA"))
            .thenThrow(createFeignException(301));

        assertThatThrownBy(() -> osService.getOsAddressByFullPostcode("SW1A 1AA"))
            .isInstanceOf(OsProcessException.class)
            .hasMessageContaining("Error calling Ordnance Survey");
    }

    @Test
    void shouldHandleNullOrEmptyCustodianCodes() {
        assertThat(invokeAreCustodianCodesTheSame(null)).isFalse();
        assertThat(invokeAreCustodianCodesTheSame(Collections.emptyList())).isFalse();
    }

    @Test
    void shouldValidatePostcodesCorrectly() {
        assertTrue(osService.isValidOsPostcode("SW1A 1AA"));
        assertThat(osService.isValidOsPostcode("NOT-A-POSTCODE")).isFalse();
    }

    private OsData createOsData(List<Integer> custodianCodes, double lat, double lng) {
        List<OsResult> results = custodianCodes.stream()
            .map(code -> OsResult.builder()
                .dpa(OsDpa.builder()
                    .localCustodianCode(code)
                    .lat(lat)
                    .lng(lng)
                    .build())
                .build())
            .toList();

        return OsData.builder()
            .results(results)
            .build();
    }

    private boolean invokeAreCustodianCodesTheSame(List<Integer> codes) {
        try {
            Method method = OsService.class.getDeclaredMethod("areCustodianCodesTheSame", List.class);
            method.setAccessible(true);
            return (boolean) method.invoke(osService, codes);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private FeignException createFeignException(int status) {
        Request request = Request.create(
            Request.HttpMethod.GET,
            "http://localhost",
            Map.of(),
            null,
            new RequestTemplate()
        );

        Response response = Response.builder()
            .status(status)
            .reason("status reason")
            .request(request)
            .build();

        return FeignException.errorStatus("os-client", response);
    }
}
