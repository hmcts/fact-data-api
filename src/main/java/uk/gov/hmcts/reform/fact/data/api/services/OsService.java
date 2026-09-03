package uk.gov.hmcts.reform.fact.data.api.services;

import feign.FeignException;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import uk.gov.hmcts.reform.fact.data.api.config.CacheConfiguration;
import uk.gov.hmcts.reform.fact.data.api.errorhandling.exceptions.InvalidPostcodeException;
import uk.gov.hmcts.reform.fact.data.api.errorhandling.exceptions.OsProcessException;
import uk.gov.hmcts.reform.fact.data.api.os.OsAddressCoordinates;
import uk.gov.hmcts.reform.fact.data.api.os.OsData;
import uk.gov.hmcts.reform.fact.data.api.os.OsDpa;
import uk.gov.hmcts.reform.fact.data.api.os.OsFeignClient;
import uk.gov.hmcts.reform.fact.data.api.os.OsHeader;
import uk.gov.hmcts.reform.fact.data.api.os.OsLpi;
import uk.gov.hmcts.reform.fact.data.api.os.OsLocationData;
import uk.gov.hmcts.reform.fact.data.api.os.OsResult;
import uk.gov.hmcts.reform.fact.data.api.repositories.LocalAuthorityTypeRepository;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class OsService {

    private final OsFeignClient osFeignClient;
    private final LocalAuthorityTypeRepository localAuthorityTypeRepository;
    private static final Pattern POSTCODE_PATTERN =
        Pattern.compile(
            "^([A-Z]{1,2}\\d[\\dA-Z]?)(?:\\s+(\\d[A-Z]{0,2}))?$",
            Pattern.CASE_INSENSITIVE
        );
    private static final Pattern OS_API_KEY_QUERY_PARAMETER_PATTERN =
        Pattern.compile("([?&]key=)[^&\\]\\s]+", Pattern.CASE_INSENSITIVE);
    private static final String ADMIN_DATASETS = "DPA,LPI";
    private static final String ADMIN_LANGUAGE = "EN";
    private static final int ADMIN_PAGE_SIZE = 100;
    private static final int MAX_ADMIN_PAGES = 100;

    /**
     * For the frontend logic. Take in the full postcode and provide a search
     * based on the outward code, plus the first character of the inward.
     * This will then be used further on to cache the result for accurate address lookup.
     *
     * @param postcode the postcode.
     * @return the location data returned from OS plus a mapping to determine the admin
     *     district based on the child and parent custodian codes.
     */
    @Cacheable(cacheNames = CacheConfiguration.OSDATA_CACHE_NAME, key = "'T-' + #postcode")
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
    @Cacheable(cacheNames = CacheConfiguration.OSDATA_CACHE_NAME, key = "'F-' + #postcode")
    public OsData getOsAddressByFullPostcode(String postcode) {
        return getOsAddressData(validateAndFormatPostcode(postcode), false);
    }

    /**
     * Retrieve all DPA and LPI address options for the admin portal. An empty combined
     * result retains the existing invalid-postcode response used by the address picker.
     *
     * @param postcode full postcode to look up
     * @return all paged DPA and LPI results returned by OS
     */
    @Cacheable(
        cacheNames = CacheConfiguration.OSDATA_CACHE_NAME,
        key = "'A-' + #postcode.trim().replaceAll('\\s+', '').toUpperCase()"
    )
    public OsData getOsAdminAddressByFullPostcode(String postcode) {
        String formattedPostcode = validateAndFormatPostcode(postcode);
        List<OsResult> combinedResults = new ArrayList<>();
        int offset = 0;

        for (int pageNumber = 0; pageNumber < MAX_ADMIN_PAGES; pageNumber++) {
            OsData page = getOsAdminAddressPage(formattedPostcode, offset);
            List<OsResult> pageResults = page == null || page.getResults() == null
                ? Collections.emptyList()
                : page.getResults();
            combinedResults.addAll(pageResults);

            int totalResults = page != null
                && page.getHeader() != null
                && page.getHeader().getTotalresults() != null
                ? page.getHeader().getTotalresults()
                : combinedResults.size();

            if (pageResults.isEmpty() || combinedResults.size() >= totalResults) {
                if (combinedResults.isEmpty()) {
                    throw new InvalidPostcodeException(
                        "No address results returned from OS for postcode %s".formatted(formattedPostcode)
                    );
                }
                return OsData.builder()
                    .header(buildCombinedHeader(totalResults))
                    .results(combinedResults)
                    .build();
            }

            offset += pageResults.size();
        }

        throw new OsProcessException(
            "OS address lookup exceeded %s pages for postcode %s".formatted(MAX_ADMIN_PAGES, formattedPostcode)
        );
    }

    /**
     * Resolve coordinates for an explicitly selected admin address, or retain the
     * existing first-DPA behaviour when the address was entered manually.
     *
     * @param postcode postcode on the address being saved
     * @param dataset selected OS dataset, if an option was selected
     * @param uprn selected OS UPRN, if an option was selected
     * @param lpiKey selected LPI key, when applicable
     * @return usable OS coordinates, or empty when no address option has usable coordinates
     */
    public Optional<OsAddressCoordinates> getOsAdminAddressCoordinates(
        String postcode,
        String dataset,
        String uprn,
        String lpiKey
    ) {
        boolean hasSelectionData = isNotBlank(dataset) || isNotBlank(uprn) || isNotBlank(lpiKey);
        if (hasSelectionData) {
            if (!isNotBlank(dataset) || !isNotBlank(uprn)) {
                throw new IllegalArgumentException("Selected OS address must include both dataset and UPRN");
            }

            OsData osData = getOsAdminAddressByFullPostcode(postcode);
            List<OsResult> results = osData.getResults() == null ? Collections.emptyList() : osData.getResults();
            String normalisedDataset = dataset.trim().toUpperCase(Locale.ROOT);
            return switch (normalisedDataset) {
                case "DPA" -> {
                    OsDpa selectedDpa = results.stream()
                        .map(OsResult::getDpa)
                        .filter(Objects::nonNull)
                        .filter(dpa -> uprn.equals(dpa.getUprn()))
                        .findFirst()
                        .orElseThrow(() -> new IllegalArgumentException(
                            "Selected OS address is no longer available for postcode %s".formatted(postcode)
                        ));
                    yield toCoordinates(selectedDpa.getLat(), selectedDpa.getLng());
                }
                case "LPI" -> {
                    OsLpi selectedLpi = results.stream()
                        .map(OsResult::getLpi)
                        .filter(Objects::nonNull)
                        .filter(lpi -> uprn.equals(lpi.getUprn()))
                        .filter(lpi -> !isNotBlank(lpiKey) || lpiKey.equals(lpi.getLpiKey()))
                        .findFirst()
                        .orElseThrow(() -> new IllegalArgumentException(
                            "Selected OS address is no longer available for postcode %s".formatted(postcode)
                        ));
                    yield toCoordinates(selectedLpi.getLat(), selectedLpi.getLng());
                }
                default -> throw new IllegalArgumentException("Unsupported OS address dataset: " + dataset);
            };
        }

        OsDpa firstDpa = getOsAddressByFullPostcode(postcode).getResults().getFirst().getDpa();
        return firstDpa == null ? Optional.empty() : toCoordinates(firstDpa.getLat(), firstDpa.getLng());
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

            if (osData == null || osData.getResults() == null || osData.getResults().isEmpty()) {
                throw new InvalidPostcodeException(
                    "No address results returned from OS for postcode %s".formatted(postcode)
                );
            }

            return osData;
        } catch (FeignException e) {
            String safeExceptionDetails = sanitiseOsException(e);
            if (e.status() >= 400 && e.status() < 500) {
                throw new InvalidPostcodeException(
                    "OS rejected postcode %s with status %s, %s"
                        .formatted(postcode, e.status(), safeExceptionDetails)
                );
            }

            throw new OsProcessException(
                "Error calling Ordnance Survey for postcode %s, %s"
                    .formatted(postcode, safeExceptionDetails)
            );
        }
    }

    private OsData getOsAdminAddressPage(String postcode, int offset) {
        try {
            return osFeignClient.getOsAdminPostcodeData(
                postcode.trim(),
                ADMIN_DATASETS,
                ADMIN_LANGUAGE,
                ADMIN_PAGE_SIZE,
                offset
            );
        } catch (FeignException e) {
            String safeExceptionDetails = sanitiseOsException(e);
            if (e.status() >= 400 && e.status() < 500) {
                throw new InvalidPostcodeException(
                    "OS rejected postcode %s with status %s, %s"
                        .formatted(postcode, e.status(), safeExceptionDetails)
                );
            }

            throw new OsProcessException(
                "Error calling Ordnance Survey for postcode %s, %s"
                    .formatted(postcode, safeExceptionDetails)
            );
        }
    }

    private OsHeader buildCombinedHeader(int totalResults) {
        return OsHeader.builder()
            .dataset(ADMIN_DATASETS)
            .totalresults(totalResults)
            .maxresults(ADMIN_PAGE_SIZE)
            .offset(0)
            .build();
    }

    private Optional<OsAddressCoordinates> toCoordinates(Double latitude, Double longitude) {
        if (latitude == null
            || longitude == null
            || !Double.isFinite(latitude)
            || !Double.isFinite(longitude)
            || latitude < -90
            || latitude > 90
            || longitude < -180
            || longitude > 180) {
            return Optional.empty();
        }

        return Optional.of(new OsAddressCoordinates(latitude, longitude));
    }

    private boolean isNotBlank(String value) {
        return value != null && !value.isBlank();
    }

    private String sanitiseOsException(FeignException exception) {
        return OS_API_KEY_QUERY_PARAMETER_PATTERN
            .matcher(exception.toString())
            .replaceAll("$1[REDACTED]");
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
            .map(OsResult::getDpa)
            .filter(Objects::nonNull)
            .map(OsDpa::getLocalCustodianCode)
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
