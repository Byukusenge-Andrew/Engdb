package com.rca.engdb.engine;

import com.rca.engdb.ast.QueryAST;
import org.springframework.stereotype.Service;

@Service
public class QueryPlanner {
    
    /**
     * Determine which database to use for the query
     */
    public DatabaseType chooseDatabaseType(QueryAST ast) {
        // For now, default to MySQL for structured queries
        // In the future, could use MongoDB for unstructured data
        return DatabaseType.MYSQL;
    }

    /**
     * Estimate query complexity
     */
    public int estimateComplexity(QueryAST ast) {
        int complexity = 1;
        
        if (!ast.getWhereConditions().isEmpty()) {
            complexity += ast.getWhereConditions().size();
        }
        
        if (!ast.getJoins().isEmpty()) {
            complexity += ast.getJoins().size() * 2;
        }
        
        return complexity;
    }

    /**
     * Check if query needs optimization
     */
    public boolean needsOptimization(QueryAST ast) {
        return estimateComplexity(ast) > 5;
    }

    public enum DatabaseType {
        MYSQL,
        MONGODB
    }
}
