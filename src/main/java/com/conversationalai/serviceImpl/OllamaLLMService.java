package com.conversationalai.serviceImpl;

import com.conversationalai.config.ConversationalAIProperties;
import com.conversationalai.dto.*;
import com.conversationalai.service.LLMService;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
@Slf4j
@Service
public class OllamaLLMService implements LLMService {

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final ConversationalAIProperties.LLMConfig config;

    public OllamaLLMService(ConversationalAIProperties properties) {
        this.config = properties.getLlm();
        this.restTemplate = createOptimizedRestTemplate();
        this.objectMapper = new ObjectMapper();
    }

    private RestTemplate createOptimizedRestTemplate() {
        RestTemplate template = new RestTemplate();

        template.getMessageConverters().add(0, new StringHttpMessageConverter(StandardCharsets.UTF_8));
        return template;
    }

    @Override
    public ConversationalLLMResponse processConversationalRequest(ConversationalLLMRequest request) {
        try {
            String prompt = buildComprehensivePrompt(request);
            String rawResponse = generateResponse(prompt);
            return parseComprehensiveResponse(rawResponse, request);
        } catch (Exception e) {
            log.error("Error in comprehensive LLM processing", e);
            return ConversationalLLMResponse.builder()
                    .valid(false)
                    .errorMessage("Failed to process request: " + e.getMessage())
                    .build();
        }
    }

    private String buildComprehensivePrompt(ConversationalLLMRequest request) {
        StringBuilder prompt = new StringBuilder();

        prompt.append("You are a SQL database assistant. Process this user request and provide a complete response.\n\n");

        prompt.append("AVAILABLE TABLES:\n");
        request.getSchema().getTables().forEach((tableName, tableInfo) -> {
            prompt.append("Table: ").append(tableName).append("\n");
            prompt.append("Columns: ");
            tableInfo.getColumns().forEach((colName, colInfo) -> {
                prompt.append(colName).append("(").append(colInfo.getDataType()).append(")");
                if (!colInfo.isNullable()) prompt.append("[REQUIRED]");
                if (colInfo.isAutoIncrement()) prompt.append("[AUTO]");
                prompt.append(" ");
            });
            prompt.append("\n\n");
        });

        prompt.append("USER REQUEST: \"").append(request.getUserInput()).append("\"\n\n");

        prompt.append("RESPONSE FORMAT (JSON only, no explanations):\n");
        prompt.append("{\n");
        prompt.append("  \"status\": \"SUCCESS|ERROR|MISSING_INFO\",\n");
        prompt.append("  \"intent\": \"CREATE|READ|UPDATE|DELETE|LIST|COUNT\",\n");
        prompt.append("  \"tableName\": \"table_name\",\n");
        prompt.append("  \"sql\": \"SQL query with ? placeholders\",\n");
        prompt.append("  \"parameters\": [\"param1\", \"param2\"],\n");
        prompt.append("  \"humanResponse\": \"Friendly response to user\",\n");
        prompt.append("  \"missingFields\": [\"field1\", \"field2\"],\n");
        prompt.append("  \"errorMessage\": \"error description if any\"\n");
        prompt.append("}\n\n");

        prompt.append("RULES:\n");
        prompt.append("1. For partial matches use LIKE with % wildcards\n");
        prompt.append("2. For exact matches use = operator\n");
        prompt.append("3. String parameters in quotes, numbers without quotes\n");
        prompt.append("4. Provide helpful humanResponse for successful operations\n");
        prompt.append("5. If missing required fields, set status to MISSING_INFO\n\n");

        prompt.append("EXAMPLES:\n");
        prompt.append("User: \"find users like john\"\n");
        prompt.append("{\n");
        prompt.append("  \"status\": \"SUCCESS\",\n");
        prompt.append("  \"intent\": \"READ\",\n");
        prompt.append("  \"tableName\": \"users\",\n");
        prompt.append("  \"sql\": \"SELECT * FROM users WHERE name LIKE ?\",\n");
        prompt.append("  \"parameters\": [\"%john%\"],\n");
        prompt.append("  \"humanResponse\": \"I'll search for users with names containing 'john'.\"\n");
        prompt.append("}\n\n");

        prompt.append("User: \"create user named Alice\"\n");
        prompt.append("{\n");
        prompt.append("  \"status\": \"MISSING_INFO\",\n");
        prompt.append("  \"intent\": \"CREATE\",\n");
        prompt.append("  \"tableName\": \"users\",\n");
        prompt.append("  \"missingFields\": [\"email\"],\n");
        prompt.append("  \"humanResponse\": \"I need more information to create a user.\"\n");
        prompt.append("}\n\n");

        prompt.append("Now process the user request:");

        return prompt.toString();
    }

    private ConversationalLLMResponse parseComprehensiveResponse(String rawResponse, ConversationalLLMRequest request) {
        try {

            String cleanResponse = cleanJsonResponse(rawResponse);
            log.debug("Cleaned LLM response: {}", cleanResponse);

            JsonNode jsonNode = objectMapper.readTree(cleanResponse);

            String status = jsonNode.get("status").asText();
            ConversationalLLMResponse.ConversationalLLMResponseBuilder builder = ConversationalLLMResponse.builder();

            if ("SUCCESS".equals(status)) {
                builder.valid(true)
                        .intent(jsonNode.get("intent").asText())
                        .tableName(jsonNode.get("tableName").asText())
                        .sql(jsonNode.get("sql").asText())
                        .parameters(parseJsonParameters(jsonNode.get("parameters")))
                        .humanResponse(jsonNode.get("humanResponse").asText())
                        .needsMoreInfo(false);
            } else if ("MISSING_INFO".equals(status)) {
                builder.valid(true)
                        .needsMoreInfo(true)
                        .intent(jsonNode.get("intent").asText())
                        .tableName(jsonNode.get("tableName").asText())
                        .missingFields(parseJsonStringArray(jsonNode.get("missingFields")))
                        .humanResponse(jsonNode.get("humanResponse").asText());
            } else {
                builder.valid(false)
                        .errorMessage(jsonNode.get("errorMessage").asText());
            }

            return builder.build();

        } catch (Exception e) {
            log.error("Error parsing LLM JSON response: {}", rawResponse, e);

            return fallbackParsing(rawResponse, request);
        }
    }

    private String cleanJsonResponse(String response) {

        response = response.replaceAll("```json", "").replaceAll("```", "").trim();

        int startIndex = response.indexOf('{');
        int endIndex = response.lastIndexOf('}');

        if (startIndex != -1 && endIndex != -1 && endIndex > startIndex) {
            return response.substring(startIndex, endIndex + 1);
        }

        return response;
    }

    private Object[] parseJsonParameters(JsonNode parametersNode) {
        if (parametersNode == null || !parametersNode.isArray()) {
            return new Object[0];
        }

        List<Object> params = new ArrayList<>();
        for (JsonNode param : parametersNode) {
            if (param.isTextual()) {
                params.add(param.asText());
            } else if (param.isInt()) {
                params.add(param.asInt());
            } else if (param.isDouble()) {
                params.add(param.asDouble());
            } else {
                params.add(param.asText());
            }
        }
        return params.toArray();
    }

    private String[] parseJsonStringArray(JsonNode arrayNode) {
        if (arrayNode == null || !arrayNode.isArray()) {
            return new String[0];
        }

        List<String> items = new ArrayList<>();
        for (JsonNode item : arrayNode) {
            items.add(item.asText());
        }
        return items.toArray(new String[0]);
    }

    private ConversationalLLMResponse fallbackParsing(String response, ConversationalLLMRequest request) {
        log.warn("Using fallback parsing for response: {}", response);

        String lowerResponse = response.toLowerCase();

        if (lowerResponse.contains("missing") || lowerResponse.contains("need")) {
            return ConversationalLLMResponse.builder()
                    .valid(true)
                    .needsMoreInfo(true)
                    .humanResponse("I need more information to complete this request.")
                    .build();
        }

        return ConversationalLLMResponse.builder()
                .valid(false)
                .errorMessage("Could not understand the request format")
                .build();
    }

    @Override
    public String generateResponse(String prompt) {
        try {
            Map<String, Object> request = new HashMap<>();
            request.put("model", config.getModel());
            request.put("prompt", prompt);
            request.put("stream", false);
            // Optimize for faster response
            request.put("options", Map.of(
                    "temperature", 0.1,
                    "top_k", 10,
                    "top_p", 0.9,
                    "num_predict", 500
            ));

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(request, headers);

            Map<String, Object> response = restTemplate.postForObject(
                    config.getBaseUrl() + "/api/generate",
                    entity,
                    Map.class
            );

            return (String) response.get("response");

        } catch (Exception e) {
            log.error("Error calling LLM service", e);
            throw new RuntimeException("Failed to generate LLM response", e);
        }
    }
    @Override
    public String parseIntent(String userInput, DatabaseSchema schema) {
        String prompt = buildIntentPrompt(userInput, schema);
        return generateResponse(prompt);
    }

    @Override
    public String generateSQL(String intent, String userInput, DatabaseSchema schema) {
        String prompt = buildSQLPrompt(intent, userInput, schema);
        return generateResponse(prompt);
    }

    @Override
    public String generateSQLWithParameters(String intent, String userInput, DatabaseSchema.TableInfo tableInfo, DatabaseSchema schema) {
        String prompt = buildSQLWithParametersPrompt(intent, userInput, tableInfo, schema);
        String response = generateResponse(prompt);
        log.debug("Raw LLM response for SQL generation: {}", response);
        return response;
    }

    @Override
    public String formatResponse(String intent, Object data, boolean success, String errorMessage) {
        String prompt = buildResponsePrompt(intent, data, success, errorMessage);
        return generateResponse(prompt);
    }

    @Override
    public String validateAndCorrectInput(String userInput, String intent, DatabaseSchema.TableInfo tableInfo) {
        String prompt = buildValidationPrompt(userInput, intent, tableInfo);
        return generateResponse(prompt);
    }

    private String buildIntentPrompt(String userInput, DatabaseSchema schema) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("You are a SQL database assistant. Analyze this user request and determine the database operation intent.\n\n");
        prompt.append("Available operations: CREATE, READ, UPDATE, DELETE, COUNT, LIST\n");
        prompt.append("Available tables: ").append(String.join(", ", schema.getTables().keySet())).append("\n\n");
        prompt.append("User request: \"").append(userInput).append("\"\n\n");
        prompt.append("IMPORTANT: Respond with ONLY the format: OPERATION:TABLE_NAME\n");
        prompt.append("Examples:\n");
        prompt.append("- For \"find users named John\" → READ:users\n");
        prompt.append("- For \"find users like John\" → READ:users\n");
        prompt.append("- For \"search users containing 'john'\" → READ:users\n");
        prompt.append("- For \"create a new user\" → CREATE:users\n");
        prompt.append("- For \"update user data\" → UPDATE:users\n");
        prompt.append("- For \"delete user\" → DELETE:users\n");
        prompt.append("- For \"count users\" → COUNT:users\n");
        prompt.append("- For \"how many users\" → COUNT:users\n");
        prompt.append("- For \"list all users\" → LIST:users\n");
        prompt.append("- For \"show all users\" → LIST:users\n\n");
        prompt.append("Your response:");

        return prompt.toString();
    }

    private String buildSQLWithParametersPrompt(String intent, String userInput, DatabaseSchema.TableInfo tableInfo, DatabaseSchema schema) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("You are a SQL query generator. Generate a proper SQL query with parameters.\n\n");
        prompt.append("Operation: ").append(intent).append("\n");
        prompt.append("User request: \"").append(userInput).append("\"\n\n");

        prompt.append("Target table: ").append(tableInfo.getTableName()).append("\n");
        prompt.append("Table columns:\n");
        tableInfo.getColumns().forEach((colName, colInfo) -> {
            prompt.append("- ").append(colName).append(" (").append(colInfo.getDataType()).append(")");
            if (!colInfo.isNullable()) prompt.append(" [REQUIRED]");
            if (colInfo.isAutoIncrement()) prompt.append(" [AUTO_INCREMENT]");
            prompt.append("\n");
        });

        prompt.append("\nIMPORTANT RULES:\n");
        prompt.append("1. Generate ONLY valid SQL with ? placeholders for parameters\n");
        prompt.append("2. After the SQL, add |PARAMS| followed by the actual parameter values\n");
        prompt.append("3. String values should be in single quotes, numbers without quotes\n");
        prompt.append("4. Use LIKE with % wildcards for partial matches\n");
        prompt.append("5. Do NOT include any explanations, code, or markdown\n\n");

        prompt.append("FORMAT: SQL_STATEMENT|PARAMS|param1,param2,param3\n\n");

        prompt.append("Examples:\n");
        if ("READ".equals(intent)) {
            prompt.append("For exact match \"find user named John\":\n");
            prompt.append("SELECT * FROM users WHERE name = ?|PARAMS|'John'\n\n");
            prompt.append("For partial match \"find users like John\" or \"search users containing john\":\n");
            prompt.append("SELECT * FROM users WHERE name LIKE ?|PARAMS|'%John%'\n\n");
            prompt.append("For starts with \"find users starting with J\":\n");
            prompt.append("SELECT * FROM users WHERE name LIKE ?|PARAMS|'J%'\n\n");
            prompt.append("For ends with \"find users ending with son\":\n");
            prompt.append("SELECT * FROM users WHERE name LIKE ?|PARAMS|'%son'\n\n");
        } else if ("LIST".equals(intent)) {
            prompt.append("For \"list all users\" or \"show all users\":\n");
            prompt.append("SELECT * FROM users|PARAMS|\n\n");
            prompt.append("For \"list users ordered by name\":\n");
            prompt.append("SELECT * FROM users ORDER BY name|PARAMS|\n\n");
        } else if ("COUNT".equals(intent)) {
            prompt.append("For \"count users\" or \"how many users\":\n");
            prompt.append("SELECT COUNT(*) FROM users|PARAMS|\n\n");
            prompt.append("For \"count users named John\":\n");
            prompt.append("SELECT COUNT(*) FROM users WHERE name = ?|PARAMS|'John'\n\n");
            prompt.append("For \"count users like John\":\n");
            prompt.append("SELECT COUNT(*) FROM users WHERE name LIKE ?|PARAMS|'%John%'\n\n");
        } else if ("CREATE".equals(intent)) {
            prompt.append("For \"create user John email john@test.com age 25\":\n");
            prompt.append("INSERT INTO users (name, email, age) VALUES (?, ?, ?)|PARAMS|'John','john@test.com',25\n\n");
        } else if ("UPDATE".equals(intent)) {
            prompt.append("For \"update user John set age to 30\":\n");
            prompt.append("UPDATE users SET age = ? WHERE name = ?|PARAMS|30,'John'\n\n");
            prompt.append("For \"update users like John set age to 30\":\n");
            prompt.append("UPDATE users SET age = ? WHERE name LIKE ?|PARAMS|30,'%John%'\n\n");
        } else if ("DELETE".equals(intent)) {
            prompt.append("For \"delete user named John\":\n");
            prompt.append("DELETE FROM users WHERE name = ?|PARAMS|'John'\n\n");
            prompt.append("For \"delete users like John\":\n");
            prompt.append("DELETE FROM users WHERE name LIKE ?|PARAMS|'%John%'\n\n");
        }

        prompt.append("PATTERN DETECTION:\n");
        prompt.append("- Use exact match (=) for: \"find user named X\", \"get user X\"\n");
        prompt.append("- Use LIKE with %X% for: \"find users like X\", \"search X\", \"containing X\"\n");
        prompt.append("- Use LIKE with X% for: \"starts with X\", \"beginning with X\"\n");
        prompt.append("- Use LIKE with %X for: \"ends with X\", \"ending with X\"\n\n");

        prompt.append("Now generate the SQL for the user request:");

        return prompt.toString();
    }

    private String buildSQLPrompt(String intent, String userInput, DatabaseSchema schema) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("Generate a SQL query for this request.\n\n");
        prompt.append("Operation: ").append(intent).append("\n");
        prompt.append("User input: ").append(userInput).append("\n\n");

        prompt.append("Database schema:\n");
        schema.getTables().forEach((tableName, tableInfo) -> {
            prompt.append("Table: ").append(tableName).append("\n");
            tableInfo.getColumns().forEach((colName, colInfo) -> {
                prompt.append("  - ").append(colName).append(" (").append(colInfo.getDataType()).append(")\n");
            });
        });

        prompt.append("\nQuery Guidelines:\n");
        prompt.append("- Use LIKE with % wildcards for partial text matches\n");
        prompt.append("- Use COUNT(*) for counting operations\n");
        prompt.append("- Use SELECT * for listing all records\n");
        prompt.append("- Generate ONLY the SQL query with ? placeholders for parameters\n");
        prompt.append("- Do not include any explanations or code blocks\n");

        return prompt.toString();
    }


    private String buildResponsePrompt(String intent, Object data, boolean success, String errorMessage) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("Format this database operation result into a friendly response.\n\n");
        prompt.append("Operation: ").append(intent).append("\n");
        prompt.append("Success: ").append(success).append("\n");

        if (success) {
            prompt.append("Result data: ").append(data).append("\n");
        } else {
            prompt.append("Error: ").append(errorMessage).append("\n");
        }

        prompt.append("\nRespond in a helpful, conversational tone. Do not include technical details.\n");

        return prompt.toString();
    }


    private String buildValidationPrompt(String userInput, String intent, DatabaseSchema.TableInfo tableInfo) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("Check if the user input has all required information for this database operation.\n\n");
        prompt.append("Operation: ").append(intent).append("\n");
        prompt.append("User input: \"").append(userInput).append("\"\n");
        prompt.append("Table: ").append(tableInfo.getTableName()).append("\n\n");

        if ("CREATE".equals(intent)) {
            prompt.append("Required fields for CREATE operation:\n");
            tableInfo.getColumns().forEach((colName, colInfo) -> {
                if (!colInfo.isNullable() && !colInfo.isAutoIncrement()) {
                    prompt.append("- ").append(colName).append(" (").append(colInfo.getDataType()).append(")\n");
                }
            });
        } else if ("READ".equals(intent) || "UPDATE".equals(intent) || "DELETE".equals(intent)) {
            prompt.append("For ").append(intent).append(" operations, at least one search criteria is recommended.\n");
            prompt.append("Available searchable fields:\n");
            tableInfo.getColumns().forEach((colName, colInfo) -> {
                prompt.append("- ").append(colName).append(" (").append(colInfo.getDataType()).append(")\n");
            });
        } else if ("COUNT".equals(intent) || "LIST".equals(intent)) {
            prompt.append("For ").append(intent).append(" operations, no specific fields are required.\n");
            prompt.append("Optional filter fields:\n");
            tableInfo.getColumns().forEach((colName, colInfo) -> {
                prompt.append("- ").append(colName).append(" (").append(colInfo.getDataType()).append(")\n");
            });
        }

        prompt.append("\nRESPONSE FORMAT:\n");
        prompt.append("If missing critical information: MISSING:field1,field2\n");
        prompt.append("If complete or sufficient: COMPLETE\n");
        prompt.append("If needs clarification: CLARIFY:reason\n\n");
        prompt.append("Your response:");

        return prompt.toString();
    }
}