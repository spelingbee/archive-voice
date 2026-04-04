package com.backapi.backapi.chat;

import com.backapi.backapi.dto.request.ChatRequest;
import com.backapi.backapi.dto.response.ChatResponse;
import com.backapi.backapi.dto.response.SessionMessageResponse;
import com.backapi.backapi.dto.response.SessionResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class RagService {

    @Value("${rag.service.url:http://localhost:8001}")
    private String ragUrl;

    private final RestTemplate restTemplate;
    private final WebClient webClient;

    // ─── Chat ───────────────────────────────────────────────────────

    public ChatResponse chat(Long userId, ChatRequest request) {
        Map<String, Object> body = new HashMap<>();
        body.put("question", request.getQuestion());
        body.put("user_id", userId.toString());
        body.put("session_id", request.getSessionId());
        body.put("person_id", request.getPersonId());

        return restTemplate.postForObject(ragUrl + "/chat", body, ChatResponse.class);
    }

    // ─── Chat SSE Streaming ─────────────────────────────────────────

    /**
     * Streaming chat: connects to rag-service /chat/stream via WebClient.
     * Reads raw DataBuffer chunks and converts to string lines,
     * preserving the streaming nature of the response.
     */
    public Flux<String> chatStream(Long userId, ChatRequest request) {
        Map<String, Object> body = new HashMap<>();
        body.put("question", request.getQuestion());
        body.put("user_id", userId.toString());
        body.put("session_id", request.getSessionId());
        body.put("person_id", request.getPersonId());

        return webClient.post()
                .uri("/chat/stream")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(body)
                .retrieve()
                .bodyToFlux(org.springframework.core.io.buffer.DataBuffer.class)
                .map(dataBuffer -> {
                    byte[] bytes = new byte[dataBuffer.readableByteCount()];
                    dataBuffer.read(bytes);
                    org.springframework.core.io.buffer.DataBufferUtils.release(dataBuffer);
                    return new String(bytes, java.nio.charset.StandardCharsets.UTF_8);
                })
                .flatMap(chunk -> {
                    // Split SSE lines: each "data: {...}\n\n" becomes one element
                    String[] lines = chunk.split("\n\n");
                    return Flux.fromArray(lines);
                })
                .filter(line -> line.startsWith("data: "))
                .map(line -> line.substring(6).trim());  // strip "data: " prefix
    }

    // ─── Sessions ───────────────────────────────────────────────────

    public List<SessionResponse> getSessions(Long userId) {
        return restTemplate.exchange(
                ragUrl + "/sessions/" + userId,
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<List<SessionResponse>>() {}
        ).getBody();
    }

    public List<SessionMessageResponse> getMessages(Long userId, Long sessionId) {
        return restTemplate.exchange(
                ragUrl + "/sessions/" + userId + "/" + sessionId + "/messages",
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<List<SessionMessageResponse>>() {}
        ).getBody();
    }

    public void deleteSession(Long sessionId) {
        restTemplate.delete(ragUrl + "/sessions/" + sessionId);
    }

    public Map<String, String> renameSession(Long sessionId, String title) {
        Map<String, String> body = Map.of("title", title);
        HttpEntity<Map<String, String>> entity = new HttpEntity<>(body);
        return restTemplate.exchange(
                ragUrl + "/sessions/" + sessionId + "/title",
                HttpMethod.PATCH, entity,
                new ParameterizedTypeReference<Map<String, String>>() {}
        ).getBody();
    }

    // ─── Person Embedding Duplicate Detection ──────────────────────

    @SuppressWarnings("unchecked")
    public List<Map<String, Object>> findSimilarPersonsByEmbedding(String text, int topK, Long excludeId) {
        try {
            Map<String, Object> body = new HashMap<>();
            body.put("text", text);
            body.put("top_k", topK);
            if (excludeId != null) {
                body.put("exclude_id", excludeId);
            }
            Object result = restTemplate.postForObject(ragUrl + "/persons/embed-similarity", body, Object.class);
            if (result instanceof List) {
                return (List<Map<String, Object>>) result;
            }
            return List.of();
        } catch (Exception e) {
            return List.of();
        }
    }

    public void storePersonEmbedding(Long personId, String text) {
        try {
            Map<String, Object> body = new HashMap<>();
            body.put("person_id", personId);
            body.put("text", text);
            restTemplate.postForObject(ragUrl + "/persons/embed-store", body, Object.class);
        } catch (Exception e) {
            // rag-service недоступен — не блокируем создание
        }
    }
}
