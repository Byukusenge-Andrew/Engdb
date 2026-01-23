package com.rca.engdb.dto;

import jakarta.validation.constraints.NotBlank;

public class QueryRequest {

    @NotBlank
    private String query;

    public String getQuery() {
        return query;
    }

    public void setQuery(String query) {
        this.query = query;
    }
}
