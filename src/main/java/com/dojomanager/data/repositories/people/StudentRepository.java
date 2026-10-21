package com.dojomanager.data.repositories.people;

import java.util.List;
import java.util.Optional;

import com.dojomanager.data.entities.people.Student;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

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

    /**
     * Removes the join rows for a schedule that is being deleted. The join table
     * has no entity of its own, so this is a native statement that works on both
     * H2 (MODE=MySQL) and MySQL.
     */
    @Modifying
    @Query(value = "DELETE FROM student_schedule_enrollment WHERE schedule_id = :scheduleId", nativeQuery = true)
    void deleteEnrollmentsByScheduleId(@Param("scheduleId") Long scheduleId);
}
