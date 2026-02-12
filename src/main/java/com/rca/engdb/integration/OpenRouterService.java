package com.rca.engdb.integration;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

@Service
public class OpenRouterService {

    private static final Logger logger = LoggerFactory.getLogger(OpenRouterService.class);

    @Value("${openrouter.api-key}")
    private String apiKey;

    @Value("${openrouter.model}")
    private String model;

    @Value("${openrouter.url:https://openrouter.ai/api/v1/chat/completions}")
    private String apiUrl;

    private final RestClient restClient;
    private final ObjectMapper objectMapper;

    public OpenRouterService(RestClient.Builder restClientBuilder, ObjectMapper objectMapper) {
        this.restClient = restClientBuilder.build();
        this.objectMapper = objectMapper;
    }

    public String generateSQL(String naturalLanguageQuery, Map<String, List<String>> schema) {
        if (apiKey == null || apiKey.isBlank() || "YOUR_KEY_HERE".equals(apiKey)) {
            logger.warn("OpenRouter API key is not set. Skipping LLM generation.");
            return null;
        }

        try {
            String schemaDescription = formatSchema(schema);
            String prompt = String.format("""
                You are a SQL expert. Convert the following natural language query into a valid SQL query for a MySQL database.
                
                Database Schema:
                %s
                
                5. Rules:
                - Return ONLY the SQL query. No markdown formatting, no explanations.
                - Use the provided schema names exactly.
                - IMPORTANT: You MUST qualify all table names with the database name if provided in the schema (e.g., `dbname.tablename`).
                - If the user asks for something not in the schema, try to make a best guess or return a SELECT * FROM first_table.
                
                User Query: %s
                """, schemaDescription, naturalLanguageQuery);

            logger.info("Sending request to OpenRouter model: {}", model);

            OpenRouterRequest request = new OpenRouterRequest(
                model,
                List.of(
                    new Message("system", "You are a helpful SQL assistant. Return only raw SQL code without markdown backticks."),
                    new Message("user", prompt)
                )
            );

            String requestBody = objectMapper.writeValueAsString(request);
            
            String responseBody = restClient.post()
                .uri(apiUrl)
                .header("Authorization", "Bearer " + apiKey)
                .header("Content-Type", "application/json")
                .header("HTTP-Referer", "http://localhost:8080") 
                .header("X-Title", "EngDB")
                .body(requestBody)
                .retrieve()
                .body(String.class);

            OpenRouterResponse response = objectMapper.readValue(responseBody, OpenRouterResponse.class);

            if (response != null && response.choices != null && !response.choices.isEmpty()) {
                String generatedSql = response.choices.get(0).message.content.trim();
                // cleanup just in case
                generatedSql = generatedSql.replaceAll("```sql", "").replaceAll("```", "").trim();
                
                // Fix common spacing issues
                generatedSql = generatedSql.replaceAll("(?i)SELECTCOUNT", "SELECT COUNT");
                generatedSql = generatedSql.replaceAll("(?i)SELECTSUM", "SELECT SUM");
                generatedSql = generatedSql.replaceAll("(?i)SELECTAVG", "SELECT AVG");
                generatedSql = generatedSql.replaceAll("(?i)SELECTMAX", "SELECT MAX");
                generatedSql = generatedSql.replaceAll("(?i)SELECTMIN", "SELECT MIN");
                logger.info("Generated SQL: {}", generatedSql);
                return generatedSql;
            }

        } catch (Exception e) {
            logger.error("Failed to call OpenRouter API", e);
        }

        return null;
    }

    private String formatSchema(Map<String, List<String>> schema) {
        StringBuilder sb = new StringBuilder();
        schema.forEach((table, columns) -> {
            sb.append("Table: ").append(table).append("\n");
            sb.append("Columns: ").append(String.join(", ", columns)).append("\n\n");
        });
        return sb.toString();
    }

    // DTOs
    record OpenRouterRequest(String model, List<Message> messages) {}
    @JsonIgnoreProperties(ignoreUnknown = true)
    record Message(String role, String content) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    record OpenRouterResponse(List<Choice> choices) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    record Choice(Message message) {}
}
