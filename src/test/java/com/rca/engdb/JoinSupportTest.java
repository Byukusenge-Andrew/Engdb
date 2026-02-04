package com.rca.engdb;

import com.rca.engdb.ast.JoinNode;
import com.rca.engdb.ast.QueryAST;
import com.rca.engdb.engine.QueryGenerator;
import com.rca.engdb.engine.QueryParser;
import com.rca.engdb.ml.IntentResult;
import com.rca.engdb.ml.IntentType;
import com.rca.engdb.nlp.ConditionExtractor;
import com.rca.engdb.nlp.EntityRecognizer;
import com.rca.engdb.nlp.JoinDetector;
import com.rca.engdb.nlp.SynonymRegistry;
import com.rca.engdb.schema.SchemaDiscoveryService;
import com.rca.engdb.schema.SchemaGraph;
import com.rca.engdb.schema.SchemaRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

class JoinSupportTest {

    private SchemaRegistry schemaRegistry;
    private SchemaDiscoveryService schemaDiscoveryService;
    private EntityRecognizer entityRecognizer;
    private ConditionExtractor conditionExtractor;
    private SynonymRegistry synonymRegistry;
    private JoinDetector joinDetector;
    private QueryParser queryParser;
    private QueryGenerator queryGenerator;
    private SchemaGraph schemaGraph;

    @BeforeEach
    void setUp() {
        // Mock Schema
        schemaRegistry = Mockito.mock(SchemaRegistry.class);
        when(schemaRegistry.getSchema()).thenReturn(Map.of(
            "students", List.of("id", "name", "age", "department"),
            "courses", List.of("id", "name", "credits"),
            "enrollments", List.of("id", "student_id", "course_id", "grade")
        ));

        // Create SchemaGraph with relationships
        schemaGraph = new SchemaGraph();
        schemaGraph.addRelationship("enrollments", "student_id", "students", "id");
        schemaGraph.addRelationship("enrollments", "course_id", "courses", "id");

        // Mock SchemaDiscoveryService
        schemaDiscoveryService = Mockito.mock(SchemaDiscoveryService.class);
        when(schemaDiscoveryService.getSchemaGraph()).thenReturn(schemaGraph);

        // Initialize components
        synonymRegistry = new SynonymRegistry();
        entityRecognizer = new EntityRecognizer(schemaRegistry, synonymRegistry);
        conditionExtractor = new ConditionExtractor(schemaRegistry);
        joinDetector = new JoinDetector(schemaRegistry);
        queryParser = new QueryParser(entityRecognizer, conditionExtractor, joinDetector, schemaDiscoveryService);
        queryGenerator = new QueryGenerator();
    }

    @Test
    void testJoinDetection_TwoTables() {
        // "Show students and courses"
        List<String> tokens = List.of("show", "students", "and", "courses");
        
        JoinDetector.JoinDetectionResult result = joinDetector.detectJoins(tokens);
        
        assertTrue(result.requiresJoin());
        assertTrue(result.getDetectedTables().contains("students"));
        assertTrue(result.getDetectedTables().contains("courses"));
        assertTrue(result.hasJoinKeywords());
    }

    @Test
    void testJoinPathFinding() {
        // Test finding path from students to courses
        var joinPath = schemaGraph.findJoinPath("students", "courses");
        
        assertNotNull(joinPath);
        assertEquals(2, joinPath.length()); // students -> enrollments -> courses
    }

    @Test
    void testQueryParserBuildsJoins() {
        // "Show students with their courses"
        List<String> tokens = List.of("show", "students", "with", "their", "courses");
        IntentResult intent = new IntentResult(IntentType.SELECT, 0.9);

        QueryAST ast = queryParser.parse(tokens, intent, null);
        
        assertNotNull(ast);
        assertFalse(ast.getJoins().isEmpty(), "Should have JOIN nodes");
    }

    @Test
    void testAvoidDuplicateJoins() {
        // Simulate detection of [students, enrollments, courses]
        // Should find path students->enrollments and students->enrollments->courses
        // But shouldn't add students->enrollments twice
        
        List<String> tokens = List.of("students", "enrollments", "courses");
        IntentResult intent = new IntentResult(IntentType.SELECT, 1.0);
        
        // We rely on parse method which calls buildJoinNodes
        QueryAST ast = queryParser.parse(tokens, intent, null);
        
        long enrollmentJoins = ast.getJoins().stream()
            .filter(j -> j.getRightTable().equals("enrollments") && j.getLeftTable().equals("students"))
            .count();
            
        assertEquals(1, enrollmentJoins, "Should only have one JOIN between students and enrollments");
        
        assertEquals(2, ast.getJoins().size(), "Should have exactly 2 joins (students->enrollments, enrollments->courses)");
    }
}
