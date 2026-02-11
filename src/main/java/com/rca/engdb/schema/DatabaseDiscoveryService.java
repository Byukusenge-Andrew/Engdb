package com.rca.engdb.schema;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class DatabaseDiscoveryService {

    private final JdbcTemplate jdbcTemplate;
    private final org.springframework.data.mongodb.core.MongoTemplate mongoTemplate;
    private final com.mongodb.client.MongoClient mongoClient;

    public DatabaseDiscoveryService(JdbcTemplate jdbcTemplate, 
                                    org.springframework.data.mongodb.core.MongoTemplate mongoTemplate,
                                    com.mongodb.client.MongoClient mongoClient) {
        this.jdbcTemplate = jdbcTemplate;
        this.mongoTemplate = mongoTemplate;
        this.mongoClient = mongoClient;
    }

    /**
     * Get a list of all databases available on the server for a specific service type
     */
    public List<String> getAllDatabases(String serviceType) {
        if ("mongodb".equalsIgnoreCase(serviceType)) {
            return getMongoDatabases();
        } else {
            return getMysqlDatabases();
        }
    }

    /**
     * Default to MySQL for backward compatibility
     */
    public List<String> getAllDatabases() {
        return getMysqlDatabases();
    }

    private List<String> getMysqlDatabases() {
        try {
            return jdbcTemplate.queryForList("SHOW DATABASES", String.class)
                .stream()
                .filter(db -> !isSystemDatabase(db))
                .collect(Collectors.toList());
        } catch (Exception e) {
            System.err.println("Error discovering MySQL databases: " + e.getMessage());
            return List.of("engdb");
        }
    }

    private List<String> getMongoDatabases() {
        try {
            List<String> dbs = new java.util.ArrayList<>();
            mongoClient.listDatabaseNames().forEach(dbs::add);
            return dbs.stream()
                .filter(db -> !isMongoSystemDatabase(db))
                .collect(Collectors.toList());
        } catch (Exception e) {
            System.err.println("Error discovering MongoDB databases: " + e.getMessage());
            return List.of("test"); // Default fallback
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

    /**
     * Filter out MongoDB system databases
     */
    private boolean isMongoSystemDatabase(String dbName) {
        return List.of(
            "admin", 
            "local", 
            "config"
        ).contains(dbName.toLowerCase());
    }
}
