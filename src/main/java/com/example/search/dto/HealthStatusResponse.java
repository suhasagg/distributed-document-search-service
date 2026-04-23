package com.example.search.dto;

import java.util.Map;

public record HealthStatusResponse(String status, Map<String, Object> dependencies) {}
