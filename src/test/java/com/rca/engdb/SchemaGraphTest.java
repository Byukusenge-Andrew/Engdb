package com.rca.engdb;

import com.rca.engdb.schema.ForeignKeyRelation;
import com.rca.engdb.schema.JoinPath;
import com.rca.engdb.schema.SchemaGraph;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class SchemaGraphTest {

    private SchemaGraph schemaGraph;

    @BeforeEach
    void setUp() {
        schemaGraph = new SchemaGraph();
        
        // Build sample schema: students -> enrollments -> courses
        schemaGraph.addRelationship("enrollments", "student_id", "students", "id");
        schemaGraph.addRelationship("enrollments", "course_id", "courses", "id");
    }

    @Test
    void testDirectRelationship() {
        assertTrue(schemaGraph.areDirectlyRelated("enrollments", "students"));
        assertTrue(schemaGraph.areDirectlyRelated("enrollments", "courses"));
        assertFalse(schemaGraph.areDirectlyRelated("students", "courses"));
    }

    @Test
    void testFindJoinPath_DirectRelationship() {
        JoinPath path = schemaGraph.findJoinPath("enrollments", "students");
        
        assertNotNull(path);
        assertEquals(1, path.length());
        
        ForeignKeyRelation relation = path.getRelations().get(0);
        assertEquals("enrollments", relation.getFromTable());
        assertEquals("students", relation.getToTable());
    }

    @Test
    void testFindJoinPath_TwoHops() {
        JoinPath path = schemaGraph.findJoinPath("students", "courses");
        
        assertNotNull(path);
        assertEquals(2, path.length());
        
        // Path should be: students -> enrollments -> courses
        ForeignKeyRelation first = path.getRelations().get(0);
        ForeignKeyRelation second = path.getRelations().get(1);
        
        assertEquals("students", first.getFromTable());
        assertEquals("enrollments", first.getToTable());
        assertEquals("enrollments", second.getFromTable());
        assertEquals("courses", second.getToTable());
    }

    @Test
    void testFindJoinPath_SameTable() {
        JoinPath path = schemaGraph.findJoinPath("students", "students");
        
        assertNotNull(path);
        assertEquals(0, path.length());
    }

    @Test
    void testFindJoinPath_NoPath() {
        // Add disconnected table
        schemaGraph.addRelationship("departments", "head_id", "professors", "id");
        
        JoinPath path = schemaGraph.findJoinPath("students", "professors");
        
        assertNull(path);
    }

    @Test
    void testGetAllTables() {
        var tables = schemaGraph.getAllTables();
        
        assertTrue(tables.contains("students"));
        assertTrue(tables.contains("enrollments"));
        assertTrue(tables.contains("courses"));
        assertEquals(3, tables.size());
    }

    @Test
    void testCacheClear() {
        // Find a path to populate cache
        schemaGraph.findJoinPath("students", "courses");
        
        // Clear cache
        schemaGraph.clearCache();
        
        // Should still work after cache clear
        JoinPath path = schemaGraph.findJoinPath("students", "courses");
        assertNotNull(path);
        assertEquals(2, path.length());
    }
}
