package com.backapi.backapi.chat;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "chat_messages")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatMessage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "session_id")
    private ChatSession session;

    private String role;           // "user" или "assistant"

    @Column(columnDefinition = "TEXT")
    private String content;

    private LocalDateTime sentAt;

    // Для RAG — какие документы были использованы в ответе
    private String usedDocuments;
}
