package com.conversationalai.config;

import com.conversationalai.core.ConversationalProcessor;
import com.conversationalai.core.ConversationalService;
import com.conversationalai.database.DatabaseSchemaAnalyzer;
import com.conversationalai.database.QueryExecutor;
import com.conversationalai.database.SchemaCache;
import com.conversationalai.dto.ResponseFormatter;
import com.conversationalai.security.SecurityValidator;
import com.conversationalai.service.LLMService;
import com.conversationalai.serviceImpl.OllamaLLMService;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.sql.DataSource;

@Configuration
@EnableConfigurationProperties(ConversationalAIProperties.class)
public class ConversationalAIAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public LLMService llmService(ConversationalAIProperties properties) {
        return new OllamaLLMService(properties);
    }

    @Bean
    @ConditionalOnMissingBean
    public SecurityValidator securityValidator() {
        return new SecurityValidator();
    }

    @Bean
    @ConditionalOnMissingBean
    public JdbcTemplate jdbcTemplate(DataSource dataSource) {
        return new JdbcTemplate(dataSource);
    }

    @Bean
    @ConditionalOnMissingBean
    public DatabaseSchemaAnalyzer databaseSchemaAnalyzer(JdbcTemplate jdbcTemplate) {
        return new DatabaseSchemaAnalyzer(jdbcTemplate);
    }

    @Bean
    @ConditionalOnMissingBean
    public QueryExecutor queryExecutor(JdbcTemplate jdbcTemplate) {
        return new QueryExecutor(jdbcTemplate);
    }

    @Bean
    @ConditionalOnMissingBean
    public ConversationalProcessor conversationalProcessor(
            LLMService llmService,
            SecurityValidator securityValidator,

            QueryExecutor queryExecutor,
            SchemaCache schemaCache,
            ResponseFormatter responseFormatter) {
        return new ConversationalProcessor(llmService, securityValidator,  queryExecutor, schemaCache,responseFormatter);
    }

    @Bean
    @ConditionalOnMissingBean
    public ConversationalService conversationalService(ConversationalProcessor processor) {
        return new ConversationalService(processor);
    }
}