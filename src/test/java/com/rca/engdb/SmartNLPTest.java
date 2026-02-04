package com.rca.engdb;

import com.rca.engdb.nlp.EntityRecognizer;
import com.rca.engdb.nlp.SynonymRegistry;
import com.rca.engdb.schema.SchemaDiscoveryService;
import com.rca.engdb.schema.SchemaRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

class SmartNLPTest {

    private EntityRecognizer entityRecognizer;
    private SchemaRegistry schemaRegistry;
    private SynonymRegistry synonymRegistry;

    @BeforeEach
    void setUp() {
        schemaRegistry = Mockito.mock(SchemaRegistry.class);
        synonymRegistry = new SynonymRegistry();
        
        // Setup mock schema
        Map<String, List<String>> mockSchema = Map.of(
            "students", List.of("id", "name", "age", "grade"),
            "courses", List.of("id", "title", "credits")
        );
        when(schemaRegistry.getSchema(any())).thenReturn(mockSchema);

        entityRecognizer = new EntityRecognizer(schemaRegistry, synonymRegistry);
    }

    @Test
    void testFuzzyMatchingTable() {
        // "studnts" -> "students"
        List<String> tokens = List.of("show", "studnts");
        var result = entityRecognizer.recognize(tokens);
        
        assertEquals("students", result.getTable(), "Should fuzzy match 'studnts' to 'students'");
    }

    @Test
    void testSynonymMatching() {
        // "pupils" -> "students"
        List<String> tokens = List.of("show", "pupils");
        var result = entityRecognizer.recognize(tokens);
        
        assertEquals("students", result.getTable(), "Should resolve synonym 'pupils' to 'students'");
    }
    
    @Test
    void testSynonymForTable2() {
        // "classes" -> "courses"
        List<String> tokens = List.of("show", "classes");
        var result = entityRecognizer.recognize(tokens);
        
        assertEquals("courses", result.getTable(), "Should resolve synonym 'classes' to 'courses'");
    }

    @Test
    void testFuzzyMatchingColumn() {
        // "crdits" -> "credits" (in courses table)
        List<String> tokens = List.of("courses", "crdits");
        var result = entityRecognizer.recognize(tokens);
        
        assertEquals("courses", result.getTable());
        assertTrue(result.getColumns().contains("credits"), "Should fuzzy match 'crdits' to 'credits'");
    }
}
