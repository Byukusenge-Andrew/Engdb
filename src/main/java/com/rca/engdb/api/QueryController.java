package com.rca.engdb.api;

import com.rca.engdb.ml.IntentClassifier;
import com.rca.engdb.ml.IntentResult;
import com.rca.engdb.dto.QueryRequest;
import com.rca.engdb.dto.QueryResponse;
import com.rca.engdb.nlp.TokenizerService;
import com.rca.engdb.nlp.PreprocessService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/query")
public class QueryController {

    private final TokenizerService tokenizer;
    private final PreprocessService preprocessor;
    private final IntentClassifier intentClassifier;

    public QueryController(
            TokenizerService tokenizer,
            PreprocessService preprocessor,
            IntentClassifier intentClassifier) {

        this.tokenizer = tokenizer;
        this.preprocessor = preprocessor;
        this.intentClassifier = intentClassifier;
    }

    @PostMapping
    public QueryResponse handleQuery(@RequestBody QueryRequest request) {

        var tokens = tokenizer.tokenize(request.getQuery());
        var cleaned = preprocessor.clean(tokens);

        var intentResult = intentClassifier.classify(cleaned);

        return new QueryResponse(
            intentResult.getIntent().name(),
            cleaned,
            intentResult.getConfidence()
        );
    }
}
