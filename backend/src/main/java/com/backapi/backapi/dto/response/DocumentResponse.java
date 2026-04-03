package com.backapi.backapi.dto.response;

import com.backapi.backapi.enums.DocumentStatus;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class DocumentResponse {
    private Long id;
    private String originalFileName;
    private String fileType;
    private Long fileSize;
    private DocumentStatus status;
    private String uploadedByUsername;
    private LocalDateTime uploadedAt;
    private String moderatedByUsername;
    private LocalDateTime moderatedAt;
    private String rejectionReason;
    private String fileUrl;           // для скачивания
}