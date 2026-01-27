package com.rca.engdb.schema;

/**
 * Represents a foreign key relationship between two tables
 */
public class ForeignKeyRelation {
    private final String fromTable;
    private final String fromColumn;
    private final String toTable;
    private final String toColumn;

    public ForeignKeyRelation(String fromTable, String fromColumn, String toTable, String toColumn) {
        this.fromTable = fromTable;
        this.fromColumn = fromColumn;
        this.toTable = toTable;
        this.toColumn = toColumn;
    }

    public String getFromTable() { return fromTable; }
    public String getFromColumn() { return fromColumn; }
    public String getToTable() { return toTable; }
    public String getToColumn() { return toColumn; }
}
