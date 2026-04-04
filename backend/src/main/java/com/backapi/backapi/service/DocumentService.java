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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
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

    private static final Logger log = LoggerFactory.getLogger(DocumentService.class);

    private final DocumentRepository documentRepository;
    private final PersonRepository personRepository;
    private final FileNameUtils fileNameUtils;

    @Value("${app.upload.dir:${user.dir}/uploads}")
    private String uploadDir;

    // ==================== Загрузка ====================

    @Transactional
    public DocumentResponse uploadDocument(DocumentUploadRequest request, User currentUser) throws IOException {
        MultipartFile file = request.getFile();
        Long personId = request.getPersonId();

        if (file == null || file.isEmpty()) {
            throw new RuntimeException("Файл пустой или не передан");
        }

        Person person = personRepository.findById(personId)
                .orElseThrow(() -> new RuntimeException("Person с id " + personId + " не найден"));

        // Создаём структуру папок: uploads/2026/04/
        String yearMonth = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy/MM"));
        Path targetDir = Paths.get(uploadDir, yearMonth).toAbsolutePath().normalize();
        Files.createDirectories(targetDir);

        // Генерируем имя файла
        String extension = getFileExtension(file.getOriginalFilename());
        String newFileName = fileNameUtils.generateFileName(personId, person.getFullName(), extension);

        Path targetPath = targetDir.resolve(newFileName);

        // Сохраняем файл на диск
        file.transferTo(targetPath.toFile());

        log.info("Файл сохранён: {} (размер: {} байт)", targetPath, file.getSize());

        // Сохраняем запись в БД
        Document document = Document.builder()
                .fileName(newFileName)
                .originalFileName(file.getOriginalFilename())
                .fileType(file.getContentType())
                .fileSize(file.getSize())
                .filePath(yearMonth + "/" + newFileName)
                .status(DocumentStatus.PENDING)
                .person(person)
                .uploadedBy(currentUser)
                .build();

        Document saved = documentRepository.save(document);
        log.info("Документ записан в БД: id={}, file={}, person_id={}", saved.getId(), newFileName, personId);

        return mapToResponse(saved);
    }

    // ==================== Скачивание ====================

    public Document getDocumentEntity(Long id) {
        return documentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Документ не найден с id: " + id));
    }

    public org.springframework.core.io.Resource loadFileAsResource(Document doc) throws IOException {
        String storedPath = doc.getFilePath().replace("\\", "/");

        Path filePath;
        // Если путь абсолютный (старые записи) — используем как есть
        if (Paths.get(storedPath).isAbsolute()) {
            filePath = Paths.get(storedPath).normalize();
        } else {
            // Относительный — резолвим от uploadDir
            // Убираем "uploads/" если есть (старый формат)
            if (storedPath.startsWith("uploads/")) {
                storedPath = storedPath.substring("uploads/".length());
            }
            filePath = Paths.get(uploadDir, storedPath).toAbsolutePath().normalize();
        }

        if (!Files.exists(filePath)) {
            log.error("Файл не найден: {}. FilePath из БД: {}", filePath, doc.getFilePath());
            throw new RuntimeException("Файл не найден на диске: " + filePath);
        }

        return new org.springframework.core.io.UrlResource(filePath.toUri());
    }

    // ==================== Списки ====================

    public Page<DocumentResponse> getDocumentsForUser(User user, Pageable pageable) {
        return documentRepository.findByStatusOrUploadedBy(DocumentStatus.APPROVED, user, pageable)
                .map(this::mapToResponse);
    }

    public Page<DocumentResponse> getAllDocuments(Pageable pageable) {
        return documentRepository.findAll(pageable).map(this::mapToResponse);
    }

    // ==================== Модерация ====================

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
        // Строим URL: убираем возможный дублирующийся "uploads/" из filePath
        String relativePath = doc.getFilePath().replace("\\", "/");
        // Убираем всё до и включая "uploads/" если оно есть (старый формат)
        int idx = relativePath.indexOf("uploads/");
        if (idx >= 0) {
            relativePath = relativePath.substring(idx + "uploads/".length());
        }

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
                .fileUrl("/uploads/" + relativePath)
                .build();
    }

    private String getFileExtension(String filename) {
        if (filename == null || !filename.contains(".")) {
            return "bin";
        }
        return filename.substring(filename.lastIndexOf(".") + 1).toLowerCase();
    }
}