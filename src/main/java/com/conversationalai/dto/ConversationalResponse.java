package com.conversationalai.dto;

import lombok.Data;
import lombok.Builder;
import java.util.List;
import java.util.Map;

@Data
@Builder
public class ConversationalResponse {
    private String response;
    private boolean success;
    private String intent;
    private String operation;
    private List<Map<String, Object>> data;
    private String errorMessage;
    private boolean needsMoreInfo;
    private List<String> requiredFields;
    private long processingTimeMs;
}