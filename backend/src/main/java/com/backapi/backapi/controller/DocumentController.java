package com.backapi.backapi.controller;

import com.backapi.backapi.dto.request.DocumentUploadRequest;
import com.backapi.backapi.dto.response.DocumentResponse;
import com.backapi.backapi.entity.User;
import com.backapi.backapi.service.DocumentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@RestController
@RequestMapping("/api/documents")
@RequiredArgsConstructor
@Tag(name = "Documents", description = "Работа с документами и материалами к личностям")
public class DocumentController {

    private final DocumentService documentService;

    // ====================== Загрузка документа пользователем ======================
    @Operation(summary = "Загрузить документ к личности")
    @PreAuthorize("hasRole('USER')")
    @PostMapping("/upload")
    public ResponseEntity<DocumentResponse> uploadDocument(
            @ModelAttribute DocumentUploadRequest request,   // используем @ModelAttribute для MultipartFile
            @AuthenticationPrincipal User currentUser) throws IOException {

        DocumentResponse response = documentService.uploadDocument(request, currentUser);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    // ====================== Документы для обычного пользователя ======================
    @Operation(summary = "Получить свои документы + подтверждённые документы других")
    @PreAuthorize("hasRole('USER')")
    @GetMapping
    public ResponseEntity<Page<DocumentResponse>> getMyDocuments(Pageable pageable,
                                                                 @AuthenticationPrincipal User currentUser) {
        Page<DocumentResponse> documents = documentService.getDocumentsForUser(currentUser, pageable);
        return ResponseEntity.ok(documents);
    }

    // ====================== Все документы для модератора ======================
    @Operation(summary = "Получить все документы (для модератора)")
    @PreAuthorize("hasRole('MODERATOR')")
    @GetMapping("/all")
    public ResponseEntity<Page<DocumentResponse>> getAllDocuments(Pageable pageable) {
        Page<DocumentResponse> documents = documentService.getAllDocuments(pageable);
        return ResponseEntity.ok(documents);
    }

    // ====================== Подтверждение документа модератором ======================
    @Operation(summary = "Подтвердить документ")
    @PreAuthorize("hasRole('MODERATOR')")
    @PutMapping("/{id}/approve")
    public ResponseEntity<DocumentResponse> approveDocument(
            @PathVariable Long id,
            @AuthenticationPrincipal User moderator) {

        DocumentResponse response = documentService.approveDocument(id, moderator);
        return ResponseEntity.ok(response);
    }

    // ====================== Отклонение документа модератором ======================
    @Operation(summary = "Отклонить документ")
    @PreAuthorize("hasRole('MODERATOR')")
    @PutMapping("/{id}/reject")
    public ResponseEntity<DocumentResponse> rejectDocument(
            @PathVariable Long id,
            @RequestParam String reason,
            @AuthenticationPrincipal User moderator) {

        DocumentResponse response = documentService.rejectDocument(id, moderator, reason);
        return ResponseEntity.ok(response);
    }
}
