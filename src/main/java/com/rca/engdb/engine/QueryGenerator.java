package com.rca.engdb.engine;

import com.rca.engdb.ast.ConditionNode;
import com.rca.engdb.ast.JoinNode;
import com.rca.engdb.ast.QueryAST;
import com.rca.engdb.ml.IntentType;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class QueryGenerator {
    
    public String generateSQL(QueryAST ast) {
        if (ast.getTargetTable() == null) {
            throw new IllegalArgumentException("Target table cannot be null");
        }

        StringBuilder sql = new StringBuilder();

        // Build SELECT clause based on intent
        switch (ast.getIntent()) {
            case SELECT:
                sql.append("SELECT ");
                if (ast.getSelectColumns().isEmpty() || ast.getSelectColumns().contains("*")) {
                    sql.append("*");
                } else {
                    sql.append(String.join(", ", ast.getSelectColumns()));
                }
                break;

            case COUNT:
                sql.append("SELECT COUNT(");
                sql.append(ast.getAggregateColumn() != null ? ast.getAggregateColumn() : "*");
                sql.append(")");
                break;

            case SUM:
                sql.append("SELECT SUM(");
                sql.append(ast.getAggregateColumn() != null ? ast.getAggregateColumn() : "id");
                sql.append(")");
                break;

            case AVG:
                sql.append("SELECT AVG(");
                sql.append(ast.getAggregateColumn() != null ? ast.getAggregateColumn() : "id");
                sql.append(")");
                break;

            case MAX:
                sql.append("SELECT MAX(");
                sql.append(ast.getAggregateColumn() != null ? ast.getAggregateColumn() : "id");
                sql.append(")");
                break;

            case MIN:
                sql.append("SELECT MIN(");
                sql.append(ast.getAggregateColumn() != null ? ast.getAggregateColumn() : "id");
                sql.append(")");
                break;

            default:
                sql.append("SELECT *");
        }

        // FROM clause
        if (ast.getDatabaseName() != null && !ast.getDatabaseName().isEmpty()) {
            sql.append(" FROM ").append(ast.getDatabaseName()).append(".").append(ast.getTargetTable());
        } else {
            sql.append(" FROM ").append(ast.getTargetTable());
        }

        // JOIN clauses
        if (!ast.getJoins().isEmpty()) {
            for (JoinNode join : ast.getJoins()) {
                sql.append(" ");
                switch (join.getJoinType()) {
                    case INNER:
                        sql.append("INNER JOIN ");
                        break;
                    case LEFT:
                        sql.append("LEFT JOIN ");
                        break;
                    case RIGHT:
                        sql.append("RIGHT JOIN ");
                        break;
                    case FULL:
                        sql.append("FULL OUTER JOIN ");
                        break;
                }
                
                // Use qualified table name for right table if database is set
                if (ast.getDatabaseName() != null && !ast.getDatabaseName().isEmpty()) {
                    sql.append(ast.getDatabaseName()).append(".").append(join.getRightTable());
                } else {
                    sql.append(join.getRightTable());
                }
                
                sql.append(" ON ")
                   .append(join.getLeftTable()).append(".").append(join.getLeftColumn())
                   .append(" = ")
                   .append(join.getRightTable()).append(".").append(join.getRightColumn());
            }
        }

        // WHERE clause
        if (!ast.getWhereConditions().isEmpty()) {
            sql.append(" WHERE ");
            sql.append(ast.getWhereConditions().stream()
                .map(this::buildCondition)
                .collect(Collectors.joining(" AND ")));
        }

        // ORDER BY clause
        if (ast.getOrderByColumn() != null) {
            sql.append(" ORDER BY ").append(ast.getOrderByColumn());
            if (ast.getOrderDirection() != null) {
                sql.append(" ").append(ast.getOrderDirection().name());
            }
        }

        // LIMIT clause
        if (ast.getLimit() != null) {
            sql.append(" LIMIT ").append(ast.getLimit());
        }

        return sql.toString();
    }

    private String buildCondition(ConditionNode condition) {
        StringBuilder sb = new StringBuilder();
        sb.append(condition.getColumn())
          .append(" ")
          .append(condition.getOperator())
          .append(" ");

        // Handle value formatting
        Object value = condition.getValue();
        if (value instanceof String) {
            sb.append("'").append(value).append("'");
        } else if (value instanceof Number) {
            sb.append(value);
        } else {
            sb.append("'").append(value).append("'");
        }

        return sb.toString();
    }

    public String generateMongoQuery(QueryAST ast) {
        if (ast.getTargetTable() == null) {
            throw new IllegalArgumentException("Target collection cannot be null");
        }

        StringBuilder mongoQuery = new StringBuilder();
        mongoQuery.append("db.").append(ast.getTargetTable());

        switch (ast.getIntent()) {
            case SELECT:
                mongoQuery.append(".find(");
                // Build filter
                if (!ast.getWhereConditions().isEmpty()) {
                    mongoQuery.append(buildMongoFilter(ast.getWhereConditions()));
                } else {
                    mongoQuery.append("{}");
                }
                mongoQuery.append(")");
                
                // Projection
                if (!ast.getSelectColumns().isEmpty() && !ast.getSelectColumns().contains("*")) {
                    mongoQuery.append(".projection({");
                    for (int i = 0; i < ast.getSelectColumns().size(); i++) {
                        if (i > 0) mongoQuery.append(", ");
                        mongoQuery.append("\"").append(ast.getSelectColumns().get(i)).append("\": 1");
                    }
                    mongoQuery.append("})");
                }
                break;

            case COUNT:
                mongoQuery.append(".countDocuments(");
                if (!ast.getWhereConditions().isEmpty()) {
                    mongoQuery.append(buildMongoFilter(ast.getWhereConditions()));
                } else {
                    mongoQuery.append("{}");
                }
                mongoQuery.append(")");
                break;

            case SUM:
            case AVG:
            case MAX:
            case MIN:
                mongoQuery.append(".aggregate([");
                
                // Match stage
                if (!ast.getWhereConditions().isEmpty()) {
                    mongoQuery.append("{$match: ").append(buildMongoFilter(ast.getWhereConditions())).append("}, ");
                }
                
                // Group stage
                mongoQuery.append("{$group: {_id: null, result: {");
                String aggOp = ast.getIntent().name().toLowerCase();
                mongoQuery.append("$").append(aggOp).append(": ");
                
                if (ast.getAggregateColumn() != null) {
                    mongoQuery.append("\"$").append(ast.getAggregateColumn()).append("\"");
                } else {
                    mongoQuery.append("1");
                }
                
                mongoQuery.append("}}}])");
                break;

            default:
                mongoQuery.append(".find({})");
        }

        if (ast.getLimit() != null) {
            mongoQuery.append(".limit(").append(ast.getLimit()).append(")");
        }

        return mongoQuery.toString();
    }

    private String buildMongoFilter(List<ConditionNode> conditions) {
        StringBuilder filter = new StringBuilder("{");
        
        for (int i = 0; i < conditions.size(); i++) {
            if (i > 0) filter.append(", ");
            
            ConditionNode condition = conditions.get(i);
            filter.append("\"").append(condition.getColumn()).append("\": ");
            
            // Handle operators
            switch (condition.getOperator()) {
                case "=":
                    if (condition.getValue() instanceof String) {
                        filter.append("\"").append(condition.getValue()).append("\"");
                    } else {
                        filter.append(condition.getValue());
                    }
                    break;
                case ">":
                    filter.append("{$gt: ").append(formatMongoValue(condition.getValue())).append("}");
                    break;
                case "<":
                    filter.append("{$lt: ").append(formatMongoValue(condition.getValue())).append("}");
                    break;
                case ">=":
                    filter.append("{$gte: ").append(formatMongoValue(condition.getValue())).append("}");
                    break;
                case "<=":
                    filter.append("{$lte: ").append(formatMongoValue(condition.getValue())).append("}");
                    break;
                case "!=":
                    filter.append("{$ne: ").append(formatMongoValue(condition.getValue())).append("}");
                    break;
                default:
                    filter.append("\"").append(condition.getValue()).append("\"");
            }
        }
        
        filter.append("}");
        return filter.toString();
    }

    private String formatMongoValue(Object value) {
        if (value instanceof String) {
            return "\"" + value + "\"";
        }
        return String.valueOf(value);
    }
}
