package com.example.search.service;

import com.example.search.dto.SearchResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Duration;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

@Service
public class CacheService {

    private static final Logger log = LoggerFactory.getLogger(CacheService.class);

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    public CacheService(StringRedisTemplate redisTemplate, ObjectMapper objectMapper) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
    }

    public SearchResponse getSearch(String key) {
        try {
            String value = redisTemplate.opsForValue().get(key);
            if (value == null) {
                return null;
            }
            return objectMapper.readValue(value, SearchResponse.class);
        } catch (Exception ex) {
            log.warn("Failed to read search cache for key={}", key, ex);
            return null;
        }
    }

    public void putSearch(String key, SearchResponse response, Duration ttl) {
        try {
            String json = objectMapper.writeValueAsString(response);
            redisTemplate.opsForValue().set(key, json, ttl);
            log.info("Cached search response key={} ttlSeconds={}", key, ttl.getSeconds());
        } catch (Exception ex) {
            log.warn("Failed to write search cache for key={}", key, ex);
        }
    }

    public void evictTenantSearches(String tenantId) {
        try {
            Set<String> keys = redisTemplate.keys("search:" + tenantId + ":*");
            if (keys != null && !keys.isEmpty()) {
                redisTemplate.delete(keys);
                log.info("Evicted {} cached search entries for tenant={}", keys.size(), tenantId);
            }
        } catch (Exception ex) {
            log.warn("Failed to evict search cache for tenant={}", tenantId, ex);
        }
    }
}
