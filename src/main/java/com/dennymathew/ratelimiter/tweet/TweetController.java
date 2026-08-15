package com.dennymathew.ratelimiter.tweet;

import com.dennymathew.ratelimiter.ratelimit.RateLimitResult;
import com.dennymathew.ratelimiter.ratelimit.RateLimiterService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/tweets")
public class TweetController {

    private final RateLimiterService rateLimiterService;

    public TweetController(RateLimiterService rateLimiterService) {
        this.rateLimiterService = rateLimiterService;
    }

    @GetMapping
    public ResponseEntity<?> getTweets(
            @RequestHeader("X-User-Id") String userId) {

        RateLimitResult result = rateLimiterService.check(userId);

        if (!result.allowed()) {
            return ResponseEntity.status(429)
                    .header("Retry-After",
                            String.valueOf(result.retryAfterSeconds()))
                    .body("Too Many Requests");
        }

        List<String> tweets = List.of(
                "Tweets requested by " + userId,
                "Learning system design",
                "Building a distributed rate limiter"
        );

        return ResponseEntity.ok()
                .header("X-RateLimit-Remaining",
                        String.valueOf(result.remaining()))
                .body(tweets);
    }
}