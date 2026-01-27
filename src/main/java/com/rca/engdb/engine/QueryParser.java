package com.rca.engdb.engine;

import com.rca.engdb.ast.QueryAST;
import com.rca.engdb.ml.IntentResult;
import com.rca.engdb.ml.IntentType;
import com.rca.engdb.nlp.ConditionExtractor;
import com.rca.engdb.nlp.EntityRecognizer;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class QueryParser {

    private final EntityRecognizer entityRecognizer;
    private final ConditionExtractor conditionExtractor;

    public QueryParser(EntityRecognizer entityRecognizer, ConditionExtractor conditionExtractor) {
        this.entityRecognizer = entityRecognizer;
        this.conditionExtractor = conditionExtractor;
    }

    /**
     * Parse tokens and intent into a QueryAST
     */
    public QueryAST parse(List<String> tokens, IntentResult intentResult) {
        QueryAST ast = new QueryAST();
        
        // Set intent
        ast.setIntent(intentResult.getIntent());
        
        // Recognize entities (tables and columns)
        EntityRecognizer.EntityRecognitionResult entities = entityRecognizer.recognize(tokens);
        
        if (entities.getTable() != null) {
            ast.setTargetTable(entities.getTable());
        }
        
        if (!entities.getColumns().isEmpty()) {
            ast.setSelectColumns(entities.getColumns());
        }
        
        // For aggregation queries, try to identify the column to aggregate
        if (isAggregationIntent(intentResult.getIntent())) {
            String aggColumn = findAggregateColumn(tokens, entities);
            if (aggColumn != null) {
                ast.setAggregateColumn(aggColumn);
            }
        }
        
        // Extract WHERE conditions
        if (ast.getTargetTable() != null) {
            ast.setWhereConditions(conditionExtractor.extractConditions(tokens, ast.getTargetTable()));
        }
        
        // TODO: Detect JOIN requirements
        // TODO: Extract ORDER BY and LIMIT
        
        return ast;
    }

    private boolean isAggregationIntent(IntentType intent) {
        return intent == IntentType.COUNT || 
               intent == IntentType.SUM || 
               intent == IntentType.AVG || 
               intent == IntentType.MAX || 
               intent == IntentType.MIN;
    }

    private String findAggregateColumn(List<String> tokens, EntityRecognizer.EntityRecognitionResult entities) {
        // Look for numeric-related keywords
        List<String> numericKeywords = List.of("age", "grade", "credit", "price", "salary", "score");
        
        for (String token : tokens) {
            if (numericKeywords.contains(token)) {
                return token;
            }
        }
        
        // If recognized columns exist, use the first non-id column
        for (String col : entities.getColumns()) {
            if (!col.equals("*") && !col.equals("id")) {
                return col;
            }
        }
        
        return null;
    }
}
