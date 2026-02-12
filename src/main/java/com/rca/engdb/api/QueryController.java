

package com.rca.engdb.api;

import com.rca.engdb.dto.QueryRequest;
import com.rca.engdb.dto.QueryResponse;
import com.rca.engdb.engine.QueryGenerator;
import com.rca.engdb.engine.QueryParser;
import com.rca.engdb.engine.QueryPlanner;
import com.rca.engdb.exec.QueryExecutor;
import com.rca.engdb.ml.IntentClassifier;
import com.rca.engdb.ml.IntentResult;
import com.rca.engdb.nlp.PreprocessService;
import com.rca.engdb.nlp.TokenizerService;
import com.rca.engdb.ml.IntentType;
import com.rca.engdb.ast.QueryAST;
import com.rca.engdb.schema.DatabaseDiscoveryService;
import com.rca.engdb.schema.SchemaDiscoveryService;

import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.List;

@RestController
@RequestMapping("/api/query")
public class QueryController {

    private final TokenizerService tokenizer;
    private final PreprocessService preprocessor;
    private final IntentClassifier intentClassifier;
    private final QueryParser queryParser;
    private final QueryGenerator queryGenerator;
    private final QueryExecutor queryExecutor;
    private final DatabaseDiscoveryService databaseDiscoveryService;
    private final SchemaDiscoveryService schemaDiscoveryService;
    private final com.rca.engdb.integration.OpenRouterService openRouterService;

    public QueryController(
            TokenizerService tokenizer,
            PreprocessService preprocessor,
            IntentClassifier intentClassifier,
            QueryParser queryParser,
            QueryGenerator queryGenerator,
            QueryExecutor queryExecutor,
            DatabaseDiscoveryService databaseDiscoveryService,
            SchemaDiscoveryService schemaDiscoveryService,
            com.rca.engdb.integration.OpenRouterService openRouterService) {

        this.tokenizer = tokenizer;
        this.preprocessor = preprocessor;
        this.intentClassifier = intentClassifier;
        this.queryParser = queryParser;
        this.queryGenerator = queryGenerator;
        this.queryExecutor = queryExecutor;
        this.databaseDiscoveryService = databaseDiscoveryService;
        this.schemaDiscoveryService = schemaDiscoveryService;
        this.openRouterService = openRouterService;
    }
    
    @GetMapping("/databases")
    public List<String> getDatabases(@RequestParam(defaultValue = "mysql") String serviceType) {
        return databaseDiscoveryService.getAllDatabases(serviceType);
    }
    
    @GetMapping("/schema")
    public java.util.Map<String, List<String>> getSchema(@RequestParam(required = false) String dbName) {
        return schemaDiscoveryService.discoverSchema(dbName); 
    }

    @PostMapping
    public QueryResponse handleQuery(@RequestBody QueryRequest request) {

        // 1. NLP Pipeline
        var tokens = tokenizer.tokenize(request.getQuery());
        var cleaned = preprocessor.clean(tokens);
        var intentResult = intentClassifier.classify(cleaned);
        
        String dbName = request.getDatabaseName();

        // 2. Query Parsing & Generation
        String generatedQuery = null;
        QueryExecutor.QueryResult result = null;

        try {

            // Try OpenRouter first
            if (openRouterService != null) {
                // Get schema for context
                var schema = schemaDiscoveryService.discoverSchema(dbName);
                generatedQuery = openRouterService.generateSQL(request.getQuery(), schema);
            }

            if (generatedQuery != null) {
                // Execute generated SQL
                result = queryExecutor.executeSQLQuery(generatedQuery);
            } else {
                // Fallback to local rule-based engine
                QueryAST ast = queryParser.parse(cleaned, intentResult, dbName);
                
                // Set database context in AST for generation
                if (dbName != null && !dbName.isEmpty()) {
                    ast.setDatabaseName(dbName);
                }

                // Check if a target table was identified (unless it's a SCHEMA intent)
                if (ast.getTargetTable() == null && intentResult.getIntent() != IntentType.SCHEMA) {
                    return new QueryResponse(
                        intentResult.getIntent().name(),
                        "Could not identify a clear query target (table). Please include a valid table name in your question.",
                        Collections.emptyList(),
                        0,
                        intentResult.getConfidence(),
                        0,
                        "No target table identified in query"
                    );
                }

                // Choose database type
                QueryPlanner planner = new QueryPlanner();
                QueryPlanner.DatabaseType dbType = planner.chooseDatabaseType(ast);

                if (dbType == QueryPlanner.DatabaseType.MONGODB) {
                    // MongoDB execution
                    generatedQuery = queryGenerator.generateMongoQuery(ast);
                    String sql = queryGenerator.generateSQL(ast);
                    result = queryExecutor.executeSQLQuery(sql);
                    generatedQuery = sql + " (MongoDB: " + generatedQuery + ")";
                } else {
                    // MySQL execution
                    generatedQuery = queryGenerator.generateSQL(ast);
                    result = queryExecutor.executeSQLQuery(generatedQuery);
                }
            }

            return new QueryResponse(
                intentResult.getIntent().name(),
                generatedQuery,
                result.getData(),
                result.getRowCount(),
                intentResult.getConfidence(),
                result.getExecutionTimeMs(),
                result.getErrorMessage()
            );

        } catch (Exception e) {
            e.printStackTrace();
            // Fallback for failed parsing/generation
            return new QueryResponse(
                intentResult.getIntent().name(),
                "ERROR: " + e.getMessage(),
                Collections.emptyList(),
                0,
                intentResult.getConfidence(),
                0,
                e.getMessage()
            );
        } finally {
             // Log the result size for debugging
             if (result != null) {
                 System.out.println("Query executed. Rows returned: " + result.getRowCount());
                 if (result.getData() != null && !result.getData().isEmpty()) {
                     System.out.println("First row: " + result.getData().get(0));
                 }
             } else {
                 System.out.println("Query execution failed or returned null result.");
             }
        }
    }
}