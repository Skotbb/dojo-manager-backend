package com.dojomanager.data.repositories.people;

import java.util.List;
import java.util.Optional;

import com.dojomanager.data.entities.people.Student;

import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Every finder exposed here is scoped either by {@code dojoId} or by
 * {@code ownerId} (derived through {@code student.dojo.owner.id}). The
 * unscoped {@code JpaRepository} finders are deliberately never used by
 * {@code StudentService}, so no query can cross an owner boundary.
 */
public interface StudentRepository extends JpaRepository<Student, Long> {

    List<Student> findByDojoId(Long dojoId);

    List<Student> findByDojoOwnerId(Long ownerId);

    Optional<Student> findByIdAndDojoOwnerId(Long id, Long ownerId);

    boolean existsByDojoIdAndEmailIgnoreCase(Long dojoId, String email);
}
