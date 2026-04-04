package com.backapi.backapi.controller;

import com.backapi.backapi.dto.request.PersonCreateRequest;
import com.backapi.backapi.dto.response.ApiResponseWrapper;
import com.backapi.backapi.dto.response.PaginatedResponse;
import com.backapi.backapi.dto.response.PersonResponse;
import com.backapi.backapi.entity.User;
import com.backapi.backapi.enums.PersonStatus;
import com.backapi.backapi.service.PersonService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/persons")
@RequiredArgsConstructor
@Tag(name = "Persons", description = "Работа с личностями репрессированных")
public class PersonController {

    private final PersonService personService;

    // ─── Публичные эндпоинты (только APPROVED) ──────────────────────

    @Operation(summary = "Получить всех одобренных личностей с пагинацией")
    @GetMapping
    public ResponseEntity<ApiResponseWrapper<PaginatedResponse<PersonResponse>>> getAllPersons(Pageable pageable) {
        PaginatedResponse<PersonResponse> page = PaginatedResponse.from(personService.getAllApprovedPersons(pageable));
        return ResponseEntity.ok(ApiResponseWrapper.of(page));
    }

    @Operation(summary = "Поиск и фильтрация (только одобренные)")
    @GetMapping("/search")
    public ResponseEntity<ApiResponseWrapper<PaginatedResponse<PersonResponse>>> searchPersons(
            @RequestParam(required = false) String query,
            @RequestParam(required = false) String region,
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) String charge,
            Pageable pageable) {
        PaginatedResponse<PersonResponse> page = PaginatedResponse.from(
                personService.searchAndFilter(query, region, year, charge, pageable));
        return ResponseEntity.ok(ApiResponseWrapper.of(page));
    }

    @Operation(summary = "Получить одну личность по ID (с документами)")
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponseWrapper<PersonResponse>> getPersonById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponseWrapper.of(personService.getPersonById(id)));
    }

    // ─── Создание (любой авторизованный пользователь) ────────────────

    @Operation(summary = "Добавить новую личность (статус PENDING)")
    @PostMapping
    public ResponseEntity<ApiResponseWrapper<PersonResponse>> createPerson(
            @Valid @RequestBody PersonCreateRequest request,
            @AuthenticationPrincipal User currentUser) {
        PersonResponse response = personService.createPerson(request, currentUser);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponseWrapper.of(response, "Запись создана и отправлена на модерацию"));
    }

    // ─── Мои записи ─────────────────────────────────────────────────

    @Operation(summary = "Получить свои созданные записи")
    @GetMapping("/my")
    public ResponseEntity<ApiResponseWrapper<PaginatedResponse<PersonResponse>>> getMyPersons(
            @AuthenticationPrincipal User currentUser,
            Pageable pageable) {
        PaginatedResponse<PersonResponse> page = PaginatedResponse.from(
                personService.getMyPersons(currentUser, pageable));
        return ResponseEntity.ok(ApiResponseWrapper.of(page));
    }

    // ─── Модерация (только MODERATOR) ────────────────────────────────

    @Operation(summary = "Список записей на модерации")
    @PreAuthorize("hasRole('MODERATOR')")
    @GetMapping("/moderation")
    public ResponseEntity<ApiResponseWrapper<PaginatedResponse<PersonResponse>>> getPending(
            @RequestParam(defaultValue = "PENDING") PersonStatus status,
            Pageable pageable) {
        PaginatedResponse<PersonResponse> page = PaginatedResponse.from(
                personService.getPersonsForModeration(status, pageable));
        return ResponseEntity.ok(ApiResponseWrapper.of(page));
    }

    @Operation(summary = "Все записи (любой статус) — для модератора")
    @PreAuthorize("hasRole('MODERATOR')")
    @GetMapping("/moderation/all")
    public ResponseEntity<ApiResponseWrapper<PaginatedResponse<PersonResponse>>> getAllForModeration(Pageable pageable) {
        PaginatedResponse<PersonResponse> page = PaginatedResponse.from(
                personService.getAllPersonsForModeration(pageable));
        return ResponseEntity.ok(ApiResponseWrapper.of(page));
    }

    @Operation(summary = "Одобрить запись")
    @PreAuthorize("hasRole('MODERATOR')")
    @PatchMapping("/{id}/approve")
    public ResponseEntity<ApiResponseWrapper<PersonResponse>> approve(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponseWrapper.of(personService.approve(id), "Запись одобрена"));
    }

    @Operation(summary = "Отклонить запись")
    @PreAuthorize("hasRole('MODERATOR')")
    @PatchMapping("/{id}/reject")
    public ResponseEntity<ApiResponseWrapper<PersonResponse>> reject(
            @PathVariable Long id,
            @RequestBody Map<String, String> body) {
        String reason = body.getOrDefault("reason", "Без указания причины");
        return ResponseEntity.ok(ApiResponseWrapper.of(personService.reject(id, reason), "Запись отклонена"));
    }
}