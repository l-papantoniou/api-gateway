package com.example.api_gateway;


import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.BucketConfiguration;
import io.github.bucket4j.Refill;
import io.github.bucket4j.distributed.ExpirationAfterWriteStrategy;
import io.github.bucket4j.redis.lettuce.cas.LettuceBasedProxyManager;
import io.lettuce.core.RedisClient;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.codec.ByteArrayCodec;
import io.lettuce.core.codec.RedisCodec;
import io.lettuce.core.codec.StringCodec;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
public class RateLimiterService {


    // Redis proxy manager - manages token buckets in Redis
    private final LettuceBasedProxyManager<String> proxyManager;

    // Cache bucket configurations to avoid recreating them
    private final ConcurrentHashMap<String, BucketConfiguration> configCache = new ConcurrentHashMap<>();

    public RateLimiterService(RedisClient redisClient) {

        // Create Redis connection with proper codecs
        StatefulRedisConnection<String, byte[]> connection =
                redisClient.connect(RedisCodec.of(StringCodec.UTF8, ByteArrayCodec.INSTANCE));

        // Create a proxy manager - this handles storing buckets in Redis
        this.proxyManager = LettuceBasedProxyManager.builderFor(connection)
                .withExpirationStrategy(
                        // expire unused buckets after 1 hour of innactivity
                        ExpirationAfterWriteStrategy.basedOnTimeForRefillingBucketUpToMax(
                                Duration.ofHours(1)
                        )
                )
                .build();

        log.info("Rate Limiter service initialized with service");
    }


    /**
     * Check if request is allowed based on rate limit
     * This is the main method called by the filter
     *
     * @param key            Unique identifier (e.g., "user:123", "ip:192.168.1.1")
     * @param capacity       Maximum tokens in bucket
     * @param refillTokens   Number of tokens to refill
     * @param refillDuration Duration between refills
     * @return true if allowed (token consumed), false if rate limit exceeded
     */
    public boolean isAllowed(String key, int capacity, int refillTokens, Duration refillDuration) {
        // Get or create bucket configuration for these parameters
        BucketConfiguration config = getBucketConfiguration(capacity, refillTokens, refillDuration);

        // Get the bucket from Redis (creates if doesn't exist)
        Bucket bucket = proxyManager.builder().build(key, config);

        // Try to consume 1 token
        // Returns true if token available, false if bucket empty
        boolean allowed = bucket.tryConsume(1);

        if (!allowed) {
            log.warn("Rate limit exceeded for key: {}", key);
        } else {
            log.debug("Rate limit check passed for key: {}", key);
        }

        return allowed;
    }


    /**
     * Get remaining tokens for a key
     * Useful for monitoring and debugging
     */
    public long getRemainingTokens(String key, int capacity, int refillTokens, Duration refillDuration) {
        BucketConfiguration config = getBucketConfiguration(capacity, refillTokens, refillDuration);
        Bucket bucket = proxyManager.builder().build(key, config);

        return bucket.getAvailableTokens();
    }

    /**
     * Get or create bucket configuration
     * Configurations are cached to avoid recreation
     */
    private BucketConfiguration getBucketConfiguration(int capacity, int refillTokens, Duration refillDuration) {
        String configKey = generateConfigKey(capacity, refillTokens, refillDuration);

        return configCache.computeIfAbsent(configKey, k ->
                createBucketConfiguration(capacity, refillTokens, refillDuration)
        );
    }


    /**
     * Create bucket configuration with Token Bucket parameters
     */
    private BucketConfiguration createBucketConfiguration(int capacity, int refillTokens, Duration refillDuration) {
        // Create bandwidth (rate limit rule)
        // Classic mode: Simple refill strategy
        // capacity: Max tokens in bucket
        // Refill.intervally: Add refillTokens every refillDuration
        Bandwidth bandwidth = Bandwidth.classic(
                capacity,
                Refill.intervally(refillTokens, refillDuration)
        );

        // Build configuration
        return BucketConfiguration.builder()
                .addLimit(bandwidth)
                .build();
    }


    /**
     * Generate unique cache key for bucket configuration
     */
    private String generateConfigKey(int capacity, int refillTokens, Duration refillDuration) {
        return String.format("config:%d:%d:%d", capacity, refillTokens, refillDuration.getSeconds());
    }
}
