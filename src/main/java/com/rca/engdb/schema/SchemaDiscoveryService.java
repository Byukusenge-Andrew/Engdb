package com.rca.engdb.schema;

import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class SchemaDiscoveryService {

    private final DataSource dataSource;
    private final Map<String, List<String>> cachedSchema = new ConcurrentHashMap<>();
    private final SchemaGraph schemaGraph = new SchemaGraph();
    private long lastRefreshTime = 0;
    private static final long DEFAULT_CACHE_TTL_MS = 60 * 60 * 1000; // 1 hour default
    private long cacheTtlMs = DEFAULT_CACHE_TTL_MS;

    public SchemaDiscoveryService(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    /**
     * Discover schema from the database or return cached version
     * Cache is refreshed if TTL has expired
     */
    public Map<String, List<String>> discoverSchema() {
        long now = System.currentTimeMillis();
        boolean cacheExpired = (now - lastRefreshTime) > cacheTtlMs;
        
        if (cachedSchema.isEmpty() || cacheExpired) {
            refreshSchema();
            lastRefreshTime = now;
        }
        
        return cachedSchema;
    }

    /**
     * Force refresh of the schema from the database
     */
    public void refreshSchema() {
        Map<String, List<String>> newSchema = new HashMap<>();
        
        try (Connection connection = dataSource.getConnection()) {
            DatabaseMetaData metaData = connection.getMetaData();
            
            // Get all tables
            try (ResultSet tables = metaData.getTables(null, null, "%", new String[]{"TABLE"})) {
                while (tables.next()) {
                    String tableName = tables.getString("TABLE_NAME");
                    List<String> columns = new ArrayList<>();
                    
                    // Get columns for this table
                    try (ResultSet cols = metaData.getColumns(null, null, tableName, "%")) {
                        while (cols.next()) {
                            columns.add(cols.getString("COLUMN_NAME"));
                        }
                    }
                    
                    newSchema.put(tableName, columns);
                }
            }
            
            cachedSchema.clear();
            cachedSchema.putAll(newSchema);
            
            // Discover foreign keys and build schema graph
            discoverForeignKeys();
            
            if (cachedSchema.isEmpty()) {
                System.out.println("WARNING: No tables found in the database!");
            }
            
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Discover foreign key relationships and populate SchemaGraph
     */
    public void discoverForeignKeys() {
        try (Connection connection = dataSource.getConnection()) {
            DatabaseMetaData metaData = connection.getMetaData();
            
            for (String tableName : cachedSchema.keySet()) {
                // Get imported keys (foreign keys in this table)
                try (ResultSet foreignKeys = metaData.getImportedKeys(null, null, tableName)) {
                    while (foreignKeys.next()) {
                        String fkTable = foreignKeys.getString("FKTABLE_NAME");
                        String fkColumn = foreignKeys.getString("FKCOLUMN_NAME");
                        String pkTable = foreignKeys.getString("PKTABLE_NAME");
                        String pkColumn = foreignKeys.getString("PKCOLUMN_NAME");
                        
                        // Add relationship to schema graph
                        schemaGraph.addRelationship(fkTable, fkColumn, pkTable, pkColumn);
                        
                        System.out.println("Found FK: " + fkTable + "." + fkColumn + " -> " + pkTable + "." + pkColumn);
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    public List<String> getTableNames() {
        return new ArrayList<>(discoverSchema().keySet());
    }
    
    public List<String> getColumns(String tableName) {
        return discoverSchema().getOrDefault(tableName, new ArrayList<>());
    }

    /**
     * Get the schema graph for JOIN path finding
     */
    public SchemaGraph getSchemaGraph() {
        if (schemaGraph.getAllTables().isEmpty()) {
            discoverForeignKeys();
        }
        return schemaGraph;
    }
    
    /**
     * Set cache TTL in minutes
     */
    public void setCacheTtlMinutes(long minutes) {
        this.cacheTtlMs = minutes * 60 * 1000;
    }
    
    /**
     * Clear the cache and force refresh on next access
     */
    public void clearCache() {
        cachedSchema.clear();
        schemaGraph.clearCache();
        lastRefreshTime = 0;
    }
    
    /**
     * Get cache statistics
     */
    public String getCacheStats() {
        long ageMs = System.currentTimeMillis() - lastRefreshTime;
        long ageMinutes = ageMs / (60 * 1000);
        return String.format("Cache age: %d minutes, Tables: %d, TTL: %d minutes",
            ageMinutes, cachedSchema.size(), cacheTtlMs / (60 * 1000));
    }
}
