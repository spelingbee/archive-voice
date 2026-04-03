package com.backapi.backapi.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.springframework.web.multipart.MultipartFile;

@Data
public class DocumentUploadRequest {

    @NotNull(message = "Файл обязателен")
    private MultipartFile file;

    @NotNull(message = "ID личности обязателен")
    private Long personId;

    private String description; // опциональное описание документа
}
