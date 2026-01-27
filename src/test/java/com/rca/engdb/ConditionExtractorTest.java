package com.rca.engdb;

import com.rca.engdb.nlp.ConditionExtractor;
import com.rca.engdb.ast.ConditionNode;
import com.rca.engdb.schema.SchemaRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

class ConditionExtractorTest {

    private ConditionExtractor conditionExtractor;
    private SchemaRegistry schemaRegistry;

    @BeforeEach
    void setUp() {
        schemaRegistry = Mockito.mock(SchemaRegistry.class);
        when(schemaRegistry.getSchema()).thenReturn(Map.of(
            "students", List.of("id", "name", "age", "department"),
            "courses", List.of("id", "name", "credits")
        ));
        
        conditionExtractor = new ConditionExtractor(schemaRegistry);
    }

    @Test
    void testImplicitEquality() {
        List<String> tokens = List.of("students", "department", "CS");
        List<ConditionNode> conditions = conditionExtractor.extractConditions(tokens, "students");
        
        assertEquals(1, conditions.size());
        assertEquals("department", conditions.get(0).getColumn());
        assertEquals("=", conditions.get(0).getOperator());
        assertEquals("CS", conditions.get(0).getValue());
    }

    @Test
    void testExplicitOperator() {
        List<String> tokens = List.of("age", "is", "20");
        List<ConditionNode> conditions = conditionExtractor.extractConditions(tokens, "students");
        
        assertEquals(1, conditions.size());
        assertEquals("age", conditions.get(0).getColumn());
        assertEquals("20", conditions.get(0).getValue());
    }

    @Test
    void testStopWordFiltering() {
        // "of" should be filtered out
        List<String> tokens = List.of("age", "of", "students");
        List<ConditionNode> conditions = conditionExtractor.extractConditions(tokens, "students");
        
        // Should not create a condition with "of" as value
        assertEquals(0, conditions.size());
    }

    @Test
    void testMultipleConditions() {
        List<String> tokens = List.of("department", "CS", "age", "20");
        List<ConditionNode> conditions = conditionExtractor.extractConditions(tokens, "students");
        
        assertEquals(2, conditions.size());
    }

    @Test
    void testNoConditions() {
        List<String> tokens = List.of("show", "all", "students");
        List<ConditionNode> conditions = conditionExtractor.extractConditions(tokens, "students");
        
        assertEquals(0, conditions.size());
    }

    @Test
    void testNullTable() {
        List<String> tokens = List.of("department", "CS");
        List<ConditionNode> conditions = conditionExtractor.extractConditions(tokens, null);
        
        assertEquals(0, conditions.size());
    }
}
