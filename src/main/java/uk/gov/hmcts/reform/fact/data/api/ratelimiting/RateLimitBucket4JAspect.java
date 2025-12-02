package uk.gov.hmcts.reform.fact.data.api.ratelimiting;

import static uk.gov.hmcts.reform.fact.data.api.config.Bucket4JConfiguration.RL_ID_HEADER_FIELD;

import uk.gov.hmcts.reform.fact.data.api.config.Bucket4JConfiguration;
import uk.gov.hmcts.reform.fact.data.api.config.properties.RateLimitBucketConfigurationProperties;
import uk.gov.hmcts.reform.fact.data.api.config.properties.RateLimitConfigurationProperties;
import uk.gov.hmcts.reform.fact.data.api.errorhandling.exceptions.RateLimitExceededException;

import java.time.Duration;
import java.util.DoubleSummaryStatistics;
import java.util.Optional;
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
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Aspect
@Component
@Slf4j
@Scope(value = ConfigurableBeanFactory.SCOPE_SINGLETON)
@RequiredArgsConstructor
public class RateLimitBucket4JAspect {

    private final Bucket4JConfiguration bucket4JConfiguration;
    private final RateLimitConfigurationProperties rateLimitConfigurationProperties;

    DoubleSummaryStatistics doubleSummaryStatisticsSensible = new DoubleSummaryStatistics();
    DoubleSummaryStatistics doubleSummaryStatisticsAll = new DoubleSummaryStatistics();

    private final ReentrantLock lock = new ReentrantLock();

    @Around("@within(rateLimitAnnotation)")
    public Object classRateLimit(ProceedingJoinPoint joinPoint, RateLimitBucket4J rateLimitAnnotation)
        throws Throwable {
        return rateLimit(joinPoint, rateLimitAnnotation);
    }

    @Around("@annotation(rateLimitAnnotation)")
    public Object methodRateLimit(ProceedingJoinPoint joinPoint, RateLimitBucket4J rateLimitAnnotation)
        throws Throwable {
        return rateLimit(joinPoint, rateLimitAnnotation);
    }

    private Object rateLimit(ProceedingJoinPoint joinPoint, RateLimitBucket4J rateLimitAnnotation) throws Throwable {
        String bucketName = rateLimitAnnotation.bucket();

        log.debug("Rate limiting a request for bucket: {}", bucketName);

        RateLimitBucketConfigurationProperties bucketProperties = rateLimitConfigurationProperties.getBuckets()
            .getOrDefault(bucketName, rateLimitConfigurationProperties.getDefaultBucket());

        log.debug("Using bucket configuration: {}", bucketProperties);

        Bucket bucket = bucket4JConfiguration.resolveBucket(bucketName);
        log.debug("Consuming a token...");

        String session = getSession();

        // this is smart enough to ONLY block if there are no tokens and the
        // expected refill will have happened before the timeout, so if there
        // are no tokens, a 5-second wait for a refill, and a 3-second timeout
        // this will simply fail fast.

        // would we rather this acted as a throttle regardless?
        long nanos = System.nanoTime();
        VerboseResult<Boolean> result = bucket.asBlocking().asVerbose().tryConsume(
            1,
            Duration.ofSeconds(bucketProperties.getTimeoutSeconds())
        );
        double secs = (System.nanoTime() - nanos) * 0.000000001d;
        if (secs < 1d) {
            doubleSummaryStatisticsSensible.accept(secs);
        }
        doubleSummaryStatisticsAll.accept(secs);

        if (Boolean.FALSE.equals(result.getValue())) {
            log.info(
                "Token not available for bucket: {}, calculating wait period and raising an exception...",
                session
            );
            log.debug(
                "Current avg consume time: {}s / {}s (all)",
                doubleSummaryStatisticsSensible.getAverage(),
                doubleSummaryStatisticsAll.getAverage()
            );
            long waitNanos = result.getState().calculateDelayNanosAfterWillBePossibleToConsume(
                1, TimeUnit.MILLISECONDS.toNanos(System.currentTimeMillis()), false);
            log.info(
                "Wait period (in seconds) calculated as {}",
                TimeUnit.NANOSECONDS.toSeconds(waitNanos)
            );
            throw new RateLimitExceededException(
                TimeUnit.NANOSECONDS.toSeconds(waitNanos),
                "Rate limit exceeded"
            );
        }

        if (doubleSummaryStatisticsAll.getCount() % 500 == 0) {
            log.info("Total Requests: {}", doubleSummaryStatisticsAll.getCount());
            log.info(
                "Average bucket lookup time: {}",
                doubleSummaryStatisticsAll.getAverage()
            );
        }

        log.debug("Token is available, proceeding with request");
        // if we didn't throw an exception, proceed with execution
        return joinPoint.proceed();
    }

    private static String getSession() {
        ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        return Optional.ofNullable(attrs)
            .map(ServletRequestAttributes::getRequest)
            .map(req -> req.getHeader(RL_ID_HEADER_FIELD))
            .orElse("default");
    }
}
