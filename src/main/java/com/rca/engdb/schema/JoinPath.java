package com.rca.engdb.schema;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents a path of JOINs between tables
 */
public class JoinPath {
    private final List<ForeignKeyRelation> relations;

    public JoinPath() {
        this.relations = new ArrayList<>();
    }

    public void addRelation(ForeignKeyRelation relation) {
        relations.add(relation);
    }

    public List<ForeignKeyRelation> getRelations() {
        return relations;
    }

    public int length() {
        return relations.size();
    }
}
