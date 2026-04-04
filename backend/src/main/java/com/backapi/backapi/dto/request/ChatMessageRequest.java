package com.backapi.backapi.dto.request;

import lombok.Data;

@Data
public class ChatMessageRequest {
    private String message;
    private Long personId;        // null = GENERAL чат
}
