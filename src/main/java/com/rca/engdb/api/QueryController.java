

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

    public QueryController(
            TokenizerService tokenizer,
            PreprocessService preprocessor,
            IntentClassifier intentClassifier,
            QueryParser queryParser,
            QueryGenerator queryGenerator,
            QueryExecutor queryExecutor,
            DatabaseDiscoveryService databaseDiscoveryService,
            SchemaDiscoveryService schemaDiscoveryService) {

        this.tokenizer = tokenizer;
        this.preprocessor = preprocessor;
        this.intentClassifier = intentClassifier;
        this.queryParser = queryParser;
        this.queryGenerator = queryGenerator;
        this.queryExecutor = queryExecutor;
        this.databaseDiscoveryService = databaseDiscoveryService;
        this.schemaDiscoveryService = schemaDiscoveryService;
    }
    
    @GetMapping("/databases")
    public List<String> getDatabases() {
        return databaseDiscoveryService.getAllDatabases();
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
        try {
            QueryAST ast = queryParser.parse(cleaned, intentResult, dbName);
            
            // Set database context in AST for generation
            if (dbName != null && !dbName.isEmpty()) {
                ast.setDatabaseName(dbName);
            }

            // 3. Choose database type
            QueryPlanner planner = new QueryPlanner();
            QueryPlanner.DatabaseType dbType = planner.chooseDatabaseType(ast);

            String generatedQuery;
            QueryExecutor.QueryResult result;

            if (dbType == QueryPlanner.DatabaseType.MONGODB) {
                // MongoDB execution
                generatedQuery = queryGenerator.generateMongoQuery(ast);
                // Note: Actual MongoDB execution would require parsing the generated query
                // For now, we'll fall back to SQL
                String sql = queryGenerator.generateSQL(ast);
                result = queryExecutor.executeSQLQuery(sql);
                generatedQuery = sql + " (MongoDB: " + generatedQuery + ")";
            } else {
                // MySQL execution
                generatedQuery = queryGenerator.generateSQL(ast);
                result = queryExecutor.executeSQLQuery(generatedQuery);
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
        }
    }
}
