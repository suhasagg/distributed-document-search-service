package com.example.search.service;

import com.example.search.dto.SearchResponse;
import com.example.search.dto.SearchResultItem;
import java.io.IOException;
import java.time.Duration;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class SearchService {

    private final ElasticsearchIndexService elasticsearchIndexService;
    private final CacheService cacheService;
    private final RateLimiterService rateLimiterService;

    public SearchService(ElasticsearchIndexService elasticsearchIndexService,
                         CacheService cacheService,
                         RateLimiterService rateLimiterService) {
        this.elasticsearchIndexService = elasticsearchIndexService;
        this.cacheService = cacheService;
        this.rateLimiterService = rateLimiterService;
    }

    public SearchResponse search(String tenantId, String query, int size) throws IOException {
        rateLimiterService.validateSearchRate(tenantId);

        String cacheKey = "search:" + tenantId + ":" + Integer.toHexString(query.hashCode()) + ":" + size;

        SearchResponse cached = cacheService.getSearch(cacheKey);
        if (cached != null) {
            return new SearchResponse(
                    cached.tenantId(),
                    cached.query(),
                    cached.total(),
                    cached.results(),
                    true
            );
        }

        List<SearchResultItem> results = elasticsearchIndexService.search(tenantId, query, size);
        SearchResponse response = new SearchResponse(tenantId, query, results.size(), results, false);

        if (!results.isEmpty()) {
            cacheService.putSearch(cacheKey, response, Duration.ofSeconds(60));
        }

        return response;
    }
}
