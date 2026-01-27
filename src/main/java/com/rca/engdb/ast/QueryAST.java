package com.rca.engdb.ast;

import com.rca.engdb.ml.IntentType;

import java.util.ArrayList;
import java.util.List;

public class QueryAST {
    
    private IntentType intent;
    private String targetTable;
    private List<String> selectColumns;
    private List<ConditionNode> whereConditions;
    private String aggregateColumn;
    private List<JoinNode> joins;
    private Integer limit;
    private String orderByColumn;
    private OrderDirection orderDirection;

    public QueryAST() {
        this.selectColumns = new ArrayList<>();
        this.whereConditions = new ArrayList<>();
        this.joins = new ArrayList<>();
    }

    public IntentType getIntent() {
        return intent;
    }

    public void setIntent(IntentType intent) {
        this.intent = intent;
    }

    public String getTargetTable() {
        return targetTable;
    }

    public void setTargetTable(String targetTable) {
        this.targetTable = targetTable;
    }

    public List<String> getSelectColumns() {
        return selectColumns;
    }

    public void setSelectColumns(List<String> selectColumns) {
        this.selectColumns = selectColumns;
    }

    public List<ConditionNode> getWhereConditions() {
        return whereConditions;
    }

    public void setWhereConditions(List<ConditionNode> whereConditions) {
        this.whereConditions = whereConditions;
    }

    public String getAggregateColumn() {
        return aggregateColumn;
    }

    public void setAggregateColumn(String aggregateColumn) {
        this.aggregateColumn = aggregateColumn;
    }

    public List<JoinNode> getJoins() {
        return joins;
    }

    public void setJoins(List<JoinNode> joins) {
        this.joins = joins;
    }

    public Integer getLimit() {
        return limit;
    }

    public void setLimit(Integer limit) {
        this.limit = limit;
    }

    public String getOrderByColumn() {
        return orderByColumn;
    }

    public void setOrderByColumn(String orderByColumn) {
        this.orderByColumn = orderByColumn;
    }

    public OrderDirection getOrderDirection() {
        return orderDirection;
    }

    public void setOrderDirection(OrderDirection orderDirection) {
        this.orderDirection = orderDirection;
    }

    public enum OrderDirection {
        ASC,
        DESC
    }
}
