package com.backapi.backapi.dto.response;

import com.backapi.backapi.enums.PersonStatus;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
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
    private PersonStatus moderationStatus;
    private String createdByEmail;
    private LocalDateTime createdAt;
    private String rejectionReason;
    private List<DocumentResponse> documents;
    private List<DuplicateCandidateResponse> duplicates;
}