package com.rca.engdb.nlp;

import com.rca.engdb.schema.SchemaRegistry;
import com.rca.engdb.util.StringUtils;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class EntityRecognizer {

    private final SchemaRegistry schemaRegistry;
    private final SynonymRegistry synonymRegistry;

    public EntityRecognizer(SchemaRegistry schemaRegistry, SynonymRegistry synonymRegistry) {
        this.schemaRegistry = schemaRegistry;
        this.synonymRegistry = synonymRegistry;
    }

    public EntityRecognitionResult recognize(List<String> tokens) {
        return recognize(tokens, null);
    }

    public EntityRecognitionResult recognize(List<String> tokens, String dbName) {
        var schema = schemaRegistry.getSchema(dbName);
        
        String bestTable = null;
        double bestTableScore = 0.0;
        List<String> recognizedColumns = new ArrayList<>();
        
        // Find best matching table
        for (String token : tokens) {
            // 1. Resolve Synonyms
            String resolvedToken = synonymRegistry.resolve(token);
            
            // 2. Check each table in schema
            for (String tableName : schema.keySet()) {
                // Check original token
                double score = calculateSimilarity(token, tableName);
                
                // Check resolved synonym if different
                if (!resolvedToken.equals(token)) {
                    score = Math.max(score, calculateSimilarity(resolvedToken, tableName));
                }

                // 3. Fuzzy Match (if score is low)
                if (score < 0.8) {
                    // Allow 1 edit for short words (len<=4), 2 for longer
                    int maxDist = tableName.length() <= 4 ? 1 : 2;
                    int dist = StringUtils.calculateLevenshteinDistance(token, tableName);
                    if (dist <= maxDist) {
                        score = Math.max(score, 0.85); // High confidence for fuzzy match
                    }
                }

                if (score > bestTableScore && score > 0.6) {
                    bestTableScore = score;
                    bestTable = tableName;
                }
            }
        }
        
        // If table found, look for column names
        if (bestTable != null) {
            List<String> columns = schema.get(bestTable);
            
            for (int i = 0; i < tokens.size(); i++) {
                String token = tokens.get(i);
                String resolvedToken = synonymRegistry.resolve(token);
                
                for (String columnName : columns) {
                    double score = calculateSimilarity(token, columnName);
                    
                    if (!resolvedToken.equals(token)) {
                        score = Math.max(score, calculateSimilarity(resolvedToken, columnName));
                    }
                    
                    // Fuzzy match for columns
                    if (score < 0.8) {
                        int maxDist = columnName.length() <= 4 ? 1 : 2;
                        if (StringUtils.calculateLevenshteinDistance(token, columnName) <= maxDist) {
                            score = Math.max(score, 0.8);
                        }
                    }

                    if (score > 0.7 && !recognizedColumns.contains(columnName)) {
                        // Avoid misidentifying filter values as columns
                        boolean isFilter = isFilterContext(tokens, i, columns);
                        
                        if (!isFilter) {
                            recognizedColumns.add(columnName);
                        }
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
    
    private boolean isFilterContext(List<String> tokens, int currentIndex, List<String> columns) {
        if (currentIndex + 1 < tokens.size()) {
            String next = tokens.get(currentIndex + 1).toLowerCase();
            // If followed by something that isn't a connector, likely a filter
            if (!next.equals("and") && !next.equals("or") && !next.equals(",")) {
                // Unless the next word is ALSO a column
                boolean nextIsColumn = columns.stream().anyMatch(col -> col.equalsIgnoreCase(next));
                 
                 // Or a standard keyword
                 if (next.equals("is") || next.equals("=")) return false; // "column is value"

                 if (!nextIsColumn) {
                     return true;
                 }
            }
        }
        return false;
    }

    /**
     * Calculate similarity between two strings
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
            return 0.95;
        }
        
        // Handle common variations
        if (token.endsWith("s") && token.substring(0, token.length() - 1).equals(target)) {
            return 0.95;
        }
        
        if (target.endsWith("s") && target.substring(0, target.length() - 1).equals(token)) {
            return 0.95;
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
