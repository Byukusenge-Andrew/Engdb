package com.rca.engdb.dto;

public class QueryResponse {

    private String generatedQuery;
    private Object result;
    private double confidence;

    public QueryResponse(String generatedQuery, Object result, double confidence) {
        this.generatedQuery = generatedQuery;
        this.result = result;
        this.confidence = confidence;
    }

    public String getGeneratedQuery() {
        return generatedQuery;
    }

    public Object getResult() {
        return result;
    }

    public double getConfidence() {
        return confidence;
    }
}
