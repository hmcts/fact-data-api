package uk.gov.hmcts.reform.fact.data.api.ratelimiting;

import uk.gov.hmcts.reform.fact.data.api.config.Bucket4JConfiguration;
import uk.gov.hmcts.reform.fact.data.api.config.properties.RateLimitBucketConfigurationProperties;
import uk.gov.hmcts.reform.fact.data.api.config.properties.RateLimitConfigurationProperties;
import uk.gov.hmcts.reform.fact.data.api.errorhandling.exceptions.RateLimitExceededException;

import java.time.Duration;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

import io.github.bucket4j.Bucket;
import io.github.bucket4j.VerboseResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

@Aspect
@Component
@Slf4j
@Scope(value = ConfigurableBeanFactory.SCOPE_SINGLETON)
@RequiredArgsConstructor
public class RateLimitBucket4JAspect {

    private final Bucket4JConfiguration bucket4JConfiguration;
    private final RateLimitConfigurationProperties rateLimitConfigurationProperties;

    private final ReentrantLock lock = new ReentrantLock();

    @Around("@annotation(rateLimitAnnotation)")
    public Object rateLimit(ProceedingJoinPoint joinPoint, RateLimitBucket4J rateLimitAnnotation) throws Throwable {
        String bucketName = rateLimitAnnotation.bucket();

        log.info("Rate limiting a request for bucket: {}", bucketName);

        RateLimitBucketConfigurationProperties bucketProperties = rateLimitConfigurationProperties.getBuckets()
            .getOrDefault(bucketName, rateLimitConfigurationProperties.getDefaultBucket());

        log.info("Using bucket configuration: {}", bucketProperties);

        Bucket bucket = bucket4JConfiguration.resolveBucket(bucketName);

        log.info("Consuming a token...");

        // this is smart enough to ONLY block if there are no tokens and the
        // expected refill will have happened before the timeout, so if there
        // are no tokens, a 5 second wait for a refill, and a 3 second timeout
        // this will simply fail fast.

        // would we rather this acted as a throttle regardless?
        VerboseResult<Boolean> result = bucket.asBlocking().asVerbose().tryConsume(
            1,
            Duration.ofSeconds(bucketProperties.getTimeoutSeconds())
        );

        if (Boolean.FALSE.equals(result.getValue())) {
            log.info("Token not available, calculating wait period and raising an exception...");
            long waitNanos = result.getState().calculateDelayNanosAfterWillBePossibleToConsume(
                1, 0, false);
            log.info(
                "Wait period (in seconds) calculated as {}",
                TimeUnit.NANOSECONDS.toSeconds(waitNanos)
            );
            throw new RateLimitExceededException(
                TimeUnit.NANOSECONDS.toSeconds(waitNanos),
                "Rate limit exceeded"
            );
        }

        log.info("Token is available, proceeding with request");
        // if we didn't throw an exception, proceed with execution
        return joinPoint.proceed();
    }
}
