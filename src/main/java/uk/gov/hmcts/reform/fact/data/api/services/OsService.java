package uk.gov.hmcts.reform.fact.data.api.services;

import feign.FeignException;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.fact.data.api.entities.LocalAuthorityType;
import uk.gov.hmcts.reform.fact.data.api.errorhandling.exceptions.InvalidPostcodeException;
import uk.gov.hmcts.reform.fact.data.api.errorhandling.exceptions.OsProcessException;
import uk.gov.hmcts.reform.fact.data.api.os.OsData;
import uk.gov.hmcts.reform.fact.data.api.os.OsFeignClient;
import uk.gov.hmcts.reform.fact.data.api.os.OsLocationData;
import uk.gov.hmcts.reform.fact.data.api.repositories.LocalAuthorityTypeRepository;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class OsService {

    private final OsFeignClient osFeignClient;
    private final LocalAuthorityTypeRepository localAuthorityTypeRepository;

    public OsService(OsFeignClient osFeignClient,
                     LocalAuthorityTypeRepository localAuthorityTypeRepository) {
        this.osFeignClient = osFeignClient;
        this.localAuthorityTypeRepository = localAuthorityTypeRepository;
    }

    public OsLocationData getOsLocationData(String postcode) {
        OsData osData = getOsAddressData(postcode);
        List<Integer> codes = getCustodianCodes(osData);

        if (codes.isEmpty()) {
            throw new OsProcessException(
                "No LOCAL_CUSTODIAN_CODE values returned from OS for postcode " + postcode
            );
        }

        return OsLocationData.builder()
            .authorityName(areCustodianCodesTheSame(codes)
                               ? getAuthorityForSingleCode(codes.getFirst())
                               : getAuthorityForMultipleCodes(codes))
            .latitude(osData.getResults().getFirst().getDpa().getYCoordinate())
            .longitude(osData.getResults().getFirst().getDpa().getXCoordinate())
            .postcode(postcode)
            .build();
    }

    public void isValidOsPostcode(String postcode) {

    }

    public OsData getOsAddressData(String postcode) {
        try {
            OsData osData = osFeignClient.getOsPostcodeData(postcode.trim());

            if (osData.getResults() == null || osData.getResults().isEmpty()) {
                throw new InvalidPostcodeException(
                    "No address results returned from OS for postcode " + postcode
                );
            }

            return osData;
        } catch (FeignException e) {
            if (e.status() >= 400 && e.status() < 500) {
                throw new InvalidPostcodeException(
                    "OS rejected postcode " + postcode + " with status " + e.status(), e
                );
            }

            throw new OsProcessException(
                "Error calling Ordnance Survey for postcode " + postcode, e
            );
        }
    }

    private List<Integer> getCustodianCodes(OsData osData) {
        return osData.getResults()
            .stream()
            .map(address -> address.getDpa().getLocalCustodianCode())
            .toList();
    }

    private boolean areCustodianCodesTheSame(List<Integer> codes) {
        return codes != null
            && !codes.isEmpty()
            && codes.stream().distinct().count() == 1;
    }

    private String getAuthorityForMultipleCodes(List<Integer> codes) {
        List<String> authorities = codes.stream()
            .map(this::getAuthorityForSingleCode)
            .toList();

        if (!allAuthoritiesMatch(authorities)) {
            throw new OsProcessException(
                "Custodian codes " + codes + " resolve to different local authorities: " + authorities
            );
        }

        return authorities.getFirst();
    }

    private String getAuthorityForSingleCode(Integer code) {
        return localAuthorityTypeRepository
            .findParentOrChildNameByCustodianCode(code)
            .orElseThrow(() -> new OsProcessException(
                "No authority found for custodian code " + code))
            .getName();
    }

    private boolean allAuthoritiesMatch(List<String> authorities) {
        return authorities.stream()
            .distinct()
            .limit(2) // Two different means we already have an edge case
            .count() <= 1;
    }
}
