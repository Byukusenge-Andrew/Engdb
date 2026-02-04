package com.rca.engdb.nlp;

import org.springframework.stereotype.Component;
import java.util.HashMap;
import java.util.Map;

@Component
public class SynonymRegistry {

    private final Map<String, String> synonyms = new HashMap<>();

    public SynonymRegistry() {
        // Initialize with some common defaults
        addSynonym("pupil", "student");
        addSynonym("pupils", "student");
        addSynonym("class", "course");
        addSynonym("classes", "course");
        addSynonym("lesson", "course");
        addSynonym("lessons", "course");
        addSynonym("lecturer", "instructor");
        addSynonym("teacher", "instructor");
        addSynonym("professor", "instructor");
        addSynonym("mark", "grade");
        addSynonym("marks", "grade");
        addSynonym("score", "grade");
        addSynonym("scores", "grade");
        addSynonym("cost", "price");
        addSynonym("pay", "salary");
        addSynonym("earnings", "salary");
        addSynonym("dept", "department");
    }

    public void addSynonym(String term, String target) {
        synonyms.put(term.toLowerCase(), target.toLowerCase());
    }

    public String resolve(String token) {
        return synonyms.getOrDefault(token.toLowerCase(), token);
    }
}
