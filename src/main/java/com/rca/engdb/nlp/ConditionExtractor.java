package com.rca.engdb.nlp;

import com.rca.engdb.ast.ConditionNode;
import com.rca.engdb.schema.SchemaRegistry;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class ConditionExtractor {

    private final SchemaRegistry schemaRegistry;

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
                // Heuristic: <column> <is/equals/in> <value>
                // Or: <column> <value>
                
                String expectedValue = null;
                String operator = "=";
                
                if (i + 1 < tokens.size()) {
                    String next = tokens.get(i + 1);
                    if (isOperatorKeyword(next)) {
                        if (i + 2 < tokens.size()) {
                            expectedValue = tokens.get(i + 2);
                        }
                    } else {
                        // Assuming implicit equality: "department CS"
                        expectedValue = next;
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
}
