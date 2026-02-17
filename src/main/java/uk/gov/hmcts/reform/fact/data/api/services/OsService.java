package uk.gov.hmcts.reform.fact.data.api.services;

import feign.FeignException;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.fact.data.api.errorhandling.exceptions.InvalidPostcodeException;
import uk.gov.hmcts.reform.fact.data.api.errorhandling.exceptions.OsProcessException;
import uk.gov.hmcts.reform.fact.data.api.os.OsData;
import uk.gov.hmcts.reform.fact.data.api.os.OsFeignClient;
import uk.gov.hmcts.reform.fact.data.api.os.OsLocationData;
import uk.gov.hmcts.reform.fact.data.api.repositories.LocalAuthorityTypeRepository;

import java.util.List;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class OsService {

    private final OsFeignClient osFeignClient;
    private final LocalAuthorityTypeRepository localAuthorityTypeRepository;
    private static final Pattern POSTCODE_PATTERN =
        Pattern.compile(
            "^([A-Z]{1,2}\\d[\\dA-Z]?)(?:\\s+(\\d[A-Z]{0,2}))?$",
            Pattern.CASE_INSENSITIVE
        );

    public OsService(OsFeignClient osFeignClient,
                     LocalAuthorityTypeRepository localAuthorityTypeRepository) {
        this.osFeignClient = osFeignClient;
        this.localAuthorityTypeRepository = localAuthorityTypeRepository;
    }

    /**
     * For the frontend logic. Take in the full postcode and provide a search
     * based on the outward code, plus the first character of the inward.
     * This will then be used further on to cache the result for accurate address lookup.
     *
     * @param postcode the postcode.
     * @return the location data returned from OS plus a mapping to determine the admin
     *     district based on the child and parent custodian codes.
     */
    public OsLocationData getOsLonLatDistrictByPartial(String postcode) {
        return getOsLatLonDistrictLookup(
            toOutwardPlusSingleInwardDigit(
                validateAndFormatPostcode(postcode)));
    }

    /**
     * For the admin portal when we look up full addresses and want the OsData back
     * that contains the multiple lines and so forth.
     *
     * @param postcode the postcode.
     * @return the OsData containing all addresses for the provided postcode.
     */
    public OsData getOsAddressByFullPostcode(String postcode) {
        return getOsAddressData(validateAndFormatPostcode(postcode), false);
    }

    /**
     * Method to determine if a postcode is valid. Will return an error if it does not match
     * the regex provided.
     *
     * @param postcode the postcode.
     * @return a boolean determining if the postcode is valid.
     */
    public boolean isValidOsPostcode(String postcode) {
        try {
            validateAndFormatPostcode(postcode);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * For the frontend specific logic. Take in a postcode, get the outward + the first
     * letter of the inward, then retrieve the lat/lon/district we need based on
     * the pairing between the custodian code and parent local authority.
     *
     * @param postcode the postcode.
     * @return a data object containing lat, lon, postcode and district.
     */
    private OsLocationData getOsLatLonDistrictLookup(String postcode) {
        OsData osData = getOsAddressData(postcode, true);
        List<Integer> codes = getCustodianCodes(osData);

        if (codes.isEmpty()) {
            throw new OsProcessException(
                "No LOCAL_CUSTODIAN_CODE values returned from OS for postcode %s"
                    .formatted(postcode)
            );
        }

        // Full postcode lookup requires lat/lon/district return
        // For court lookup, the first court will likely suffice for a lookup
        return OsLocationData.builder()
            .authorityName(areCustodianCodesTheSame(codes)
                               ? getAuthorityForSingleCode(codes.getFirst())
                               : getAuthorityForMultipleCodes(codes))
            .latitude(osData.getResults().getFirst().getDpa().getLat())
            .longitude(osData.getResults().getFirst().getDpa().getLng())
            .postcode(postcode)
            .build();
    }

    /**
     * Retrieve address data from OS based on the provided postcode.
     *
     * @param postcode the postcode.
     * @param maxResultsRequired if we require a maxresults param.
     * @return the OsData object containing address information.
     */
    private OsData getOsAddressData(String postcode, boolean maxResultsRequired) {
        try {
            OsData osData = maxResultsRequired
                ? osFeignClient.getOsPostcodeDataWithMaxResultsLimit(postcode.trim(), 1)
                : osFeignClient.getOsPostcodeData(postcode.trim());

            if (osData.getResults() == null || osData.getResults().isEmpty()) {
                throw new InvalidPostcodeException(
                    "No address results returned from OS for postcode %s".formatted(postcode)
                );
            }

            return osData;
        } catch (FeignException e) {
            if (e.status() >= 400 && e.status() < 500) {
                throw new InvalidPostcodeException(
                    "OS rejected postcode %s with status %s, %s".formatted(postcode, e.status(), e)
                );
            }

            throw new OsProcessException(
                "Error calling Ordnance Survey for postcode %s, %s".formatted(postcode, e)
            );
        }
    }

    /**
     * Determine the custodian codes based on the OS data returned.
     *
     * @param osData the os data
     * @return a list of custodian codes
     */
    private List<Integer> getCustodianCodes(OsData osData) {
        return osData.getResults()
            .stream()
            .map(address -> address.getDpa().getLocalCustodianCode())
            .filter(Objects::nonNull)
            .toList();
    }

    /**
     * Determine if the custodian codes are all the same or not.
     *
     * @param codes the custodian codes
     * @return a boolean determining custodian codes are identical
     */
    private boolean areCustodianCodesTheSame(List<Integer> codes) {
        return codes != null
            && !codes.isEmpty()
            && codes.stream().distinct().count() == 1;
    }

    /**
     * Where we have multiple codes, determine if there are different admin
     * districts between each. If there is, then we return an error. Note that
     * this will be an unlikely edge case.
     *
     * @param codes the custodian codes
     * @return the admin district shared between different codes
     */
    private String getAuthorityForMultipleCodes(List<Integer> codes) {
        List<String> authorities = codes.stream()
            .map(this::getAuthorityForSingleCode)
            .toList();

        if (!allAuthoritiesMatch(authorities)) {
            throw new OsProcessException(
                "%s resolve to different local authorities: %s".formatted(codes, authorities)
            );
        }

        return authorities.getFirst();
    }

    /**
     * Where we need to look up the authority for a single code, provide
     * a lookup based on our mapping from the GDS dataset.
     *
     * @param code the custodian code.
     * @return the authority.
     */
    private String getAuthorityForSingleCode(Integer code) {
        return localAuthorityTypeRepository
            .findParentOrChildNameByCustodianCode(code)
            .orElseThrow(() -> new OsProcessException(
                // Note that a 7655 code error could be related to a PO Box address
                // or one in other words that is not tied to a physical location
                "No authority found for custodian code %s".formatted(code)))
            .getName();
    }

    /**
     * Determine if all authorities match; used for where we have multiple
     * custodian codes for a list of addresses.
     *
     * @param authorities the authorities.
     * @return if they match or not.
     */
    private boolean allAuthoritiesMatch(List<String> authorities) {
        return authorities.stream()
            .distinct()
            .limit(2) // Two different means we already have an edge case
            .count() <= 1;
    }

    /**
     * We need to get the partial for a postcode. I.e. OX14 4 or SL6 8.
     * This is for accurate postcode lookup for when we cache requests
     *
     * @param postcode the postcode
     * @return the formatted postcode; outward plus first number of inward
     */
    private String validateAndFormatPostcode(String postcode) {
        Matcher m = POSTCODE_PATTERN.matcher(postcode.toUpperCase());
        if (!m.matches()) {
            throw new InvalidPostcodeException("Invalid postcode format: %s".formatted(postcode));
        }

        return m.group(2) == null
            ? m.group(1)
            : m.group(1) + " " + m.group(2);
    }

    /**
     * For the frontend lookup primarily so if we search, we can do a partial
     * lookup on OS based on the outward plus the first number of the inward.
     *
     * @param postcode the postcode formatted
     * @return the outward plus the first number of the inward
     */
    private String toOutwardPlusSingleInwardDigit(String postcode) {
        int spaceIndex = postcode.indexOf(' ');

        // If there is no inward, return outward only
        if (spaceIndex == -1) {
            return postcode;
        }

        return postcode.substring(0, spaceIndex)
            + " " + postcode.charAt(spaceIndex + 1);
    }
}
