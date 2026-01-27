

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

    public QueryController(
            TokenizerService tokenizer,
            PreprocessService preprocessor,
            IntentClassifier intentClassifier,
            QueryParser queryParser,
            QueryGenerator queryGenerator,
            QueryExecutor queryExecutor) {

        this.tokenizer = tokenizer;
        this.preprocessor = preprocessor;
        this.intentClassifier = intentClassifier;
        this.queryParser = queryParser;
        this.queryGenerator = queryGenerator;
        this.queryExecutor = queryExecutor;
    }

    @PostMapping
    public QueryResponse handleQuery(@RequestBody QueryRequest request) {

        // 1. NLP Pipeline
        var tokens = tokenizer.tokenize(request.getQuery());
        var cleaned = preprocessor.clean(tokens);
        var intentResult = intentClassifier.classify(cleaned);

        // 2. Query Parsing & Generation
        try {
            QueryAST ast = queryParser.parse(tokens, intentResult); // Use original tokens for entity recognition context if needed, or cleaned
            // For now using tokens as EntityRecognizer might need uncleaned ones? 
            // Actually EntityRecognizer uses tokens. Let's pass 'cleaned' if EntityRecognizer expects lemmas, 
            // or 'tokens' if it expects exact words. 
            // Looking at EntityRecognizer, it does fuzzy match. 'cleaned' is safer for now.
            // But wait, parse signature in QueryParser takes `List<String> tokens`.
            
            // Re-parsing with cleaned tokens for better matching
             ast = queryParser.parse(cleaned, intentResult);

            // 3. Generation
            String sql = queryGenerator.generateSQL(ast);

            // 4. Execution
            var result = queryExecutor.executeSQLQuery(sql);

            return new QueryResponse(
                intentResult.getIntent().name(),
                sql,
                result.getData(),
                result.getRowCount(),
                intentResult.getConfidence(), // TODO: combine with entity confidence
                result.getExecutionTimeMs(),
                result.getErrorMessage()
            );

        } catch (Exception e) {
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
