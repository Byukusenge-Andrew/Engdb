# Engdb

Natural Language to Any Database Query Engine.

## Technologies

- Java 21
- Spring Boot 4.0.1
- Maven
- MySQL
- MongoDB

## How to Run

1. **Clone the repository:**
   ```bash
   git clone <repository-url>
   ```
2. **Navigate to the project directory:**
   ```bash
   cd engdb
   ```
3. **Run the application using Maven:**
   ```bash
   ./mvnw spring-boot:run
   ```
   The application will be available at `http://localhost:8080`.

## API Endpoints

### POST /api/query

This endpoint takes a natural language query and returns the classified intent.

**Request Body:**

```json
{
  "query": "your natural language query"
}
```

**Response Body:**

```json
{
  "intent": "CLASSIFIED_INTENT",
  "tokens": [
    "token1",
    "token2"
  ],
  "confidence": 0.95
}
```
