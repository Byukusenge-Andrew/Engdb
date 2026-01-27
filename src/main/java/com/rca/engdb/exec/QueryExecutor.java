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
    
    @PersistenceContext
    private EntityManager entityManager;

    /**
     * Execute SQL query and return results
     */
    public QueryResult executeSQLQuery(String sql) {
        long startTime = System.currentTimeMillis();
        
        try {
            Query query = entityManager.createNativeQuery(sql);
            List<?> rawResults = query.getResultList();
            
            List<Map<String, Object>> formattedResults = formatResults(rawResults);
            long executionTime = System.currentTimeMillis() - startTime;
            
            return new QueryResult(
                formattedResults,
                formattedResults.size(),
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

    /**
     * Format raw query results into a list of maps
     */
    private List<Map<String, Object>> formatResults(List<?> rawResults) {
        List<Map<String, Object>> formatted = new ArrayList<>();
        
        for (Object row : rawResults) {
            Map<String, Object> rowMap = new HashMap<>();
            
            if (row instanceof Object[]) {
                // Multiple columns
                Object[] columns = (Object[]) row;
                for (int i = 0; i < columns.length; i++) {
                    rowMap.put("column_" + i, columns[i]);
                }
            } else {
                // Single column (e.g., COUNT, SUM)
                rowMap.put("result", row);
            }
            
            formatted.add(rowMap);
        }
        
        return formatted;
    }

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
