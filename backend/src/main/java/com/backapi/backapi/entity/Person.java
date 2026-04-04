package com.backapi.backapi.entity;


import com.backapi.backapi.enums.PersonStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.antlr.v4.runtime.misc.NotNull;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Builder
public class Person {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    private String fullName;      // full_name
    @NotNull
    private Integer birthYear;    // birth_year

    @NotNull
    private Integer deathYear;    // death_year

    private String region;        // region

    private String district;      // district

    private String occupation;    // occupation

    private String charge;        // charge (статья обвинения)

    private LocalDate arrestDate; // arrest_date

    private String sentence;      // sentence (приговор)

    private LocalDate sentenceDate; // sentence_date

    private LocalDate rehabilitationDate; // rehabilitation_date

    private String biography;     // biography

    private String source;        // source (архивные данные)

    private String status;        // status (verified/unverified)

    // ─── Документы ──────────────────────────────────────────────────
    @OneToMany(mappedBy = "person", fetch = FetchType.LAZY)
    @Builder.Default
    private List<Document> documents = new ArrayList<>();

    // ─── Модерация ──────────────────────────────────────────────────
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private PersonStatus moderationStatus = PersonStatus.PENDING;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by_id")
    private User moderatedBy;

    @Column(updatable = false)
    private LocalDateTime createdAt;

    private String rejectionReason;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }
}
