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

    public SchemaDiscoveryService(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    /**
     * Discover schema from the database or return cached version
     */
    public Map<String, List<String>> discoverSchema() {
        if (!cachedSchema.isEmpty()) {
            return cachedSchema;
        }
        
        refreshSchema();
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
            
            // If empty (e.g., first run), maybe add some dummy data for testing if no DB exists?
            // Or better, let it be empty so we know to instruct user.
            if (cachedSchema.isEmpty()) {
                System.out.println("WARNING: No tables found in the database!");
            }
            
        } catch (Exception e) {
            e.printStackTrace();
            // Fallback?
        }
    }
    
    public List<String> getTableNames() {
        return new ArrayList<>(discoverSchema().keySet());
    }
    
    public List<String> getColumns(String tableName) {
        return discoverSchema().getOrDefault(tableName, new ArrayList<>());
    }
}
