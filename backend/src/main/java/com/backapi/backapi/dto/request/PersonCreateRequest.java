package com.backapi.backapi.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;

@Data
public class PersonCreateRequest {

    @NotBlank(message = "ФИО обязательно")
    private String fullName;

    @NotNull(message = "Год рождения обязателен")
    private Integer birthYear;

    @NotNull(message = "Год смерти обязателен")
    private Integer deathYear;

    private String region;
    private String district;
    private String occupation;
    private String charge;

    private LocalDate arrestDate;
    private String sentence;
    private LocalDate sentenceDate;
    private LocalDate rehabilitationDate;

    private String biography;
    private String source;
}
