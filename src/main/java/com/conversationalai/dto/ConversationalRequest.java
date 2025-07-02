package com.conversationalai.dto;

import lombok.Data;
import lombok.Builder;

@Data
@Builder
public class ConversationalRequest {
    private String userInput;
    private String sessionId;
    private String userId;
    private Object context;
}
