package uk.gov.hmcts.reform.fact.data.api.clients;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.restclient.RestTemplateBuilder;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;
import tools.jackson.databind.JsonNode;
import uk.gov.hmcts.reform.fact.data.api.config.OnspdProperties;
import uk.gov.hmcts.reform.fact.data.api.errorhandling.exceptions.PostcodeReferenceImportException;
import uk.gov.hmcts.reform.fact.data.api.models.OnspdRelease;

import java.net.URI;
import java.time.Duration;
import java.time.Month;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
@Slf4j
public class OnspdClient {

    static final String CATALOGUE_QUERY =
        "owner:ONSGeography_data AND tags:PRD_ONSPD AND type:\"Feature Service\"";
    // Filtering at source avoids downloading roughly one million ordinary postcode records on every new release.
    static final String POSTCODE_SELECTION_FILTER = "usrtypind = 1 OR doterm IS NOT NULL";
    static final String POSTCODE_FIELDS = "ObjectId,pcds,dointr,doterm,usrtypind,gridind,lat,long";

    private static final Pattern RELEASE_PATTERN = Pattern.compile(
        "\\((January|February|March|April|May|June|July|August|September|October|November|December) "
            + "(\\d{4})\\)",
        Pattern.CASE_INSENSITIVE
    );
    private static final DateTimeFormatter MONTH_FORMATTER = DateTimeFormatter.ofPattern("MMMM", Locale.ENGLISH);

    private final RestTemplate restTemplate;
    private final OnspdProperties properties;

    @Autowired
    public OnspdClient(RestTemplateBuilder restTemplateBuilder, OnspdProperties properties) {
        this(
            restTemplateBuilder
                .connectTimeout(Duration.ofSeconds(30))
                .readTimeout(Duration.ofMinutes(2))
                .build(),
            properties
        );
    }

    OnspdClient(RestTemplate restTemplate, OnspdProperties properties) {
        this.restTemplate = restTemplate;
        this.properties = properties;
    }

    /**
     * Finds the newest valid ONSPD hosted-table release by its title date rather than catalogue creation order.
     *
     * @return latest valid ONSPD release
     * @throws PostcodeReferenceImportException when the catalogue cannot provide a valid release
     */
    public OnspdRelease findLatestRelease() {
        URI uri = UriComponentsBuilder.fromUriString(properties.getCatalogueUrl())
            .queryParam("f", "json")
            .queryParam("num", 100)
            .queryParam("sortField", "created")
            .queryParam("sortOrder", "desc")
            .queryParam("q", CATALOGUE_QUERY)
            .build()
            .encode()
            .toUri();

        JsonNode response = get(uri, "catalogue search");
        List<OnspdRelease> releases = new ArrayList<>();
        response.path("results").forEach(result -> toRelease(result).ifPresent(releases::add));

        return releases.stream()
            .max(Comparator.comparing(OnspdRelease::releaseDate).thenComparingLong(OnspdRelease::modified))
            .orElseThrow(() -> new PostcodeReferenceImportException(
                "No ONSPD hosted-table release found in the catalogue"));
    }

    /**
     * Counts the remotely filtered records so an incomplete or changing export cannot be imported.
     *
     * @param release ONSPD release to query
     * @return number of large-user or terminated postcode records
     * @throws PostcodeReferenceImportException when ArcGIS does not return a valid count
     */
    public long getFilteredPostcodeCount(OnspdRelease release) {
        URI uri = filteredPostcodeQueryUri(release)
            .queryParam("returnCountOnly", true)
            .build()
            .encode()
            .toUri();

        JsonNode response = get(uri, "record count query");
        if (!response.path("count").canConvertToLong()) {
            throw new PostcodeReferenceImportException("ONSPD record count response did not contain a valid count");
        }
        return response.path("count").asLong();
    }

    /**
     * Retrieves the next page using ObjectId keyset pagination, avoiding slow high ArcGIS offsets.
     *
     * @param release ONSPD release to query
     * @param lastObjectId final object ID processed from the preceding page
     * @return postcode attributes ordered by object ID
     * @throws PostcodeReferenceImportException when ArcGIS cannot return a valid page
     */
    public List<JsonNode> getPostcodeRecordsAfter(OnspdRelease release, long lastObjectId) {
        URI uri = filteredPostcodeQueryUri(release)
            .queryParam("outFields", POSTCODE_FIELDS)
            .queryParam("returnGeometry", false)
            .queryParam("orderByFields", "ObjectId")
            .queryParam("resultRecordCount", properties.getPageSize())
            .replaceQueryParam("where", postcodePageFilter(lastObjectId))
            .build()
            .encode()
            .toUri();

        JsonNode response = get(uri, "postcode query after ObjectId " + lastObjectId);
        List<JsonNode> records = new ArrayList<>();
        response.path("features").forEach(feature -> records.add(feature.path("attributes")));
        return records;
    }

    private UriComponentsBuilder filteredPostcodeQueryUri(OnspdRelease release) {
        validateServiceUrl(release.serviceUrl());
        return UriComponentsBuilder.fromUriString(release.serviceUrl() + "/0/query")
            .queryParam("f", "json")
            .queryParam("where", POSTCODE_SELECTION_FILTER);
    }

    private String postcodePageFilter(long lastObjectId) {
        return "(" + POSTCODE_SELECTION_FILTER + ") AND ObjectId > " + lastObjectId;
    }

    private Optional<OnspdRelease> toRelease(JsonNode result) {
        String title = result.path("title").asText();
        if (!title.startsWith("ONS Postcode Directory") || !title.contains("Hosted Table")) {
            return Optional.empty();
        }

        Matcher matcher = RELEASE_PATTERN.matcher(title);
        if (!matcher.find()) {
            return Optional.empty();
        }

        try {
            Month month = Month.from(MONTH_FORMATTER.parse(capitalise(matcher.group(1))));
            YearMonth releaseDate = YearMonth.of(Integer.parseInt(matcher.group(2)), month);
            if (!result.path("id").isString()
                || !result.path("url").isString()
                || !result.path("modified").canConvertToLong()) {
                return Optional.empty();
            }
            return Optional.of(new OnspdRelease(
                result.path("id").asText(),
                title,
                releaseDate,
                result.path("url").asText(),
                result.path("modified").asLong()
            ));
        } catch (DateTimeParseException | NumberFormatException ex) {
            return Optional.empty();
        }
    }

    private JsonNode get(URI uri, String operation) {
        PostcodeReferenceImportException lastFailure = null;
        int maxAttempts = Math.max(1, properties.getMaxAttempts());
        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            try {
                JsonNode response = restTemplate.getForObject(uri, JsonNode.class);
                if (response == null) {
                    throw new PostcodeReferenceImportException("Empty response received from ONSPD source");
                }
                validateArcGisResponse(response, operation);
                return response;
            } catch (RestClientException ex) {
                lastFailure = new PostcodeReferenceImportException(
                    "Unable to retrieve ONSPD data during " + operation, ex);
            } catch (PostcodeReferenceImportException ex) {
                lastFailure = ex;
            }

            if (attempt < maxAttempts) {
                log.warn(
                    "ONSPD request failed during {} (attempt {} of {}): {}",
                    operation,
                    attempt,
                    maxAttempts,
                    lastFailure.getMessage()
                );
                waitBeforeRetry(attempt, operation);
            }
        }
        throw lastFailure;
    }

    private void waitBeforeRetry(int attempt, String operation) {
        long delay = Math.max(0, properties.getInitialRetryBackoffMillis())
            * (1L << Math.min(attempt - 1, 10));
        try {
            Thread.sleep(delay);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new PostcodeReferenceImportException("Interrupted while retrying " + operation, ex);
        }
    }

    private void validateServiceUrl(String serviceUrl) {
        URI uri;
        try {
            uri = URI.create(serviceUrl);
        } catch (IllegalArgumentException ex) {
            throw new PostcodeReferenceImportException("ONSPD catalogue returned an invalid service URL", ex);
        }

        if (!"https".equalsIgnoreCase(uri.getScheme())
            || !properties.getAllowedServiceHost().equalsIgnoreCase(uri.getHost())) {
            throw new PostcodeReferenceImportException(
                "ONSPD catalogue returned a service URL from an unexpected host");
        }
    }

    private void validateArcGisResponse(JsonNode response, String operation) {
        if (response.has("error")) {
            JsonNode error = response.path("error");
            List<String> details = new ArrayList<>();
            error.path("details").forEach(detail -> details.add(detail.asText()));
            String description = "code " + error.path("code").asText("unknown")
                + ": " + error.path("message").asText("No message supplied");
            if (!details.isEmpty()) {
                description += " (" + String.join("; ", details) + ")";
            }
            throw new PostcodeReferenceImportException(
                "ArcGIS returned an error during " + operation + " - " + description);
        }
    }

    private String capitalise(String value) {
        return value.substring(0, 1).toUpperCase(Locale.ENGLISH)
            + value.substring(1).toLowerCase(Locale.ENGLISH);
    }
}
