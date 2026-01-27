package com.rca.engdb;

import com.rca.engdb.ast.ConditionNode;
import com.rca.engdb.ast.QueryAST;
import com.rca.engdb.engine.QueryGenerator;
import com.rca.engdb.engine.QueryParser;
import com.rca.engdb.ml.IntentResult;
import com.rca.engdb.ml.IntentType;
import com.rca.engdb.nlp.ConditionExtractor;
import com.rca.engdb.nlp.EntityRecognizer;
import com.rca.engdb.schema.SchemaDiscoveryService;
import com.rca.engdb.schema.SchemaRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.when;

class QueryPipelineTest {

    private SchemaRegistry schemaRegistry;
    private EntityRecognizer entityRecognizer;
    private ConditionExtractor conditionExtractor;
    private QueryParser queryParser;
    private QueryGenerator queryGenerator;

    @BeforeEach
    void setUp() {
        // Mock Schema Discovery
        schemaRegistry = Mockito.mock(SchemaRegistry.class);
        when(schemaRegistry.getSchema()).thenReturn(Map.of(
            "students", List.of("id", "name", "age", "department", "grade"),
            "courses", List.of("id", "name", "credits")
        ));

        // Initialize Components
        entityRecognizer = new EntityRecognizer(schemaRegistry);
        conditionExtractor = new ConditionExtractor(schemaRegistry);
        queryParser = new QueryParser(entityRecognizer, conditionExtractor);
        queryGenerator = new QueryGenerator();
    }

    @Test
    void testSimpleSelect() {
        // "Show me all students"
        List<String> tokens = List.of("show", "me", "all", "students");
        IntentResult intent = new IntentResult(IntentType.SELECT, 0.9);

        QueryAST ast = queryParser.parse(tokens, intent);
        String sql = queryGenerator.generateSQL(ast);

        assertEquals("students", ast.getTargetTable());
        assertEquals("SELECT * FROM students", sql.trim());
    }

    @Test
    void testWhereClauseExtraction() {
        // "Show students in CS department"
        List<String> tokens = List.of("show", "students", "in", "cs", "department");
        IntentResult intent = new IntentResult(IntentType.SELECT, 0.9);

        QueryAST ast = queryParser.parse(tokens, intent);
        String sql = queryGenerator.generateSQL(ast);

        assertEquals("students", ast.getTargetTable());
        assertEquals(1, ast.getWhereConditions().size());
        
        ConditionNode condition = ast.getWhereConditions().get(0);
        assertEquals("department", condition.getColumn());
        assertEquals("=", condition.getOperator());
        assertEquals("cs", condition.getValue());

        assertEquals("SELECT * FROM students WHERE department = 'cs'", sql.trim());
    }

    @Test
    void testAggregationCount() {
        // "How many students are there"
        List<String> tokens = List.of("how", "many", "students", "are", "there");
        IntentResult intent = new IntentResult(IntentType.COUNT, 0.9);

        QueryAST ast = queryParser.parse(tokens, intent);
        String sql = queryGenerator.generateSQL(ast);

        assertEquals("SELECT COUNT(*) FROM students", sql.trim());
    }

    @Test
    void testAggregationAverage() {
        // "Average age of students"
        List<String> tokens = List.of("average", "age", "of", "students");
        IntentResult intent = new IntentResult(IntentType.AVG, 0.9);

        QueryAST ast = queryParser.parse(tokens, intent);
        String sql = queryGenerator.generateSQL(ast);

        assertEquals("age", ast.getAggregateColumn());
        assertEquals("SELECT AVG(age) FROM students", sql.trim());
    }

    @Test
    void testImplicitEquality() {
        // "students department CS"
        List<String> tokens = List.of("students", "department", "CS");
        IntentResult intent = new IntentResult(IntentType.SELECT, 0.8);

        QueryAST ast = queryParser.parse(tokens, intent);
        String sql = queryGenerator.generateSQL(ast);
        
        assertEquals("SELECT * FROM students WHERE department = 'CS'", sql.trim());
    }
}
