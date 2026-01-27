package com.rca.engdb.schema;

import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
public class SchemaRegistry {

    private final SchemaDiscoveryService discoveryService;

    public SchemaRegistry(SchemaDiscoveryService discoveryService) {
        this.discoveryService = discoveryService;
    }

    // Hardcoded fallback for now, but primary source is discovery
    private static final Map<String, List<String>> FALLBACK_SCHEMA = Map.of(
        "students", List.of("id", "name", "age", "department"),
        "courses", List.of("id", "name", "credits"),
        "enrollments", List.of("student_id", "course_id", "grade")
    );

    public Map<String, List<String>> getSchema() {
        Map<String, List<String>> discovered = discoveryService.discoverSchema();
        if (discovered.isEmpty()) {
            return FALLBACK_SCHEMA;
        }
        return discovered;
    }
}
