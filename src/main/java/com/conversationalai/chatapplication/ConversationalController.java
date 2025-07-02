package com.conversationalai.chatapplication;


import com.conversationalai.core.ConversationalService;
import com.conversationalai.dto.ConversationalResponse;
import org.springframework.web.bind.annotation.*;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/conversation")
@RequiredArgsConstructor
public class ConversationalController {

    private final ConversationalService conversationalService;

    @PostMapping("/chat")
    public ConversationalResponse chat(@RequestBody ChatRequest request) {
        return conversationalService.processRequest(
                request.getMessage(),
                request.getSessionId(),
                request.getUserId()
        );
    }

    @GetMapping("/chat")
    public ConversationalResponse chatGet(@RequestParam String message) {
        return conversationalService.processRequest(message);
    }

    public static class ChatRequest {
        private String message;
        private String sessionId;
        private String userId;

        // Getters and setters
        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }
        public String getSessionId() { return sessionId; }
        public void setSessionId(String sessionId) { this.sessionId = sessionId; }
        public String getUserId() { return userId; }
        public void setUserId(String userId) { this.userId = userId; }
    }
}
