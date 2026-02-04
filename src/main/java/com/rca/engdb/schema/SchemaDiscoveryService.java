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
    // Map<DatabaseName, Map<TableName, List<ColumnName>>>
    private final Map<String, Map<String, List<String>>> globalSchemaCache = new ConcurrentHashMap<>();
    
    // Map<DatabaseName, SchemaGraph>
    private final Map<String, SchemaGraph> globalGraphCache = new ConcurrentHashMap<>();
    
    private final Map<String, Long> lastRefreshTimes = new ConcurrentHashMap<>();
    private static final long DEFAULT_CACHE_TTL_MS = 60 * 60 * 1000; // 1 hour default
    private long cacheTtlMs = DEFAULT_CACHE_TTL_MS;
    
    private String defaultDatabase = "engdb";

    public SchemaDiscoveryService(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    /**
     * Discover schema for the default database or return cached version
     */
    public Map<String, List<String>> discoverSchema() {
        return discoverSchema(defaultDatabase);
    }
    
    /**
     * Discover schema for a specific database
     */
    public Map<String, List<String>> discoverSchema(String dbName) {
        if (dbName == null || dbName.isEmpty()) dbName = defaultDatabase;
        
        long now = System.currentTimeMillis();
        long lastRefresh = lastRefreshTimes.getOrDefault(dbName, 0L);
        boolean cacheExpired = (now - lastRefresh) > cacheTtlMs;
        
        if (!globalSchemaCache.containsKey(dbName) || cacheExpired) {
            refreshSchema(dbName);
            lastRefreshTimes.put(dbName, now);
        }
        
        return globalSchemaCache.get(dbName);
    }

    /**
     * Force refresh of the schema for a specific database
     */
    public void refreshSchema(String dbName) {
        Map<String, List<String>> newSchema = new HashMap<>();
        SchemaGraph newGraph = new SchemaGraph();
        
        try (Connection connection = dataSource.getConnection()) {
            DatabaseMetaData metaData = connection.getMetaData();
            
            // Get tables for specific database (catalog)
            try (ResultSet tables = metaData.getTables(dbName, null, "%", new String[]{"TABLE"})) {
                while (tables.next()) {
                    String tableName = tables.getString("TABLE_NAME");
                    List<String> columns = new ArrayList<>();
                    
                    // Get columns
                    try (ResultSet cols = metaData.getColumns(dbName, null, tableName, "%")) {
                        while (cols.next()) {
                            columns.add(cols.getString("COLUMN_NAME"));
                        }
                    }
                    
                    newSchema.put(tableName, columns);
                }
            }
            
            globalSchemaCache.put(dbName, newSchema);
            globalGraphCache.put(dbName, newGraph);
            
            // Discover foreign keys
            discoverForeignKeys(dbName, newGraph);
            
            if (newSchema.isEmpty()) {
                System.out.println("WARNING: No tables found in database: " + dbName);
            }
            
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Discover foreign key relationships for a specific database
     */
    private void discoverForeignKeys(String dbName, SchemaGraph graph) {
        try (Connection connection = dataSource.getConnection()) {
            DatabaseMetaData metaData = connection.getMetaData();
            Map<String, List<String>> schema = globalSchemaCache.get(dbName);
            
            if (schema == null) return;
            
            for (String tableName : schema.keySet()) {
                try (ResultSet foreignKeys = metaData.getImportedKeys(dbName, null, tableName)) {
                    while (foreignKeys.next()) {
                        String fkTable = foreignKeys.getString("FKTABLE_NAME");
                        String fkColumn = foreignKeys.getString("FKCOLUMN_NAME");
                        String pkTable = foreignKeys.getString("PKTABLE_NAME");
                        String pkColumn = foreignKeys.getString("PKCOLUMN_NAME");
                        
                        graph.addRelationship(fkTable, fkColumn, pkTable, pkColumn);
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    public List<String> getTableNames(String dbName) {
        return new ArrayList<>(discoverSchema(dbName).keySet());
    }
    
    public List<String> getColumns(String dbName, String tableName) {
        return discoverSchema(dbName).getOrDefault(tableName, new ArrayList<>());
    }

    /**
     * Get the schema graph for the default database
     */
    public SchemaGraph getSchemaGraph() {
        return getSchemaGraph(defaultDatabase);
    }
    
    /**
     * Get the schema graph for a specific database
     */
    public SchemaGraph getSchemaGraph(String dbName) {
        if (dbName == null || dbName.isEmpty()) dbName = defaultDatabase;
        
        if (!globalGraphCache.containsKey(dbName)) {
            discoverSchema(dbName); // Triggers load
        }
        return globalGraphCache.get(dbName);
    }
    
    public void setDefaultDatabase(String dbName) {
        this.defaultDatabase = dbName;
    }
    
    public void setCacheTtlMinutes(long minutes) {
        this.cacheTtlMs = minutes * 60 * 1000;
    }
    
    public void clearCache() {
        globalSchemaCache.clear();
        globalGraphCache.clear();
        lastRefreshTimes.clear();
    }
    
    public String getCacheStats() {
        return String.format("Cached tables for [engdb]: %d", globalSchemaCache.getOrDefault("engdb", Map.of()).size());
    }
}
