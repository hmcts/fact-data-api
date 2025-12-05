package uk.gov.hmcts.reform.fact.data.api.config;

import static uk.gov.hmcts.reform.fact.data.api.config.Bucket4JConfiguration.CACHE_NAME;

import uk.gov.hmcts.reform.fact.data.api.config.properties.FactDataApiConfigurationProperties;
import uk.gov.hmcts.reform.fact.data.api.config.properties.RedisServerConfigurationProperties;

import java.io.Serializable;
import java.util.Optional;
import java.util.OptionalLong;
import java.util.concurrent.TimeUnit;

import javax.cache.Cache;
import javax.cache.CacheManager;
import javax.cache.configuration.FactoryBuilder;
import javax.cache.configuration.MutableCacheEntryListenerConfiguration;
import javax.cache.configuration.MutableConfiguration;
import javax.cache.event.CacheEntryEvent;
import javax.cache.event.CacheEntryExpiredListener;
import javax.cache.event.CacheEntryListenerException;
import javax.cache.expiry.Duration;
import javax.cache.expiry.ModifiedExpiryPolicy;
import javax.sql.DataSource;

import io.github.bucket4j.distributed.ExpirationAfterWriteStrategy;
import io.github.bucket4j.distributed.jdbc.PrimaryKeyMapper;
import io.github.bucket4j.distributed.proxy.ProxyManager;
import io.github.bucket4j.distributed.serialization.Mapper;
import io.github.bucket4j.grid.jcache.JCacheProxyManager;
import io.github.bucket4j.postgresql.Bucket4jPostgreSQL;
import io.github.bucket4j.postgresql.PostgreSQLSelectForUpdateBasedProxyManager;
import io.github.bucket4j.redis.redisson.Bucket4jRedisson;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.ClusterServersConfig;
import org.redisson.config.Config;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Configuration("redisConfiguration")
@Slf4j
@EnableCaching
@RequiredArgsConstructor
public class Bucket4JCacheConfiguration {

    private final FactDataApiConfigurationProperties factDataApiConfigurationProperties;

    private final SimpleCacheEntryListener listener = new SimpleCacheEntryListener();

    // -------------------------------------------------------------------
    // Redis config

    @Bean
    // rldev is just for this rate limit test; It lets me specifically
    // target the cluster config for testing without needing ssl enabled
    @Profile({"!dev & !test & !rldev"})
    public Config redissonConfiguration() {
        log.warn("Using PROD redisson configuration");

        Config config = new Config();
        ClusterServersConfig clusterServersConfig = config.useClusterServers();
        factDataApiConfigurationProperties.getRedisServers().forEach(redisServer -> {
            String connectionString = "rediss://"
                + (redisServer.getPassword() == null || redisServer.getPassword().isEmpty()
                ? ""
                : ":" + redisServer.getPassword() + "@")
                + redisServer.getHost() + ":" + redisServer.getPort();
            clusterServersConfig.addNodeAddress(connectionString);
        });

        return config;
    }

    @Bean
    @Profile({"rldev"})
    public Config redissonConfigurationRlDev() {
        log.warn("Using RLDEV redisson configuration");
        Config config = new Config();
        ClusterServersConfig clusterServersConfig = config.useClusterServers();
        factDataApiConfigurationProperties.getRedisServers().forEach(redisServer -> {
            String connectionString = "redis://"
                + (redisServer.getPassword() == null || redisServer.getPassword().isEmpty()
                ? ""
                : ":" + redisServer.getPassword() + "@")
                + redisServer.getHost() + ":" + redisServer.getPort();
            clusterServersConfig.addNodeAddress(connectionString);
        });
        return config;
    }

    @Bean
    @Profile({"dev", "test"})
    public Config redissonConfigurationDev() {
        log.warn("Using DEV/TEST redisson configuration");

        RedisServerConfigurationProperties redisServer =
            Optional.ofNullable(factDataApiConfigurationProperties.getRedisServers().getFirst())
                .orElseThrow(() -> new IllegalStateException("RedisServer configuration has not been set"));

        // in production, use "rediss://"
        String connectionString = "redis://"
            + (redisServer.getPassword() == null || redisServer.getPassword().isEmpty()
            ? ""
            : ":" + redisServer.getPassword() + "@")
            + redisServer.getHost() + ":" + redisServer.getPort();

        Config config = new Config();
        // in production, use config.useClusterServer().addNodeAddress()
        config.useSingleServer().setAddress(connectionString);
        return config;
    }

    @Bean
    @ConditionalOnProperty(
        prefix = "spring.cache.jcache",
        name = "provider",
        havingValue = "org.redisson.jcache.JCachingProvider",
        matchIfMissing = false)
    @ConditionalOnProperty(
        prefix = "fact.data-api.rate-limit",
        name = "use-postgres",
        havingValue = "false",
        matchIfMissing = true)
    public RedissonClient redissonClient(Config redissonConfig) {
        return Redisson.create(redissonConfig);
    }

    @Bean
    @ConditionalOnProperty(
        prefix = "spring.cache.jcache",
        name = "provider",
        havingValue = "org.redisson.jcache.JCachingProvider",
        matchIfMissing = false)
    @ConditionalOnProperty(
        prefix = "fact.data-api.rate-limit",
        name = "use-postgres",
        havingValue = "false",
        matchIfMissing = true)
    public ProxyManager<String> proxyManagerRedis(RedissonClient redissonClient) {
        if (redissonClient instanceof Redisson r) {
            return Bucket4jRedisson.casBasedBuilder(r.getCommandExecutor())
                .expirationAfterWrite(ExpirationAfterWriteStrategy.basedOnTimeForRefillingBucketUpToMax(
                    java.time.Duration.ofSeconds(10)))
                .keyMapper(Mapper.STRING)
                .build();
        }
        throw new IllegalStateException("RedissonClient is not instance of Redisson");
    }

    // -------------------------------------------------------------------
    // PostgresSQL config
    @Bean
    @ConditionalOnProperty(
        prefix = "fact.data-api.rate-limit",
        name = "use-postgres",
        havingValue = "true",
        matchIfMissing = false)
    public ProxyManager<String> proxyManagerPostgreSQL(DataSource dataSource) {
        Bucket4jPostgreSQL.PostgreSQLSelectForUpdateBasedProxyManagerBuilder<String> builder =
            new Bucket4jPostgreSQL.PostgreSQLSelectForUpdateBasedProxyManagerBuilder<>(
                dataSource,
                PrimaryKeyMapper.STRING
            );
        builder.expirationAfterWrite(ExpirationAfterWriteStrategy.basedOnTimeForRefillingBucketUpToMax(
            java.time.Duration.ofSeconds(10)));
        return new PostgreSQLSelectForUpdateBasedProxyManager<>(builder);
    }

    // -------------------------------------------------------------------
    // Local JCache config

    @Bean
    @ConditionalOnProperty(
        prefix = "spring.cache.jcache",
        name = "provider",
        havingValue = "com.github.benmanes.caffeine.jcache.spi.CaffeineCachingProvider",
        matchIfMissing = false)
    @ConditionalOnProperty(
        prefix = "fact.data-api.rate-limit",
        name = "use-postgres",
        havingValue = "false",
        matchIfMissing = true)
    public ProxyManager<String> proxyManagerCaffeine(CacheManager cacheManager) {
        com.github.benmanes.caffeine.jcache.configuration.CaffeineConfiguration<String, byte[]>
            caffeineConfiguration = new com.github.benmanes.caffeine.jcache.configuration.CaffeineConfiguration<>();
        caffeineConfiguration.setExpireAfterWrite(OptionalLong.of(TimeUnit.SECONDS.toNanos(5)));
        Cache<String, byte[]> cache = cacheManager.createCache(CACHE_NAME, caffeineConfiguration);
        cache.registerCacheEntryListener(new MutableCacheEntryListenerConfiguration<>(
            FactoryBuilder.factoryOf(this.listener), null, false, true));
        return new JCacheProxyManager<>(cache);
    }

    @Bean
    public MutableConfiguration<String, byte[]> jcacheConfiguration() {
        MutableConfiguration<String, byte[]> config = new MutableConfiguration<>();
        config.setExpiryPolicyFactory(ModifiedExpiryPolicy.factoryOf(
            new Duration(
                TimeUnit.SECONDS,
                60
            )));
        config.addCacheEntryListenerConfiguration(new MutableCacheEntryListenerConfiguration<>(
            FactoryBuilder.factoryOf(this.listener), null, false, true)
        );
        return config;
    }

    private static final class SimpleCacheEntryListener implements
        CacheEntryExpiredListener<Object, Object>,
        Serializable {
        @Override
        public void onExpired(final Iterable<CacheEntryEvent<?, ?>> cacheEntryEvents)
            throws CacheEntryListenerException {
            cacheEntryEvents.forEach(cacheEntryEvent -> {
                log.info("Cache entry expired - {}", cacheEntryEvent.getKey());
            });
        }
    }
}


