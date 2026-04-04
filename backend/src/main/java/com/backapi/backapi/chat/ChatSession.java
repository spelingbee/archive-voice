package com.backapi.backapi.chat;

import com.backapi.backapi.entity.Person;
import com.backapi.backapi.entity.User;
import com.backapi.backapi.enums.ChatType;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "chat_sessions")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatSession {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String sessionId;           // уникальный UUID для чата

    @ManyToOne
    @JoinColumn(name = "user_id")
    private User user;

    @ManyToOne
    @JoinColumn(name = "person_id")     // null = общий чат
    private Person person;

    private String title;               // "Общий чат" или "Чат с Ивановым И.И."

    @Enumerated(EnumType.STRING)
    private ChatType chatType;          // GENERAL или PERSON

    private LocalDateTime createdAt;
    private LocalDateTime lastMessageAt;
}
