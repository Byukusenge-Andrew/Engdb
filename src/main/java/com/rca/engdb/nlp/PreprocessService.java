package com.rca.engdb.nlp;


import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class PreprocessService {

    private static final Set<String> STOP_WORDS = Set.of(
        "how", "what", "is", "are", "the", "a", "an", "of", "to", "in", "on"
    );

    public List<String> clean(List<String> tokens) {
        return tokens.stream()
                .filter(t -> !STOP_WORDS.contains(t))
                .map(this::simpleLemma)
                .collect(Collectors.toList());
    }

    // VERY simple lemmatization (we upgrade later)
    private String simpleLemma(String word) {
        if (word.endsWith("ing")) return word.substring(0, word.length() - 3);
        if (word.endsWith("ed")) return word.substring(0, word.length() - 2);
        if (word.endsWith("s")) return word.substring(0, word.length() - 1);
        return word;
    }
}
