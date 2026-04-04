package com.backapi.backapi.service;

import com.backapi.backapi.chat.RagService;
import com.backapi.backapi.dto.request.PersonCreateRequest;
import com.backapi.backapi.dto.response.DocumentResponse;
import com.backapi.backapi.dto.response.DuplicateCandidateResponse;
import com.backapi.backapi.dto.response.PersonResponse;
import com.backapi.backapi.entity.Document;
import com.backapi.backapi.entity.Person;
import com.backapi.backapi.entity.PersonDuplicate;
import com.backapi.backapi.entity.User;
import com.backapi.backapi.enums.PersonStatus;
import com.backapi.backapi.repository.PersonDuplicateRepository;
import com.backapi.backapi.repository.PersonRepository;
import com.backapi.backapi.repository.PersonSpecifications;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class PersonService {

    private final PersonRepository personRepository;
    private final PersonSearchService personSearchService;
    private final PersonDuplicateRepository personDuplicateRepository;
    private final RagService ragService;

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
        Page<Person> personPage = personRepository.findByModeratedBy(user, pageable);

        List<Long> personIds = personPage.getContent().stream()
                .map(Person::getId)
                .toList();
        List<PersonDuplicate> allDuplicates = personDuplicateRepository.findByPersonIdIn(personIds);

        Map<Long, List<PersonDuplicate>> duplicatesByPersonId = allDuplicates.stream()
                .collect(Collectors.groupingBy(d -> d.getPerson().getId()));

        return personPage.map(person -> {
            PersonResponse response = mapToResponse(person);
            List<PersonDuplicate> dups = duplicatesByPersonId.getOrDefault(person.getId(), List.of());
            response.setDuplicates(dups.stream().map(this::mapDuplicateToResponse).toList());
            return response;
        });
    }

    // ─── Модератор ──────────────────────────────────────────────────

    public Page<PersonResponse> getAllPersons(Pageable pageable) {
        return personRepository.findAll(pageable).map(this::mapToResponse);
    }

    public Page<PersonResponse> getPersonsByStatus(PersonStatus status, Pageable pageable) {
        return personRepository.findByModerationStatus(status, pageable)
                .map(this::mapToResponse);
    }

    /**
     * Для модерации: загружает персоны и подтягивает информацию о дублях.
     */
    public Page<PersonResponse> getPersonsForModeration(PersonStatus status, Pageable pageable) {
        Page<Person> personPage = personRepository.findByModerationStatus(status, pageable);

        // Batch-подгрузка дублей для всех персон на странице
        List<Long> personIds = personPage.getContent().stream()
                .map(Person::getId)
                .toList();
        List<PersonDuplicate> allDuplicates = personDuplicateRepository.findByPersonIdIn(personIds);

        // Группируем дубли по person_id
        Map<Long, List<PersonDuplicate>> duplicatesByPersonId = allDuplicates.stream()
                .collect(Collectors.groupingBy(d -> d.getPerson().getId()));

        return personPage.map(person -> {
            PersonResponse response = mapToResponse(person);
            List<PersonDuplicate> dups = duplicatesByPersonId.getOrDefault(person.getId(), List.of());
            response.setDuplicates(dups.stream().map(this::mapDuplicateToResponse).toList());
            return response;
        });
    }

    /**
     * Для модерации: все статусы + дубли.
     */
    public Page<PersonResponse> getAllPersonsForModeration(Pageable pageable) {
        Page<Person> personPage = personRepository.findAll(pageable);

        List<Long> personIds = personPage.getContent().stream()
                .map(Person::getId)
                .toList();
        List<PersonDuplicate> allDuplicates = personDuplicateRepository.findByPersonIdIn(personIds);

        Map<Long, List<PersonDuplicate>> duplicatesByPersonId = allDuplicates.stream()
                .collect(Collectors.groupingBy(d -> d.getPerson().getId()));

        return personPage.map(person -> {
            PersonResponse response = mapToResponse(person);
            List<PersonDuplicate> dups = duplicatesByPersonId.getOrDefault(person.getId(), List.of());
            response.setDuplicates(dups.stream().map(this::mapDuplicateToResponse).toList());
            return response;
        });
    }

    // ─── Получение по ID (с документами) ────────────────────────────

    public PersonResponse getPersonById(Long id) {
        Person person = personRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Person не найден с id: " + id));
        PersonResponse response = mapToResponse(person);
        List<PersonDuplicate> dups = personDuplicateRepository.findByPersonIdIn(List.of(person.getId()));
        response.setDuplicates(dups.stream().map(this::mapDuplicateToResponse).toList());
        return response;
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

        // Асинхронно вычисляем дубли (не блокируем ответ при ошибке)
        try {
            computeAndStoreDuplicates(saved);
        } catch (Exception e) {
            log.warn("Ошибка при вычислении дублей для Person {}: {}", saved.getId(), e.getMessage());
        }

        return mapToResponse(saved);
    }

    // ─── Поиск дублей ───────────────────────────────────────────────

    /**
     * Вычисляет дубли для персоны комбинируя fuzzy-поиск и embedding-similarity.
     * Результаты сохраняются в таблицу person_duplicates.
     */
    private void computeAndStoreDuplicates(Person person) {
        Map<Long, DuplicateScores> candidates = new LinkedHashMap<>();

        // 1. Fuzzy-поиск по ФИО
        List<PersonSearchService.ScoredPerson> fuzzyResults =
                personSearchService.findFuzzyDuplicates(person.getFullName(), person.getId());

        for (PersonSearchService.ScoredPerson sp : fuzzyResults) {
            candidates.computeIfAbsent(sp.person().getId(), k -> new DuplicateScores())
                    .fuzzyScore = sp.score();
        }

        // 2. Embedding-поиск через rag-service
        String personText = buildPersonText(person);
        List<Map<String, Object>> embeddingResults =
                ragService.findSimilarPersonsByEmbedding(personText, 10, person.getId());

        for (Map<String, Object> match : embeddingResults) {
            Object personIdObj = match.get("person_id");
            Object similarityObj = match.get("similarity");
            if (personIdObj == null || similarityObj == null) continue;

            Long matchId = ((Number) personIdObj).longValue();
            double similarity = ((Number) similarityObj).doubleValue();

            candidates.computeIfAbsent(matchId, k -> new DuplicateScores())
                    .embeddingScore = similarity;
        }

        // 3. Вычисляем итоговый скор и сохраняем
        List<PersonDuplicate> duplicates = new ArrayList<>();

        for (Map.Entry<Long, DuplicateScores> entry : candidates.entrySet()) {
            Long dupId = entry.getKey();
            DuplicateScores scores = entry.getValue();

            // Определяем метод
            String method;
            if (scores.fuzzyScore > 0 && scores.embeddingScore > 0) {
                method = "both";
            } else if (scores.fuzzyScore > 0) {
                method = "fuzzy";
            } else {
                method = "embedding";
            }

            // Итоговый скор: max из двух + бонус если оба нашли
            double combined = Math.max(scores.fuzzyScore * 0.6, scores.embeddingScore * 0.5);
            if ("both".equals(method)) {
                combined = Math.min(1.0, combined + 0.15);
            }

            // Порог: 0.3
            if (combined < 0.3) continue;

            // Дополнительный бонус за совпадение года рождения
            personRepository.findById(dupId).ifPresent(dup -> {
                double bonus = 0;
                if (person.getBirthYear() != null && dup.getBirthYear() != null) {
                    int diff = Math.abs(person.getBirthYear() - dup.getBirthYear());
                    if (diff == 0) bonus = 0.1;
                    else if (diff <= 3) bonus = 0.05;
                }

                double finalScore = Math.min(1.0, scores.combinedScore(method) + bonus);

                duplicates.add(PersonDuplicate.builder()
                        .person(person)
                        .duplicatePerson(dup)
                        .matchScore(finalScore)
                        .matchMethod(method)
                        .fuzzyScore(scores.fuzzyScore > 0 ? scores.fuzzyScore : null)
                        .embeddingScore(scores.embeddingScore > 0 ? scores.embeddingScore : null)
                        .build());
            });
        }

        if (!duplicates.isEmpty()) {
            personDuplicateRepository.saveAll(duplicates);
            log.info("Найдено {} дублей для Person {} ({})",
                    duplicates.size(), person.getId(), person.getFullName());
        }

        // 4. Сохраняем эмбеддинг новой персоны в rag-service
        try {
            ragService.storePersonEmbedding(person.getId(), personText);
        } catch (Exception e) {
            log.warn("Не удалось сохранить эмбеддинг для Person {}: {}", person.getId(), e.getMessage());
        }
    }

    /**
     * Строит текстовое представление персоны для эмбеддинга.
     */
    private String buildPersonText(Person person) {
        List<String> parts = new ArrayList<>();
        if (person.getFullName() != null) parts.add(person.getFullName());
        if (person.getBirthYear() != null) parts.add("родился " + person.getBirthYear());
        if (person.getDeathYear() != null) parts.add("умер " + person.getDeathYear());
        if (person.getRegion() != null) parts.add(person.getRegion());
        if (person.getOccupation() != null) parts.add("профессия: " + person.getOccupation());
        if (person.getCharge() != null) parts.add("обвинение: " + person.getCharge());
        if (person.getBiography() != null) {
            parts.add(person.getBiography().length() > 300
                    ? person.getBiography().substring(0, 300)
                    : person.getBiography());
        }
        return String.join(", ", parts);
    }

    private static class DuplicateScores {
        double fuzzyScore = 0;
        double embeddingScore = 0;

        double combinedScore(String method) {
            double combined = Math.max(fuzzyScore * 0.6, embeddingScore * 0.5);
            if ("both".equals(method)) {
                combined = Math.min(1.0, combined + 0.15);
            }
            return combined;
        }
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

    private DuplicateCandidateResponse mapDuplicateToResponse(PersonDuplicate dup) {
        Person dupPerson = dup.getDuplicatePerson();
        return DuplicateCandidateResponse.builder()
                .duplicatePersonId(dupPerson.getId())
                .fullName(dupPerson.getFullName())
                .birthYear(dupPerson.getBirthYear())
                .deathYear(dupPerson.getDeathYear())
                .region(dupPerson.getRegion())
                .occupation(dupPerson.getOccupation())
                .matchScore(dup.getMatchScore())
                .matchMethod(dup.getMatchMethod())
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
