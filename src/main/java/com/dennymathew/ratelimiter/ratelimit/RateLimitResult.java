package com.dennymathew.ratelimiter.ratelimit;

public record RateLimitResult(
        boolean allowed,
        long remaining,
        long retryAfterSeconds
) {
}