package com.conversationalai.dto;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ConversationalLLMResponse {
    private String intent;
    private String tableName;
    private String sql;
    private Object[] parameters;
    private String humanResponse;
    private boolean valid;
    private boolean needsMoreInfo;
    private String[] missingFields;
    private String errorMessage;
}