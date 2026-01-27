package com.rca.engdb.schema;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Models database schema relationships as a graph
 * Supports finding JOIN paths between tables
 */
public class SchemaGraph {

    private final Map<String, List<ForeignKeyRelation>> adjacencyList;
    private final Map<String, Map<String, JoinPath>> pathCache;

    public SchemaGraph() {
        this.adjacencyList = new ConcurrentHashMap<>();
        this.pathCache = new ConcurrentHashMap<>();
    }

    /**
     * Add a foreign key relationship to the graph
     */
    public void addRelationship(String fromTable, String fromColumn, String toTable, String toColumn) {
        ForeignKeyRelation relation = new ForeignKeyRelation(fromTable, fromColumn, toTable, toColumn);
        
        adjacencyList.computeIfAbsent(fromTable, k -> new ArrayList<>()).add(relation);
        
        // Add reverse relationship for bidirectional traversal
        ForeignKeyRelation reverseRelation = new ForeignKeyRelation(toTable, toColumn, fromTable, fromColumn);
        adjacencyList.computeIfAbsent(toTable, k -> new ArrayList<>()).add(reverseRelation);
    }

    /**
     * Check if two tables are directly related
     */
    public boolean areDirectlyRelated(String table1, String table2) {
        List<ForeignKeyRelation> relations = adjacencyList.get(table1);
        if (relations == null) return false;
        
        return relations.stream()
            .anyMatch(r -> r.getToTable().equalsIgnoreCase(table2));
    }

    /**
     * Find the shortest JOIN path between two tables using BFS
     */
    public JoinPath findJoinPath(String fromTable, String toTable) {
        // Check cache first
        String cacheKey = fromTable + "->" + toTable;
        if (pathCache.containsKey(fromTable) && pathCache.get(fromTable).containsKey(toTable)) {
            return pathCache.get(fromTable).get(toTable);
        }

        if (fromTable.equalsIgnoreCase(toTable)) {
            return new JoinPath(); // Empty path for same table
        }

        // BFS to find shortest path
        Queue<String> queue = new LinkedList<>();
        Map<String, ForeignKeyRelation> parentMap = new HashMap<>();
        Set<String> visited = new HashSet<>();

        queue.offer(fromTable);
        visited.add(fromTable.toLowerCase());

        while (!queue.isEmpty()) {
            String currentTable = queue.poll();

            if (currentTable.equalsIgnoreCase(toTable)) {
                // Reconstruct path
                JoinPath path = reconstructPath(fromTable, toTable, parentMap);
                
                // Cache the result
                pathCache.computeIfAbsent(fromTable, k -> new HashMap<>()).put(toTable, path);
                
                return path;
            }

            List<ForeignKeyRelation> relations = adjacencyList.get(currentTable);
            if (relations != null) {
                for (ForeignKeyRelation relation : relations) {
                    String nextTable = relation.getToTable();
                    if (!visited.contains(nextTable.toLowerCase())) {
                        visited.add(nextTable.toLowerCase());
                        parentMap.put(nextTable, relation);
                        queue.offer(nextTable);
                    }
                }
            }
        }

        return null; // No path found
    }

    private JoinPath reconstructPath(String fromTable, String toTable, Map<String, ForeignKeyRelation> parentMap) {
        JoinPath path = new JoinPath();
        List<ForeignKeyRelation> relations = new ArrayList<>();

        String current = toTable;
        while (!current.equalsIgnoreCase(fromTable)) {
            ForeignKeyRelation relation = parentMap.get(current);
            if (relation == null) break;
            
            relations.add(0, relation); // Add to front
            current = relation.getFromTable();
        }

        for (ForeignKeyRelation relation : relations) {
            path.addRelation(relation);
        }

        return path;
    }

    /**
     * Get all tables in the graph
     */
    public Set<String> getAllTables() {
        return adjacencyList.keySet();
    }

    /**
     * Get all relationships for a table
     */
    public List<ForeignKeyRelation> getRelationships(String table) {
        return adjacencyList.getOrDefault(table, new ArrayList<>());
    }

    /**
     * Clear the path cache (useful when schema changes)
     */
    public void clearCache() {
        pathCache.clear();
    }
}
