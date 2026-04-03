package com.backapi.backapi.service;

import com.backapi.backapi.dto.request.PersonCreateRequest;
import com.backapi.backapi.dto.response.PersonResponse;
import com.backapi.backapi.entity.Person;
import com.backapi.backapi.repository.PersonRepository;
import jakarta.transaction.Transactional;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;



@Service
@RequiredArgsConstructor
public class PersonService {

    private final PersonRepository personRepository;

    public Page<PersonResponse> getAllPersons(Pageable pageable) {
        return personRepository.findAll(pageable).map(this::mapToResponse);
    }

    public Page<PersonResponse> searchPersons(String fullName, Pageable pageable) {
        return personRepository.findByFullNameContainingIgnoreCase(fullName, pageable)
                .map(this::mapToResponse);
    }

    public PersonResponse getPersonById(Long id) {
        Person person = personRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Person не найден с id: " + id));
        return mapToResponse(person);
    }

    private PersonResponse mapToResponse(Person person) {
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
                .build();
    }

    // Добавь этот метод в PersonService
    @Transactional
    public PersonResponse createPerson(PersonCreateRequest request) {
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
                .status("unverified")           // по умолчанию
                .build();

        Person saved = personRepository.save(person);
        return mapToResponse(saved);
    }
}
