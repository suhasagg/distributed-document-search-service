package com.example.search.service;

import java.util.Map;
import java.util.concurrent.TimeUnit;
import org.apache.kafka.clients.admin.AdminClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class KafkaHealthService {

    private final String bootstrapServers;

    public KafkaHealthService(@Value("${spring.kafka.bootstrap-servers}") String bootstrapServers) {
        this.bootstrapServers = bootstrapServers;
    }

    public boolean isUp() {
        try (AdminClient adminClient = AdminClient.create(Map.of("bootstrap.servers", bootstrapServers))) {
            adminClient.describeCluster().clusterId().get(3, TimeUnit.SECONDS);
            return true;
        } catch (Exception ex) {
            return false;
        }
    }
}
