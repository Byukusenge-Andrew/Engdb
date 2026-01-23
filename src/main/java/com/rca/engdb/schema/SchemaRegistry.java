package com.rca.engdb.schema;

import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
public class SchemaRegistry {

    private static final Map<String, List<String>> SCHEMA = Map.of(
        "students", List.of("id", "name", "age", "department"),
        "courses", List.of("id", "name", "credits"),
        "enrollments", List.of("student_id", "course_id", "grade")
    );

    public Map<String, List<String>> getSchema() {
        return SCHEMA;
    }
}
