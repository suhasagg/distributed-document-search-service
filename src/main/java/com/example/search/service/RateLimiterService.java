package com.example.search.service;

import com.example.search.exception.RateLimitExceededException;
import java.time.Duration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

@Service
public class RateLimiterService {

    private final StringRedisTemplate redisTemplate;
    private final long maxRequestsPerMinute;

    public RateLimiterService(StringRedisTemplate redisTemplate,
                              @Value("${rate-limit.requests-per-minute}") long maxRequestsPerMinute) {
        this.redisTemplate = redisTemplate;
        this.maxRequestsPerMinute = maxRequestsPerMinute;
    }

    public void validateSearchRate(String tenantId) {
        String key = "rl:search:" + tenantId + ":" + (System.currentTimeMillis() / 60000);
        Long count = redisTemplate.opsForValue().increment(key);
        if (count != null && count == 1L) {
            redisTemplate.expire(key, Duration.ofMinutes(2));
        }
        if (count != null && count > maxRequestsPerMinute) {
            throw new RateLimitExceededException("Tenant search rate limit exceeded");
        }
    }
}
