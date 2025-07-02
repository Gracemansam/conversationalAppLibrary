package com.conversationalai.chatapplication;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import lombok.Data;

@Configuration
@ConfigurationProperties(prefix = "conversational.ai")
@Data
public class CustomConversationalConfig {


    private LLMConfig llm = new LLMConfig();

    @Data
    public static class LLMConfig {
        private String baseUrl = "http://localhost:11434";
        private String model = "llama3.2";
        private int timeout = 30000;
        private double temperature = 0.3;
    }
}