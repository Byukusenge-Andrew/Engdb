package com.rca.engdb.nlp;

import com.rca.engdb.ast.ConditionNode;
import com.rca.engdb.schema.SchemaRegistry;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
public class ConditionExtractor {

    private final SchemaRegistry schemaRegistry;
    
    // Common stop words that should not be treated as values
    private static final Set<String> STOP_WORDS = Set.of(
        "the", "a", "an", "of", "in", "on", "at", "to", "for", "with", 
        "from", "by", "about", "as", "into", "through", "during", "before", 
        "after", "above", "below", "between", "under", "again", "further",
        "then", "once", "here", "there", "when", "where", "why", "how",
        "all", "both", "each", "few", "more", "most", "other", "some",
        "such", "no", "nor", "not", "only", "own", "same", "so", "than",
        "too", "very", "can", "will", "just", "should", "now", "are", "is"
    );

    public ConditionExtractor(SchemaRegistry schemaRegistry) {
        this.schemaRegistry = schemaRegistry;
    }

    public List<ConditionNode> extractConditions(List<String> tokens, String tableName) {
        List<ConditionNode> conditions = new ArrayList<>();
        if (tableName == null) return conditions;

        List<String> columns = schemaRegistry.getSchema().getOrDefault(tableName, new ArrayList<>());

        for (int i = 0; i < tokens.size(); i++) {
            String token = tokens.get(i);
            
            // Check if token matches a column
            String column = findColumnMatch(token, columns);
            if (column != null) {
                // Look ahead for value
                String expectedValue = null;
                String operator = "=";
                
                if (i + 1 < tokens.size()) {
                    String next = tokens.get(i + 1);
                    if (isOperatorKeyword(next)) {
                        // Skip operator and get value
                        if (i + 2 < tokens.size()) {
                            String potentialValue = tokens.get(i + 2);
                            if (!isStopWord(potentialValue) && !isTableOrColumn(potentialValue, columns)) {
                                expectedValue = potentialValue;
                            }
                        }
                    } else if (!isStopWord(next) && !isTableOrColumn(next, columns)) {
                        // Implicit equality: "department CS"
                        expectedValue = next;
                    }
                }
                
                // If lookahead failed, check previous token (e.g., "CS department")
                if (expectedValue == null && i > 0) {
                    String prev = tokens.get(i - 1);
                    if (!isStopWord(prev) && !isOperatorKeyword(prev) && !isTableOrColumn(prev, columns)) {
                        expectedValue = prev;
                    }
                }
                
                if (expectedValue != null) {
                    conditions.add(new ConditionNode(column, operator, expectedValue));
                }
            }
        }
        
        return conditions;
    }

    private String findColumnMatch(String token, List<String> columns) {
        for (String col : columns) {
            if (col.equalsIgnoreCase(token)) return col;
        }
        return null;
    }

    private boolean isOperatorKeyword(String token) {
        return List.of("is", "equals", "equal", "in", "=", "like").contains(token.toLowerCase());
    }
    
    private boolean isStopWord(String token) {
        return STOP_WORDS.contains(token.toLowerCase());
    }
    
    private boolean isTableOrColumn(String token, List<String> columns) {
        // Check if token is a column name
        for (String col : columns) {
            if (col.equalsIgnoreCase(token)) return true;
        }
        // Check if token is a table name
        for (String table : schemaRegistry.getSchema().keySet()) {
            if (table.equalsIgnoreCase(token) || 
                (token + "s").equalsIgnoreCase(table) ||
                token.equalsIgnoreCase(table + "s")) {
                return true;
            }
        }
        return false;
    }
}
