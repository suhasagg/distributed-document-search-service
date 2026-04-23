package com.example.search.controller;

import com.example.search.dto.HealthStatusResponse;
import com.example.search.service.KafkaHealthService;
import java.util.Map;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/health")
public class HealthController {

    private final JdbcTemplate jdbcTemplate;
    private final RedisConnectionFactory redisConnectionFactory;
    private final KafkaHealthService kafkaHealthService;

    public HealthController(JdbcTemplate jdbcTemplate,
                            RedisConnectionFactory redisConnectionFactory,
                            KafkaHealthService kafkaHealthService) {
        this.jdbcTemplate = jdbcTemplate;
        this.redisConnectionFactory = redisConnectionFactory;
        this.kafkaHealthService = kafkaHealthService;
    }

    @GetMapping
    public HealthStatusResponse health() {
        boolean dbUp = Integer.valueOf(1).equals(jdbcTemplate.queryForObject("select 1", Integer.class));
        boolean redisUp = redisConnectionFactory.getConnection().ping() != null;
        boolean kafkaUp = kafkaHealthService.isUp();
        String overall = dbUp && redisUp && kafkaUp ? "UP" : "DEGRADED";
        return new HealthStatusResponse(overall, Map.of(
                "postgres", dbUp ? "UP" : "DOWN",
                "redis", redisUp ? "UP" : "DOWN",
                "kafka", kafkaUp ? "UP" : "DOWN"
        ));
    }
}
