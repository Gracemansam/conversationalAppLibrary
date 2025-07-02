package com.conversationalai.core;


import com.conversationalai.dto.ConversationalRequest;
import com.conversationalai.dto.ConversationalResponse;
import org.springframework.stereotype.Service;

@Service
public class ConversationalService {

    private final ConversationalProcessor processor;

    public ConversationalService(ConversationalProcessor processor) {
        this.processor = processor;
    }

    public ConversationalResponse processRequest(String userInput) {
        return processRequest(userInput, null, null);
    }

    public ConversationalResponse processRequest(String userInput, String sessionId) {
        return processRequest(userInput, sessionId, null);
    }

    public ConversationalResponse processRequest(String userInput, String sessionId, String userId) {
        ConversationalRequest request = ConversationalRequest.builder()
                .userInput(userInput)
                .sessionId(sessionId)
                .userId(userId)
                .build();

        return processor.process(request);
    }
}
