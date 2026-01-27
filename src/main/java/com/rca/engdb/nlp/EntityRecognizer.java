package com.rca.engdb.nlp;

import com.rca.engdb.schema.SchemaRegistry;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class EntityRecognizer {

    private final SchemaRegistry schemaRegistry;

    public EntityRecognizer(SchemaRegistry schemaRegistry) {
        this.schemaRegistry = schemaRegistry;
    }

    public EntityRecognitionResult recognize(List<String> tokens) {
        var schema = schemaRegistry.getSchema();
        
        String bestTable = null;
        double bestTableScore = 0.0;
        List<String> recognizedColumns = new ArrayList<>();
        
        // Find best matching table
        for (String token : tokens) {
            for (String tableName : schema.keySet()) {
                double score = calculateSimilarity(token, tableName);
                if (score > bestTableScore && score > 0.6) {
                    bestTableScore = score;
                    bestTable = tableName;
                }
            }
        }
        
        // If table found, look for column names
        if (bestTable != null) {
            List<String> columns = schema.get(bestTable);
            for (String token : tokens) {
                for (String columnName : columns) {
                    double score = calculateSimilarity(token, columnName);
                    if (score > 0.7 && !recognizedColumns.contains(columnName)) {
                        recognizedColumns.add(columnName);
                    }
                }
            }
        }
        
        // Calculate overall confidence
        double confidence = bestTableScore;
        if (!recognizedColumns.isEmpty()) {
            confidence = Math.min(1.0, confidence + 0.1 * recognizedColumns.size());
        }
        
        return new EntityRecognitionResult(
            bestTable,
            recognizedColumns.isEmpty() ? List.of("*") : recognizedColumns,
            confidence
        );
    }

    /**
     * Calculate similarity between two strings using a simple approach:
     * - Exact match = 1.0
     * - Singular/plural match = 0.9
     * - Substring match = 0.7
     * - No match = 0.0
     */
    private double calculateSimilarity(String token, String target) {
        token = token.toLowerCase();
        target = target.toLowerCase();
        
        // Exact match
        if (token.equals(target)) {
            return 1.0;
        }
        
        // Handle plural/singular
        if (token.equals(target + "s") || (token + "s").equals(target)) {
            return 0.9;
        }
        
        // Handle common variations
        if (token.endsWith("s") && token.substring(0, token.length() - 1).equals(target)) {
            return 0.9;
        }
        
        if (target.endsWith("s") && target.substring(0, target.length() - 1).equals(token)) {
            return 0.9;
        }
        
        // Substring match
        if (token.contains(target) || target.contains(token)) {
            return 0.7;
        }
        
        return 0.0;
    }

    public static class EntityRecognitionResult {
        private final String table;
        private final List<String> columns;
        private final double confidence;

        public EntityRecognitionResult(String table, List<String> columns, double confidence) {
            this.table = table;
            this.columns = columns;
            this.confidence = confidence;
        }

        public String getTable() {
            return table;
        }

        public List<String> getColumns() {
            return columns;
        }

        public double getConfidence() {
            return confidence;
        }
    }
}
