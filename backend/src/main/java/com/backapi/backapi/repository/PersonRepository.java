package com.backapi.backapi.repository;

import com.backapi.backapi.entity.Person;
import com.backapi.backapi.enums.PersonStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PersonRepository extends JpaRepository<Person, Long>, JpaSpecificationExecutor<Person> {

    Page<Person> findByFullNameContainingIgnoreCase(String fullName, Pageable pageable);

    Page<Person> findByModerationStatus(PersonStatus status, Pageable pageable);

    Page<Person> findByFullNameContainingIgnoreCaseAndModerationStatus(
            String fullName, PersonStatus status, Pageable pageable);

    @Query(value = "SELECT * FROM person " +
            "WHERE fullName % :name " +
            "ORDER BY similarity(fullName, :name) DESC",
            nativeQuery = true)
    List<Person> findByFuzzyName(@Param("name") String name);

    @Query(value = "SELECT * FROM person " +
            "WHERE levenshtein(LOWER(full_name), LOWER(:name)) <= 2 " +
            "ORDER BY levenshtein(full_name, :name) ASC",
            nativeQuery = true)
    List<Person> findWithTypos(@Param("name") String name);

    Page<Person> findByModeratedBy(com.backapi.backapi.entity.User user, Pageable pageable);
}
