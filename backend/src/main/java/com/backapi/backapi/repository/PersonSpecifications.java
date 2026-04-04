package com.backapi.backapi.repository;

import com.backapi.backapi.entity.Person;
import com.backapi.backapi.enums.PersonStatus;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;

/**
 * JPA Specifications for dynamic Person queries.
 * Supports: search (fullName, region, charge) and filter (region, year, charge).
 */
public class PersonSpecifications {

    private PersonSpecifications() {}

    /**
     * Поиск: ищет query в fullName, region ИЛИ charge (OR-логика).
     */
    public static Specification<Person> search(String query) {
        return (root, cq, cb) -> {
            String pattern = "%" + query.toLowerCase() + "%";
            return cb.or(
                    cb.like(cb.lower(root.get("fullName")), pattern),
                    cb.like(cb.lower(root.get("region")), pattern),
                    cb.like(cb.lower(root.get("charge")), pattern)
            );
        };
    }

    /**
     * Фильтрация по region (точное совпадение, case-insensitive).
     */
    public static Specification<Person> hasRegion(String region) {
        return (root, cq, cb) ->
                cb.equal(cb.lower(root.get("region")), region.toLowerCase());
    }

    /**
     * Фильтрация по году (birthYear <= year <= deathYear, либо точное birthYear).
     */
    public static Specification<Person> hasYear(Integer year) {
        return (root, cq, cb) ->
                cb.or(
                        cb.equal(root.get("birthYear"), year),
                        cb.equal(root.get("deathYear"), year)
                );
    }

    /**
     * Фильтрация по статье обвинения (содержит подстроку).
     */
    public static Specification<Person> hasCharge(String charge) {
        return (root, cq, cb) ->
                cb.like(cb.lower(root.get("charge")), "%" + charge.toLowerCase() + "%");
    }

    /**
     * Только с определённым статусом модерации.
     */
    public static Specification<Person> hasModerationStatus(PersonStatus status) {
        return (root, cq, cb) ->
                cb.equal(root.get("moderationStatus"), status);
    }

    /**
     * Собирает комбинированный фильтр из параметров.
     * search — OR по fullName/region/charge.
     * Остальные — AND-фильтры.
     */
    public static Specification<Person> buildSpec(
            String query,
            String region,
            Integer year,
            String charge,
            PersonStatus moderationStatus) {

        List<Specification<Person>> specs = new ArrayList<>();

        if (moderationStatus != null) {
            specs.add(hasModerationStatus(moderationStatus));
        }
        if (query != null && !query.isBlank()) {
            specs.add(search(query));
        }
        if (region != null && !region.isBlank()) {
            specs.add(hasRegion(region));
        }
        if (year != null) {
            specs.add(hasYear(year));
        }
        if (charge != null && !charge.isBlank()) {
            specs.add(hasCharge(charge));
        }

        if (specs.isEmpty()) {
            return Specification.where(null);
        }

        Specification<Person> result = specs.get(0);
        for (int i = 1; i < specs.size(); i++) {
            result = result.and(specs.get(i));
        }
        return result;
    }
}
