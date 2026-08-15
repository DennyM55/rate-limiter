package com.dennymathew.ratelimiter.ratelimit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

@Service
public class RateLimiterService {
    private static final Logger log =
            LoggerFactory.getLogger(RateLimiterService.class);
    private static final long LIMIT = 10;
    private static final long WINDOW_MILLIS = 60_000;

    private final StringRedisTemplate redisTemplate;
    private final DefaultRedisScript<List> rateLimitScript;

    public RateLimiterService(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;

        this.rateLimitScript = new DefaultRedisScript<>();
        this.rateLimitScript.setLocation(
                new ClassPathResource("rate-limiter.lua")
        );
        this.rateLimitScript.setResultType(List.class);
    }

    public RateLimitResult check(String userId) {

        String key = "rate-limit:GET-tweets:" + userId;
        long now = System.currentTimeMillis();
        String requestId = UUID.randomUUID().toString();
        try {
            List<?> result = redisTemplate.execute(
                    rateLimitScript,
                    Collections.singletonList(key),
                    String.valueOf(now),
                    String.valueOf(WINDOW_MILLIS),
                    String.valueOf(LIMIT),
                    requestId
            );
            if (result == null || result.size() < 3) {
                throw new IllegalStateException(
                        "Invalid response from Redis rate limiter"
                );
            }

            boolean allowed =
                    ((Number) result.get(0)).longValue() == 1;

            long remaining =
                    ((Number) result.get(1)).longValue();

            long retryAfterSeconds =
                    ((Number) result.get(2)).longValue();

            return new RateLimitResult(
                    allowed,
                    remaining,
                    retryAfterSeconds
            );
        } catch (RedisConnectionFailureException exception) {

            log.warn(
                    "Redis unavailable; allowing request for user {}",
                    userId
            );

            return new RateLimitResult(true, LIMIT, 0);
        }

    }
}
