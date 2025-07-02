package com.conversationalai.config;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import org.springframework.boot.context.properties.ConfigurationProperties;

import org.springframework.boot.context.properties.ConfigurationProperties;
import lombok.Data;

@Data
@ConfigurationProperties(prefix = "conversational.ai")
public class ConversationalAIProperties {

    private LLMConfig llm = new LLMConfig();
    private SecurityConfig security = new SecurityConfig();
    private DatabaseConfig database = new DatabaseConfig();

    @Data
    public static class LLMConfig {
        private String baseUrl = "http://localhost:11434";
        private String model = "llama3.2";
        private int timeout = 30000;
        private double temperature = 0.1;
    }

    @Data
    public static class SecurityConfig {
        private boolean enableSafetyCheck = true;
        private int maxRecordsPerOperation = 100;
        private String[] allowedOperations = {"CREATE", "READ", "UPDATE", "DELETE"};
        private String[] blockedKeywords = {"DROP", "TRUNCATE", "ALTER", "GRANT", "REVOKE"};
    }

    @Data
    public static class DatabaseConfig {
        private boolean autoDiscoverSchema = true;
        private String[] includeTables = {};
        private String[] excludeTables = {};
    }
}