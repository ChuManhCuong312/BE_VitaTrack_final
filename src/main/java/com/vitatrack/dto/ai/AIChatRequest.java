package com.vitatrack.dto.ai;

import lombok.Data;

@Data
public class AIChatRequest {
    private String message;
    private String conversationId;  // null for new conversation
}
