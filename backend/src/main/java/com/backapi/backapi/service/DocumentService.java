package com.backapi.backapi.service;

import com.backapi.backapi.dto.request.DocumentUploadRequest;
import com.backapi.backapi.dto.response.DocumentResponse;
import com.backapi.backapi.entity.Document;
import com.backapi.backapi.entity.Person;
import com.backapi.backapi.entity.User;
import com.backapi.backapi.enums.DocumentStatus;
import com.backapi.backapi.repository.DocumentRepository;
import com.backapi.backapi.repository.PersonRepository;
import com.backapi.backapi.util.FileNameUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Service
@RequiredArgsConstructor
public class DocumentService {

    private final DocumentRepository documentRepository;
    private final PersonRepository personRepository;
    private final FileNameUtils fileNameUtils;

    private final String BASE_UPLOAD_DIR = "uploads/";

    // Для обычного пользователя: свои документы + подтверждённые от других
    public Page<DocumentResponse> getDocumentsForUser(User user, Pageable pageable) {
        return documentRepository.findByStatusOrUploadedBy(DocumentStatus.APPROVED, user, pageable)
                .map(this::mapToResponse);
    }

    // Для модератора: все документы
    public Page<DocumentResponse> getAllDocuments(Pageable pageable) {
        return documentRepository.findAll(pageable).map(this::mapToResponse);
    }

    @Transactional
    public DocumentResponse approveDocument(Long documentId, User moderator) {
        Document doc = documentRepository.findById(documentId)
                .orElseThrow(() -> new RuntimeException("Документ не найден"));

        doc.setStatus(DocumentStatus.APPROVED);
        doc.setModeratedBy(moderator);
        doc.setModeratedAt(LocalDateTime.now());

        return mapToResponse(documentRepository.save(doc));
    }

    @Transactional
    public DocumentResponse rejectDocument(Long documentId, User moderator, String reason) {
        Document doc = documentRepository.findById(documentId)
                .orElseThrow(() -> new RuntimeException("Документ не найден"));

        doc.setStatus(DocumentStatus.REJECTED);
        doc.setModeratedBy(moderator);
        doc.setModeratedAt(LocalDateTime.now());
        doc.setRejectionReason(reason);

        return mapToResponse(documentRepository.save(doc));
    }

    // ==================== Маппинг ====================
    private DocumentResponse mapToResponse(Document doc) {
        return DocumentResponse.builder()
                .id(doc.getId())
                .originalFileName(doc.getOriginalFileName())
                .fileType(doc.getFileType())
                .fileSize(doc.getFileSize())
                .status(doc.getStatus())
                .uploadedByUsername(doc.getUploadedBy().getEmail())
                .uploadedAt(doc.getUploadedAt())
                .moderatedByUsername(doc.getModeratedBy() != null ? doc.getModeratedBy().getEmail() : null)
                .moderatedAt(doc.getModeratedAt())
                .rejectionReason(doc.getRejectionReason())
                .fileUrl("/uploads/" + doc.getFileName())   // для фронта
                .build();
    }

    private String getFileExtension(String filename) {
        if (filename == null || !filename.contains(".")) {
            return "bin";
        }
        return filename.substring(filename.lastIndexOf(".") + 1).toLowerCase();
    }

    @Transactional
    public DocumentResponse uploadDocument(DocumentUploadRequest request, User currentUser) throws IOException {
        MultipartFile file = request.getFile();
        Long personId = request.getPersonId();

        // Проверяем существование Person
        Person person = personRepository.findById(personId)
                .orElseThrow(() -> new RuntimeException("Person с id " + personId + " не найден"));

        // Создаём структуру папок: uploads/2026/04/
        String yearMonth = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy/MM"));
        Path uploadDir = Paths.get(BASE_UPLOAD_DIR, yearMonth);
        if (!Files.exists(uploadDir)) {
            Files.createDirectories(uploadDir);
        }

        // Генерируем красивое имя файла
        String extension = getFileExtension(file.getOriginalFilename());
        String newFileName = fileNameUtils.generateFileName(personId, person.getFullName(), extension);

        Path filePath = uploadDir.resolve(newFileName);

        // Сохраняем файл
        Files.copy(file.getInputStream(), filePath);

        // Сохраняем в БД
        Document document = Document.builder()
                .fileName(newFileName)
                .originalFileName(file.getOriginalFilename())
                .fileType(file.getContentType())
                .fileSize(file.getSize())
                .filePath(filePath.toString().replace("\\", "/"))
                .status(DocumentStatus.PENDING)
                .person(person)
                .uploadedBy(currentUser)
                .build();

        Document saved = documentRepository.save(document);

        System.out.println("Документ загружен: " + newFileName + " к Person id=" + personId);

        return mapToResponse(saved);
    }
}