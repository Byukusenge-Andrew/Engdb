package com.rca.engdb.nlp;

import com.rca.engdb.schema.SchemaRegistry;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class JoinDetector {

    private final SchemaRegistry schemaRegistry;
    
    // Keywords that suggest JOIN operations
    private static final Set<String> JOIN_KEYWORDS = Set.of(
        "with", "and", "along", "including", "having", "their", "its"
    );

    public JoinDetector(SchemaRegistry schemaRegistry) {
        this.schemaRegistry = schemaRegistry;
    }

    /**
     * Detect if a query requires JOINs based on tokens
     */
    public JoinDetectionResult detectJoins(List<String> tokens) {
        return detectJoins(tokens, null);
    }

    /**
     * Detect if a query requires JOINs based on tokens
     */
    public JoinDetectionResult detectJoins(List<String> tokens, String dbName) {
        Set<String> detectedTables = new HashSet<>();
        Map<String, List<String>> schema = schemaRegistry.getSchema(dbName);
        
        // Strategy 1: Detect multiple table names in query
        for (String token : tokens) {
            for (String tableName : schema.keySet()) {
                if (isSimilar(token, tableName)) {
                    detectedTables.add(tableName);
                }
            }
        }
        
        // Strategy 2: Check for JOIN keywords
        boolean hasJoinKeywords = tokens.stream()
            .anyMatch(token -> JOIN_KEYWORDS.contains(token.toLowerCase()));
        
        // Strategy 3: Detect columns from different tables
        Set<String> tablesFromColumns = detectTablesFromColumns(tokens, schema);
        detectedTables.addAll(tablesFromColumns);
        
        boolean requiresJoin = detectedTables.size() > 1 || 
                              (hasJoinKeywords && !detectedTables.isEmpty());
        
        return new JoinDetectionResult(
            requiresJoin,
            new ArrayList<>(detectedTables),
            hasJoinKeywords
        );
    }

    /**
     * Detect which tables are referenced by column names
     */
    private Set<String> detectTablesFromColumns(List<String> tokens, Map<String, List<String>> schema) {
        Set<String> tables = new HashSet<>();
        
        for (String token : tokens) {
            for (Map.Entry<String, List<String>> entry : schema.entrySet()) {
                String tableName = entry.getKey();
                List<String> columns = entry.getValue();
                
                for (String column : columns) {
                    if (isSimilar(token, column)) {
                        tables.add(tableName);
                        break;
                    }
                }
            }
        }
        
        return tables;
    }

    /**
     * Simple similarity check (exact match or plural/singular)
     */
    private boolean isSimilar(String token, String target) {
        token = token.toLowerCase();
        target = target.toLowerCase();
        
        if (token.equals(target)) return true;
        if (token.equals(target + "s") || (token + "s").equals(target)) return true;
        if (token.endsWith("s") && token.substring(0, token.length() - 1).equals(target)) return true;
        if (target.endsWith("s") && target.substring(0, target.length() - 1).equals(token)) return true;
        
        return false;
    }

    /**
     * Result of JOIN detection
     */
    public static class JoinDetectionResult {
        private final boolean requiresJoin;
        private final List<String> detectedTables;
        private final boolean hasJoinKeywords;

        public JoinDetectionResult(boolean requiresJoin, List<String> detectedTables, boolean hasJoinKeywords) {
            this.requiresJoin = requiresJoin;
            this.detectedTables = detectedTables;
            this.hasJoinKeywords = hasJoinKeywords;
        }

        public boolean requiresJoin() {
            return requiresJoin;
        }

        public List<String> getDetectedTables() {
            return detectedTables;
        }

        public boolean hasJoinKeywords() {
            return hasJoinKeywords;
        }
    }
}
