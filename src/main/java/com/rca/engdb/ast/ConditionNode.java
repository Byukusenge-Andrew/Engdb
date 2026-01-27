package com.rca.engdb.ast;

public class ConditionNode {
    
    private String column;
    private String operator;  // =, >, <, >=, <=, !=, LIKE, IN
    private Object value;
    private LogicalOperator logicalOperator;  // AND, OR (for chaining conditions)

    public ConditionNode(String column, String operator, Object value) {
        this.column = column;
        this.operator = operator;
        this.value = value;
        this.logicalOperator = LogicalOperator.AND;  // Default
    }

    public ConditionNode(String column, String operator, Object value, LogicalOperator logicalOperator) {
        this.column = column;
        this.operator = operator;
        this.value = value;
        this.logicalOperator = logicalOperator;
    }

    public String getColumn() {
        return column;
    }

    public void setColumn(String column) {
        this.column = column;
    }

    public String getOperator() {
        return operator;
    }

    public void setOperator(String operator) {
        this.operator = operator;
    }

    public Object getValue() {
        return value;
    }

    public void setValue(Object value) {
        this.value = value;
    }

    public LogicalOperator getLogicalOperator() {
        return logicalOperator;
    }

    public void setLogicalOperator(LogicalOperator logicalOperator) {
        this.logicalOperator = logicalOperator;
    }

    public enum LogicalOperator {
        AND,
        OR
    }
}
