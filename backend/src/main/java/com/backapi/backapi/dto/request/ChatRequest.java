package com.backapi.backapi.dto.request;

import lombok.Data;

@Data
public class ChatRequest {
    private String question;
    private Long sessionId;  // null если новый чат
    private Long personId;   // null = общий чат, число = карточка персоны
}