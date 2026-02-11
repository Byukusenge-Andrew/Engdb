package com.rca.engdb.ml;
    

import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
public class IntentClassifier {

    private static final Map<IntentType, List<String>> KEYWORDS = Map.of(
        IntentType.COUNT, List.of("count", "many", "number", "total"),
        IntentType.SUM, List.of("sum", "total"),
        IntentType.AVG, List.of("average", "avg", "mean"),
        IntentType.MAX, List.of("max", "highest", "largest"),
        IntentType.MIN, List.of("min", "lowest", "smallest"),
        IntentType.SELECT, List.of("list", "show", "display", "find"),
        IntentType.SCHEMA, List.of("tables", "schema", "databases", "structure")
    );

    public IntentResult classify(List<String> tokens) {

        IntentType bestIntent = IntentType.UNKNOWN;
        int bestScore = 0;

        for (var entry : KEYWORDS.entrySet()) {
            int score = 0;
            for (String token : tokens) {
                if (entry.getValue().contains(token)) {
                    score++;
                }
            }

            if (score > bestScore) {
                bestScore = score;
                bestIntent = entry.getKey();
            }
        }

        double confidence = bestScore == 0 ? 0.3 : Math.min(1.0, 0.6 + bestScore * 0.2);

        return new IntentResult(bestIntent, confidence);
    }
}
