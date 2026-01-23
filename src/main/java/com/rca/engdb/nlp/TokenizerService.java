package com.rca.engdb.nlp;



import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;

@Service
public class TokenizerService {

    public List<String> tokenize(String text) {
        return Arrays.asList(
            text
                .toLowerCase()
                .replaceAll("[^a-z0-9\\s]", "")
                .split("\\s+")
        );
    }
}
