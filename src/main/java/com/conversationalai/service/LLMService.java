package com.conversationalai.service;


import com.conversationalai.dto.*;

public interface LLMService {
    String generateResponse(String prompt);

    ConversationalLLMResponse processConversationalRequest(ConversationalLLMRequest request);

    @Deprecated
    String parseIntent(String userInput, DatabaseSchema schema);

    @Deprecated
    String generateSQL(String intent, String userInput, DatabaseSchema schema);

    @Deprecated
    String generateSQLWithParameters(String intent, String userInput, DatabaseSchema.TableInfo tableInfo, DatabaseSchema schema);

    @Deprecated
    String formatResponse(String intent, Object data, boolean success, String errorMessage);

    @Deprecated
    String validateAndCorrectInput(String userInput, String intent, DatabaseSchema.TableInfo tableInfo);
}