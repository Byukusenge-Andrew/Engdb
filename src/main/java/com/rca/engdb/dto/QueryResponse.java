package com.rca.engdb.dto;

import java.util.List;
import java.util.Map;

public class QueryResponse {

    private String intent;
    private String generatedQuery;
    private List<Map<String, Object>> results;
    private int rowCount;
    private double confidence;
    private long executionTimeMs;
    private String errorMessage;

    public QueryResponse(String intent, String generatedQuery, List<Map<String, Object>> results, 
                         int rowCount, double confidence, long executionTimeMs, String errorMessage) {
        this.intent = intent;
        this.generatedQuery = generatedQuery;
        this.results = results;
        this.rowCount = rowCount;
        this.confidence = confidence;
        this.executionTimeMs = executionTimeMs;
        this.errorMessage = errorMessage;
    }

    // Convenience constructor for errors or partial results
    public QueryResponse(String intent, List<String> tokens, double confidence) {
        this.intent = intent;
        this.confidence = confidence;
        // Legacy constructor support
    }

    public String getIntent() {
        return intent;
    }

    public String getGeneratedQuery() {
        return generatedQuery;
    }

    public List<Map<String, Object>> getResults() {
        return results;
    }

    public int getRowCount() {
        return rowCount;
    }

    public double getConfidence() {
        return confidence;
    }

    public long getExecutionTimeMs() {
        return executionTimeMs;
    }

    public String getErrorMessage() {
        return errorMessage;
    }
}
