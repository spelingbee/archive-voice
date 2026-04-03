package com.backapi.backapi.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.antlr.v4.runtime.misc.NotNull;

import java.time.LocalDate;

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
}
