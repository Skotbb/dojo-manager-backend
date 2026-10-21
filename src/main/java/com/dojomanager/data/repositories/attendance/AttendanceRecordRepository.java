package com.dojomanager.data.repositories.attendance;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

import com.dojomanager.data.entities.attendance.AttendanceRecord;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Persistence for {@link AttendanceRecord}.
 *
 * <p>Every finder is scoped either by {@code sessionId} or by
 * {@code ownerId} (derived through {@code record.dojo.owner.id}) so that no
 * query can cross an owner boundary. The unscoped {@code JpaRepository}
 * finders are deliberately never used by {@code AttendanceService}.</p>
 */
@Repository
public interface AttendanceRecordRepository extends JpaRepository<AttendanceRecord, Long> {

    Optional<AttendanceRecord> findByStudentIdAndSessionIdAndSessionDate(
            Long studentId, Long sessionId, LocalDate sessionDate);

    List<AttendanceRecord> findBySessionIdAndSessionDate(Long sessionId, LocalDate sessionDate);

    List<AttendanceRecord> findBySessionIdIn(Collection<Long> sessionIds);

    List<AttendanceRecord> findBySessionDateAndDojoOwnerId(LocalDate sessionDate, Long ownerId);

    List<AttendanceRecord> findByStudentIdAndDojoOwnerId(Long studentId, Long ownerId);
}
