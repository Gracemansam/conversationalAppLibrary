package com.conversationalai.core;

import com.conversationalai.database.DatabaseSchemaAnalyzer;
import com.conversationalai.database.QueryExecutor;
import com.conversationalai.database.SchemaCache;
import com.conversationalai.dto.*;
import com.conversationalai.security.SecurityValidator;
import com.conversationalai.service.LLMService;
import org.springframework.stereotype.Component;
import lombok.extern.slf4j.Slf4j;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.regex.Matcher;
import java.util.ArrayList;
import java.util.Arrays;
@Slf4j
@Component
public class ConversationalProcessor {

    private final LLMService llmService;
    private final SecurityValidator securityValidator;
    private final QueryExecutor queryExecutor;
    private final SchemaCache schemaCache;
    private final ResponseFormatter responseFormatter;

    public ConversationalProcessor(LLMService llmService,
                                   SecurityValidator securityValidator,
                                   QueryExecutor queryExecutor,
                                   SchemaCache schemaCache,
                                   ResponseFormatter responseFormatter) {
        this.llmService = llmService;
        this.securityValidator = securityValidator;
        this.queryExecutor = queryExecutor;
        this.schemaCache = schemaCache;
        this.responseFormatter = responseFormatter;
    }

    public ConversationalResponse process(ConversationalRequest request) {
        long startTime = System.currentTimeMillis();

        try {

            DatabaseSchema schema = schemaCache.getSchema();


            ConversationalLLMRequest llmRequest = ConversationalLLMRequest.builder()
                    .userInput(request.getUserInput())
                    .schema(schema)
                    .build();

            ConversationalLLMResponse llmResponse = llmService.processConversationalRequest(llmRequest);

            if (!llmResponse.isValid()) {
                return buildErrorResponse(llmResponse.getHumanResponse(), startTime);
            }

            if (llmResponse.isNeedsMoreInfo()) {
                return buildMissingInfoResponse(llmResponse.getMissingFields(), startTime, llmResponse.getHumanResponse());
            }

            if (!securityValidator.isIntentAllowed(llmResponse.getIntent()) ||
                    !securityValidator.isQuerySafe(llmResponse.getSql())) {
                return buildErrorResponse("🚫 **Access Denied**\n\nThis operation is not permitted for security reasons. Please contact your administrator if you need access to this functionality.", startTime);
            }


            Object result = executeQuery(llmResponse.getIntent(), llmResponse.getSql(), llmResponse.getParameters());


            String formattedResponse = formatResponseByType(llmResponse.getIntent(), result, llmResponse.getHumanResponse());

            return ConversationalResponse.builder()
                    .response(formattedResponse)
                    .success(true)
                    .intent(llmResponse.getIntent())
                    .operation(llmResponse.getIntent())
                    .data(result instanceof List ? (List<Map<String, Object>>) result : null)
                    .processingTimeMs(System.currentTimeMillis() - startTime)
                    .build();

        } catch (Exception e) {
            log.error("Error processing request", e);
            return buildErrorResponse("**System Error**\n\nI encountered an unexpected error while processing your request. Please try again or contact support if the problem persists.", startTime);
        }
    }

    private String formatResponseByType(String intent, Object result, String baseMessage) {
        switch (intent.toUpperCase()) {
            case "READ":
            case "LIST":
                return responseFormatter.formatDataResponse(intent, (List<Map<String, Object>>) result, baseMessage);

            case "COUNT":
                return responseFormatter.formatCountResponse((List<Map<String, Object>>) result, baseMessage);

            case "UPDATE":
            case "DELETE":
                return responseFormatter.formatUpdateResponse(result, baseMessage);

            case "CREATE":
                return "✅ **Record Created Successfully**\n\nGreat! I've successfully added the new record to the database. The information has been saved and is now available for future searches.";

            default:
                return baseMessage;
        }
    }


    private ConversationalResponse buildErrorResponse(String message, long startTime) {
        return ConversationalResponse.builder()
                .response(message)
                .success(false)
                .errorMessage(message)
                .processingTimeMs(System.currentTimeMillis() - startTime)
                .build();
    }

    private ConversationalResponse buildMissingInfoResponse(String[] missingFields, long startTime, String customMessage) {
        String message = customMessage != null ? customMessage :
                "ℹ️ **Additional Information Needed**\n\nTo complete this operation, I need:\n• " +
                        String.join("\n• ", missingFields) +
                        "\n\nPlease provide this information and I'll be happy to help!";

        return ConversationalResponse.builder()
                .response(message)
                .success(false)
                .needsMoreInfo(true)
                .requiredFields(Arrays.asList(missingFields))
                .processingTimeMs(System.currentTimeMillis() - startTime)
                .build();
    }

    private Object executeQuery(String intent, String sql, Object[] parameters) {
        switch (intent.toUpperCase()) {
            case "CREATE":
            case "UPDATE":
            case "DELETE":
                int affectedRows = queryExecutor.executeUpdate(sql, parameters);
                return Map.of("affectedRows", affectedRows);
            case "READ":
            case "LIST":
            case "COUNT":
                return queryExecutor.executeQuery(sql, parameters);
            default:
                throw new IllegalArgumentException("Unsupported intent: " + intent);
        }
    }
}