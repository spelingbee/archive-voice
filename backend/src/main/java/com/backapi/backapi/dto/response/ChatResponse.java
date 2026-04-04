package com.backapi.backapi.dto.response;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class ChatResponse {
    private String response;
    private List<String> sources;   // какие документы были использованы
}
