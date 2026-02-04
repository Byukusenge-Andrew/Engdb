package com.rca.engdb.exec;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class QueryExecutor {
    
    private final org.springframework.jdbc.core.JdbcTemplate jdbcTemplate;

    public QueryExecutor(org.springframework.jdbc.core.JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /**
     * Execute SQL query and return results
     */
    public QueryResult executeSQLQuery(String sql) {
        long startTime = System.currentTimeMillis();
        
        try {
            // JdbcTemplate.queryForList returns List<Map<String, Object>> with column names as keys
            List<Map<String, Object>> results = jdbcTemplate.queryForList(sql);
            long executionTime = System.currentTimeMillis() - startTime;
            
            return new QueryResult(
                results,
                results.size(),
                executionTime,
                true,
                null
            );
            
        } catch (Exception e) {
            long executionTime = System.currentTimeMillis() - startTime;
            return new QueryResult(
                new ArrayList<>(),
                0,
                executionTime,
                false,
                e.getMessage()
            );
        }
    }
    
    // formatResults method is no longer needed

    /**
     * Result container
     */
    public static class QueryResult {
        private final List<Map<String, Object>> data;
        private final int rowCount;
        private final long executionTimeMs;
        private final boolean success;
        private final String errorMessage;

        public QueryResult(List<Map<String, Object>> data, int rowCount, long executionTimeMs, 
                          boolean success, String errorMessage) {
            this.data = data;
            this.rowCount = rowCount;
            this.executionTimeMs = executionTimeMs;
            this.success = success;
            this.errorMessage = errorMessage;
        }

        public List<Map<String, Object>> getData() {
            return data;
        }

        public int getRowCount() {
            return rowCount;
        }

        public long getExecutionTimeMs() {
            return executionTimeMs;
        }

        public boolean isSuccess() {
            return success;
        }

        public String getErrorMessage() {
            return errorMessage;
        }
    }
}
