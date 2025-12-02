package uk.gov.hmcts.reform.fact.data.api.config;

import uk.gov.hmcts.reform.fact.data.api.config.properties.RateLimitBucketConfigurationProperties;
import uk.gov.hmcts.reform.fact.data.api.config.properties.RateLimitConfigurationProperties;

import java.time.Duration;
import java.util.Optional;
import java.util.function.Supplier;

import javax.cache.CacheManager;

import com.giffing.bucket4j.spring.boot.starter.config.cache.SyncCacheResolver;
import com.giffing.bucket4j.spring.boot.starter.config.cache.jcache.JCacheCacheResolver;
import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.BandwidthBuilder;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.BucketConfiguration;
import io.github.bucket4j.distributed.proxy.ProxyManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Configuration
@RequiredArgsConstructor
@Slf4j
public class Bucket4JConfiguration {

    public static final String CACHE_NAME = "fact-data-api-bucket-cache";

    private static final String DEFAULT_BUCKET_NAME = "__default__";
    public static final String RL_ID_HEADER_FIELD = "Rate-Limit-Identifier";
    private static final String KEY_FORMAT = "%s-%s";

    private final ProxyManager<String> buckets;
    private final RateLimitConfigurationProperties rateLimitConfigurationProperties;

    @Bean
    public SyncCacheResolver bucket4jCacheResolver(CacheManager cacheManager) {
        return new JCacheCacheResolver(cacheManager);
    }

    public Bucket resolveBucket(String bucketName) {
        RateLimitBucketConfigurationProperties bucketProperties = rateLimitConfigurationProperties.getBuckets()
            .getOrDefault(bucketName, rateLimitConfigurationProperties.getDefaultBucket());

        Supplier<BucketConfiguration> configSupplier = getBucketConfiguration(bucketProperties);

        String bucketSuffix = Optional.ofNullable(bucketName)
            .filter(s -> rateLimitConfigurationProperties.getBuckets().containsKey(s.trim()))
            .orElse(DEFAULT_BUCKET_NAME);

        String key = bucketProperties.isPerSession() ? buildKey(bucketSuffix) : bucketSuffix;

        return buckets.builder().build(key, configSupplier);
    }

    private Supplier<BucketConfiguration> getBucketConfiguration(
        RateLimitBucketConfigurationProperties bucketProperties) {

        // standard bandwidth
        Bandwidth bandwidth = BandwidthBuilder.builder()
            .capacity(bucketProperties.getCapacity())
            .refillGreedy(bucketProperties.getCapacity(), Duration.ofSeconds(bucketProperties.getIntervalSeconds()))
            .build();

        // burst bandwidth
        Bandwidth burstBandwidth = BandwidthBuilder.builder()
            .capacity(bucketProperties.getBurstCapacity())
            .refillIntervally(
                bucketProperties.getBurstCapacity(),
                Duration.ofSeconds(bucketProperties.getBurstIntervalSeconds())
            )
            .build();

        return () -> (BucketConfiguration.builder()
            .addLimit(bandwidth)
            .addLimit(burstBandwidth)
            .build());
    }

    private String buildKey(final String bucketName) {

        // For now, I'll just use a header variable as the unique identifier for
        // the rate limiting bucket. In production, this would be either the user
        // id (SSO id? as we know we'll have access to that for admin frontend
        // interactions), or the session Id (which will require mandating that the
        // public frontend maintains an internal session ID and provides it in
        // request header?)

        ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        String prefix = Optional.ofNullable(attrs)
            .map(ServletRequestAttributes::getRequest)
            .map(req -> req.getHeader(RL_ID_HEADER_FIELD))
            .orElse("default");

        return String.format(KEY_FORMAT, prefix, bucketName);
    }
}
