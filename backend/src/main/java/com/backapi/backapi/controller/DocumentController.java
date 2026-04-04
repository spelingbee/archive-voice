package com.backapi.backapi.controller;

import com.backapi.backapi.dto.request.DocumentUploadRequest;
import com.backapi.backapi.dto.response.ApiResponseWrapper;
import com.backapi.backapi.dto.response.DocumentResponse;
import com.backapi.backapi.dto.response.PaginatedResponse;
import com.backapi.backapi.entity.Document;
import com.backapi.backapi.entity.User;
import com.backapi.backapi.service.DocumentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;

@RestController
@RequestMapping("/api/documents")
@RequiredArgsConstructor
@Tag(name = "Documents", description = "Работа с документами и материалами к личностям")
public class DocumentController {

    private final DocumentService documentService;

    // ─── Загрузка ───────────────────────────────────────────────────

    @Operation(summary = "Загрузить документ к личности")
    @PostMapping("/upload")
    public ResponseEntity<ApiResponseWrapper<DocumentResponse>> uploadDocument(
            @ModelAttribute DocumentUploadRequest request,
            @AuthenticationPrincipal User currentUser) throws IOException {

        DocumentResponse response = documentService.uploadDocument(request, currentUser);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponseWrapper.of(response, "Документ загружен"));
    }

    // ─── Скачивание ─────────────────────────────────────────────────

    @Operation(summary = "Скачать документ по ID")
    @GetMapping("/{id}/download")
    public ResponseEntity<Resource> downloadDocument(@PathVariable Long id) throws IOException {
        Document doc = documentService.getDocumentEntity(id);
        Resource resource = documentService.loadFileAsResource(doc);

        // Определяем Content-Type
        String contentType = doc.getFileType();
        if (contentType == null || contentType.isBlank()) {
            contentType = "application/octet-stream";
        }

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(contentType))
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + doc.getOriginalFileName() + "\"")
                .body(resource);
    }

    // ─── Списки ─────────────────────────────────────────────────────

    @Operation(summary = "Получить свои документы + подтверждённые документы других")
    @GetMapping
    public ResponseEntity<ApiResponseWrapper<PaginatedResponse<DocumentResponse>>> getMyDocuments(
            Pageable pageable,
            @AuthenticationPrincipal User currentUser) {
        PaginatedResponse<DocumentResponse> page = PaginatedResponse.from(
                documentService.getDocumentsForUser(currentUser, pageable));
        return ResponseEntity.ok(ApiResponseWrapper.of(page));
    }

    @Operation(summary = "Получить все документы (для модератора)")
    @PreAuthorize("hasRole('MODERATOR')")
    @GetMapping("/all")
    public ResponseEntity<ApiResponseWrapper<PaginatedResponse<DocumentResponse>>> getAllDocuments(Pageable pageable) {
        PaginatedResponse<DocumentResponse> page = PaginatedResponse.from(
                documentService.getAllDocuments(pageable));
        return ResponseEntity.ok(ApiResponseWrapper.of(page));
    }

    // ─── Модерация ──────────────────────────────────────────────────

    @Operation(summary = "Подтвердить документ")
    @PreAuthorize("hasRole('MODERATOR')")
    @PutMapping("/{id}/approve")
    public ResponseEntity<ApiResponseWrapper<DocumentResponse>> approveDocument(
            @PathVariable Long id,
            @AuthenticationPrincipal User moderator) {
        DocumentResponse response = documentService.approveDocument(id, moderator);
        return ResponseEntity.ok(ApiResponseWrapper.of(response, "Документ подтверждён"));
    }

    @Operation(summary = "Отклонить документ")
    @PreAuthorize("hasRole('MODERATOR')")
    @PutMapping("/{id}/reject")
    public ResponseEntity<ApiResponseWrapper<DocumentResponse>> rejectDocument(
            @PathVariable Long id,
            @RequestParam String reason,
            @AuthenticationPrincipal User moderator) {
        DocumentResponse response = documentService.rejectDocument(id, moderator, reason);
        return ResponseEntity.ok(ApiResponseWrapper.of(response, "Документ отклонён"));
    }
}
