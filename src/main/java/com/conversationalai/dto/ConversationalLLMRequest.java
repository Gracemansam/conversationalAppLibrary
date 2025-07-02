package com.conversationalai.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ConversationalLLMRequest {
    private String userInput;
    private DatabaseSchema schema;
}