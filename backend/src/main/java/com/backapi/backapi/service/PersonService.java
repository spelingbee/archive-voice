package com.backapi.backapi.service;

import com.backapi.backapi.dto.request.PersonCreateRequest;
import com.backapi.backapi.dto.response.DocumentResponse;
import com.backapi.backapi.dto.response.PersonResponse;
import com.backapi.backapi.entity.Document;
import com.backapi.backapi.entity.Person;
import com.backapi.backapi.entity.User;
import com.backapi.backapi.enums.PersonStatus;
import com.backapi.backapi.repository.PersonRepository;
import com.backapi.backapi.repository.PersonSpecifications;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class PersonService {

    private final PersonRepository personRepository;
    private final PersonSearchService personSearchService;

    // ─── Публичные списки (только APPROVED) ─────────────────────────

    public Page<PersonResponse> getAllApprovedPersons(Pageable pageable) {
        return personRepository.findByModerationStatus(PersonStatus.APPROVED, pageable)
                .map(this::mapToResponse);
    }

    /**
     * Поиск + фильтрация (публичный, только APPROVED).
     *
     * Если query передан — используется нечёткий поиск (Триграм + Левенштейн + Jaro-Winkler).
     * Дополнительные фильтры (region, year, charge) применяются сверху.
     */
    public Page<PersonResponse> searchAndFilter(
            String query, String region, Integer year, String charge, Pageable pageable) {

        if (query != null && !query.isBlank()) {
            // 1. Получаем ID отсортированные по fuzzy-релевантности
            List<Long> fuzzyIds = personSearchService.fuzzySearchIds(query);

            if (fuzzyIds.isEmpty()) {
                return Page.empty(pageable);
            }

            // 2. Применяем доп. фильтры через Specification + ограничиваем по fuzzy ID
            Specification<Person> spec = PersonSpecifications.buildSpec(
                    null, region, year, charge, PersonStatus.APPROVED);
            spec = spec.and((root, cq, cb) -> root.get("id").in(fuzzyIds));

            List<Person> filtered = personRepository.findAll(spec);

            // 3. Сохраняем порядок fuzzy-релевантности
            Map<Long, Integer> idOrder = new java.util.LinkedHashMap<>();
            for (int i = 0; i < fuzzyIds.size(); i++) {
                idOrder.put(fuzzyIds.get(i), i);
            }
            filtered.sort(java.util.Comparator.comparingInt(p -> idOrder.getOrDefault(p.getId(), Integer.MAX_VALUE)));

            // 4. Пагинация вручную
            int start = (int) pageable.getOffset();
            int end = Math.min(start + pageable.getPageSize(), filtered.size());
            List<PersonResponse> pageContent = filtered.subList(
                    Math.min(start, filtered.size()),
                    Math.min(end, filtered.size())
            ).stream().map(this::mapToResponse).toList();

            return new PageImpl<>(pageContent, pageable, filtered.size());
        }

        // Без query — обычная фильтрация через Specification
        Specification<Person> spec = PersonSpecifications.buildSpec(
                null, region, year, charge, PersonStatus.APPROVED);
        return personRepository.findAll(spec, pageable).map(this::mapToResponse);
    }

    // ─── Мои записи (текущий пользователь) ──────────────────────────

    public Page<PersonResponse> getMyPersons(User user, Pageable pageable) {
        return personRepository.findByModeratedBy(user, pageable)
                .map(this::mapToResponse);
    }

    // ─── Модератор ──────────────────────────────────────────────────

    public Page<PersonResponse> getAllPersons(Pageable pageable) {
        return personRepository.findAll(pageable).map(this::mapToResponse);
    }

    public Page<PersonResponse> getPersonsByStatus(PersonStatus status, Pageable pageable) {
        return personRepository.findByModerationStatus(status, pageable)
                .map(this::mapToResponse);
    }

    // ─── Получение по ID (с документами) ────────────────────────────

    public PersonResponse getPersonById(Long id) {
        Person person = personRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Person не найден с id: " + id));
        return mapToResponse(person);
    }

    // ─── Создание ───────────────────────────────────────────────────

    @Transactional
    public PersonResponse createPerson(PersonCreateRequest request, User currentUser) {
        Person person = Person.builder()
                .fullName(request.getFullName())
                .birthYear(request.getBirthYear())
                .deathYear(request.getDeathYear())
                .region(request.getRegion())
                .district(request.getDistrict())
                .occupation(request.getOccupation())
                .charge(request.getCharge())
                .arrestDate(request.getArrestDate())
                .sentence(request.getSentence())
                .sentenceDate(request.getSentenceDate())
                .rehabilitationDate(request.getRehabilitationDate())
                .biography(request.getBiography())
                .source(request.getSource())
                .status("unverified")
                .moderationStatus(PersonStatus.PENDING)
                .moderatedBy(currentUser)
                .build();

        Person saved = personRepository.save(person);
        return mapToResponse(saved);
    }

    // ─── Модерация ──────────────────────────────────────────────────

    @Transactional
    public PersonResponse approve(Long id) {
        Person person = personRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Person не найден с id: " + id));
        person.setModerationStatus(PersonStatus.APPROVED);
        person.setRejectionReason(null);
        return mapToResponse(personRepository.save(person));
    }

    @Transactional
    public PersonResponse reject(Long id, String reason) {
        Person person = personRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Person не найден с id: " + id));
        person.setModerationStatus(PersonStatus.REJECTED);
        person.setRejectionReason(reason);
        return mapToResponse(personRepository.save(person));
    }

    // ─── Маппинг ────────────────────────────────────────────────────

    private PersonResponse mapToResponse(Person person) {
        List<DocumentResponse> docResponses = person.getDocuments().stream()
                .map(this::mapDocToResponse)
                .toList();

        return PersonResponse.builder()
                .id(person.getId())
                .fullName(person.getFullName())
                .birthYear(person.getBirthYear())
                .deathYear(person.getDeathYear())
                .region(person.getRegion())
                .district(person.getDistrict())
                .occupation(person.getOccupation())
                .charge(person.getCharge())
                .arrestDate(person.getArrestDate())
                .sentence(person.getSentence())
                .sentenceDate(person.getSentenceDate())
                .rehabilitationDate(person.getRehabilitationDate())
                .biography(person.getBiography())
                .source(person.getSource())
                .status(person.getStatus())
                .moderationStatus(person.getModerationStatus())
                .createdByEmail(person.getModeratedBy() != null ? person.getModeratedBy().getEmail() : null)
                .createdAt(person.getCreatedAt())
                .rejectionReason(person.getRejectionReason())
                .documents(docResponses)
                .build();
    }

    private DocumentResponse mapDocToResponse(Document doc) {
        String relativePath = doc.getFilePath().replace("\\", "/");
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
}
