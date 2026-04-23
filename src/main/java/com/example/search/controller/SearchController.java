package com.example.search.controller;

import com.example.search.service.SearchService;
import java.io.IOException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping
public class SearchController {

    private final SearchService searchService;

    public SearchController(SearchService searchService) {
        this.searchService = searchService;
    }

    @GetMapping("/search")
    public ResponseEntity<?> search(
            @RequestHeader("X-Tenant-ID") String tenantId,
            @RequestParam("q") String query,
            @RequestParam(value = "size", defaultValue = "10") int size) throws IOException {

        return ResponseEntity.ok(searchService.search(tenantId, query, size));
    }
}
