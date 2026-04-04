package com.backapi.backapi.chat;

import com.backapi.backapi.dto.request.ChatRequest;
import com.backapi.backapi.dto.response.*;
import com.backapi.backapi.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
@Tag(name = "Chat (RAG)", description = "Чат с ИИ-ассистентом на основе документов архива")
public class RagController {

    private final RagService ragService;
    private final UserService userService;

    @Operation(summary = "Отправить вопрос в чат")
    @PostMapping
    public ResponseEntity<ApiResponseWrapper<ChatResponse>> chat(@RequestBody ChatRequest request) {
        Long userId = userService.getCurrentUser().getId();
        ChatResponse response = ragService.chat(userId, request);
        return ResponseEntity.ok(ApiResponseWrapper.of(response));
    }

    @Operation(summary = "Стриминг ответа (SSE)")
    @PostMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ServerSentEvent<String>> chatStream(@RequestBody ChatRequest request) {
        Long userId = userService.getCurrentUser().getId();

        return ragService.chatStream(userId, request)
                .map(data -> ServerSentEvent.<String>builder()
                        .data(data)
                        .build());
    }

    @Operation(summary = "Список сессий текущего пользователя")
    @GetMapping("/sessions")
    public ResponseEntity<ApiResponseWrapper<List<SessionResponse>>> getSessions() {
        Long userId = userService.getCurrentUser().getId();
        List<SessionResponse> sessions = ragService.getSessions(userId);
        return ResponseEntity.ok(ApiResponseWrapper.of(sessions));
    }

    @Operation(summary = "Сообщения конкретной сессии")
    @GetMapping("/sessions/{sessionId}/messages")
    public ResponseEntity<ApiResponseWrapper<List<SessionMessageResponse>>> getMessages(
            @PathVariable Long sessionId) {
        Long userId = userService.getCurrentUser().getId();
        List<SessionMessageResponse> messages = ragService.getMessages(userId, sessionId);
        return ResponseEntity.ok(ApiResponseWrapper.of(messages));
    }

    @Operation(summary = "Удалить сессию")
    @DeleteMapping("/sessions/{sessionId}")
    public ResponseEntity<Void> deleteSession(@PathVariable Long sessionId) {
        ragService.deleteSession(sessionId);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Переименовать сессию")
    @PatchMapping("/sessions/{sessionId}/title")
    public ResponseEntity<ApiResponseWrapper<Map<String, String>>> renameSession(
            @PathVariable Long sessionId,
            @RequestBody Map<String, String> body) {
        Map<String, String> result = ragService.renameSession(sessionId, body.get("title"));
        return ResponseEntity.ok(ApiResponseWrapper.of(result));
    }
}