package com.rca.engdb.exec;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import org.bson.Document;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class MongoQueryExecutor {

    private final MongoClient mongoClient;

    public MongoQueryExecutor(MongoClient mongoClient) {
        this.mongoClient = mongoClient;
    }

    /**
     * Execute MongoDB query and return results
     * Note: This is a simplified implementation that executes basic find queries
     */
    public QueryExecutor.QueryResult executeMongoQuery(String collectionName, Document filter, Document projection) {
        long startTime = System.currentTimeMillis();
        
        try {
            MongoDatabase database = mongoClient.getDatabase("engdb");
            MongoCollection<Document> collection = database.getCollection(collectionName);
            
            List<Map<String, Object>> results = new ArrayList<>();
            
            // Execute find query
            for (Document doc : collection.find(filter).projection(projection)) {
                Map<String, Object> row = new HashMap<>();
                for (String key : doc.keySet()) {
                    row.put(key, doc.get(key));
                }
                results.add(row);
            }
            
            long executionTime = System.currentTimeMillis() - startTime;
            
            return new QueryExecutor.QueryResult(
                results,
                results.size(),
                executionTime,
                true,
                null
            );
            
        } catch (Exception e) {
            long executionTime = System.currentTimeMillis() - startTime;
            return new QueryExecutor.QueryResult(
                new ArrayList<>(),
                0,
                executionTime,
                false,
                e.getMessage()
            );
        }
    }

    /**
     * Execute count query
     */
    public QueryExecutor.QueryResult executeCountQuery(String collectionName, Document filter) {
        long startTime = System.currentTimeMillis();
        
        try {
            MongoDatabase database = mongoClient.getDatabase("engdb");
            MongoCollection<Document> collection = database.getCollection(collectionName);
            
            long count = collection.countDocuments(filter);
            
            List<Map<String, Object>> results = new ArrayList<>();
            Map<String, Object> result = new HashMap<>();
            result.put("count", count);
            results.add(result);
            
            long executionTime = System.currentTimeMillis() - startTime;
            
            return new QueryExecutor.QueryResult(
                results,
                1,
                executionTime,
                true,
                null
            );
            
        } catch (Exception e) {
            long executionTime = System.currentTimeMillis() - startTime;
            return new QueryExecutor.QueryResult(
                new ArrayList<>(),
                0,
                executionTime,
                false,
                e.getMessage()
            );
        }
    }
}
