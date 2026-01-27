package com.rca.engdb.engine;

import com.rca.engdb.ast.ConditionNode;
import com.rca.engdb.ast.JoinNode;
import com.rca.engdb.ast.QueryAST;
import com.rca.engdb.ml.IntentType;
import org.springframework.stereotype.Service;

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
        sql.append(" FROM ").append(ast.getTargetTable());

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
                sql.append(join.getRightTable())
                   .append(" ON ")
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
        // TODO: Implement MongoDB query generation
        // For now, return a placeholder
        return "{ \"collection\": \"" + ast.getTargetTable() + "\" }";
    }
}
