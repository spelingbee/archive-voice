package com.backapi.backapi.controller;

import com.backapi.backapi.dto.request.PersonCreateRequest;
import com.backapi.backapi.dto.response.PersonResponse;
import com.backapi.backapi.service.PersonService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/persons")
@RequiredArgsConstructor
@Tag(name = "Persons", description = "Работа с личностями репрессированных")
public class PersonController {

    private final PersonService personService;

    @Operation(summary = "Получить всех личностей с пагинацией")
    @GetMapping
    public ResponseEntity<Page<PersonResponse>> getAllPersons(Pageable pageable) {
        return ResponseEntity.ok(personService.getAllPersons(pageable));
    }

    @Operation(summary = "Поиск по ФИО")
    @GetMapping("/search")
    public ResponseEntity<Page<PersonResponse>> searchPersons(
            @RequestParam String fullName,
            Pageable pageable) {
        return ResponseEntity.ok(personService.searchPersons(fullName, pageable));
    }

    @Operation(summary = "Получить одну личность по ID")
    @GetMapping("/{id}")
    public ResponseEntity<PersonResponse> getPersonById(@PathVariable Long id) {
        return ResponseEntity.ok(personService.getPersonById(id));
    }

    @Operation(summary = "Добавить новую личность (заключённого)")
    @PreAuthorize("hasRole('MODERATOR')")   // только модератор может добавлять
    @PostMapping
    public ResponseEntity<PersonResponse> createPerson(@Valid @RequestBody PersonCreateRequest request) {
        PersonResponse response = personService.createPerson(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

}
