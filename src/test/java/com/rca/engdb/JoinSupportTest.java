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
        entityRecognizer = new EntityRecognizer(schemaRegistry);
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

        QueryAST ast = queryParser.parse(tokens, intent);
        
        assertNotNull(ast);
        assertFalse(ast.getJoins().isEmpty(), "Should have JOIN nodes");
    }

    @Test
    void testJoinSQLGeneration() {
        // Manually create AST with JOIN
        QueryAST ast = new QueryAST();
        ast.setIntent(IntentType.SELECT);
        ast.setTargetTable("students");
        ast.setSelectColumns(List.of("*"));
        
        JoinNode join = new JoinNode();
        join.setJoinType(JoinNode.JoinType.INNER);
        join.setLeftTable("students");
        join.setRightTable("enrollments");
        join.setLeftColumn("id");
        join.setRightColumn("student_id");
        
        ast.getJoins().add(join);
        
        String sql = queryGenerator.generateSQL(ast);
        
        assertTrue(sql.contains("INNER JOIN"));
        assertTrue(sql.contains("enrollments"));
        assertTrue(sql.contains("students.id = enrollments.student_id"));
    }

    @Test
    void testMultipleJoins() {
        // Create AST with multiple JOINs (students -> enrollments -> courses)
        QueryAST ast = new QueryAST();
        ast.setIntent(IntentType.SELECT);
        ast.setTargetTable("students");
        ast.setSelectColumns(List.of("students.name", "courses.name"));
        
        // First JOIN: students -> enrollments
        JoinNode join1 = new JoinNode();
        join1.setJoinType(JoinNode.JoinType.INNER);
        join1.setLeftTable("students");
        join1.setRightTable("enrollments");
        join1.setLeftColumn("id");
        join1.setRightColumn("student_id");
        ast.getJoins().add(join1);
        
        // Second JOIN: enrollments -> courses
        JoinNode join2 = new JoinNode();
        join2.setJoinType(JoinNode.JoinType.INNER);
        join2.setLeftTable("enrollments");
        join2.setRightTable("courses");
        join2.setLeftColumn("course_id");
        join2.setRightColumn("id");
        ast.getJoins().add(join2);
        
        String sql = queryGenerator.generateSQL(ast);
        
        assertTrue(sql.contains("INNER JOIN enrollments"));
        assertTrue(sql.contains("INNER JOIN courses"));
    }
}
