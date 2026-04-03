package com.backapi.backapi.repository;

import com.backapi.backapi.entity.Document;
import com.backapi.backapi.entity.User;
import com.backapi.backapi.enums.DocumentStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DocumentRepository extends JpaRepository<Document, Long> {

    Page<Document> findByUploadedBy(User user, Pageable pageable);

    Page<Document> findByStatus(DocumentStatus status, Pageable pageable);

    Page<Document> findByStatusOrUploadedBy(DocumentStatus status, User user, Pageable pageable);
}
