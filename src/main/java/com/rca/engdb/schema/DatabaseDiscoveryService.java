package com.rca.engdb.schema;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class DatabaseDiscoveryService {

    private final JdbcTemplate jdbcTemplate;

    public DatabaseDiscoveryService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /**
     * Get a list of all databases available on the server
     */
    public List<String> getAllDatabases() {
        try {
            return jdbcTemplate.queryForList("SHOW DATABASES", String.class)
                .stream()
                .filter(db -> !isSystemDatabase(db))
                .collect(Collectors.toList());
        } catch (Exception e) {
            System.err.println("Error discovering databases: " + e.getMessage());
            // Fallback: return at least the current default database
            return List.of("engdb");
        }
    }

    /**
     * Filter out MySQL system databases
     */
    private boolean isSystemDatabase(String dbName) {
        return List.of(
            "information_schema", 
            "mysql", 
            "performance_schema", 
            "sys"
        ).contains(dbName.toLowerCase());
    }
}
