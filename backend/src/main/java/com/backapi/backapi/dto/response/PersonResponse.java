package com.backapi.backapi.dto.response;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;

@Data
@Builder
public class PersonResponse {
    private Long id;
    private String fullName;
    private Integer birthYear;
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
    private String status;
}