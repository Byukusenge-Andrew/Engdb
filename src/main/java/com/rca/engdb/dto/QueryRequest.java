package com.rca.engdb.dto;

import jakarta.validation.constraints.NotBlank;

public class QueryRequest {

    @NotBlank
    private String query;

    private String databaseName;

    public String getQuery() {
        return query;
    }

    public void setQuery(String query) {
        this.query = query;
    }

    public String getDatabaseName() {
        return databaseName;
    }

    public void setDatabaseName(String databaseName) {
        this.databaseName = databaseName;
    }
}
