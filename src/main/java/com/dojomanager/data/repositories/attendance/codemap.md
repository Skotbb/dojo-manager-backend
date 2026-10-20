# src/main/java/com/dojomanager/data/repositories/attendance/

## Responsibility
Data Access Layer — attendance record repositories. Provides persistence and owner-scoped lookups for `AttendanceRecord` check-ins, exposing the queries required by the attendance and student-facing APIs without allowing cross-owner data access.

## Design
`AttendanceRecordRepository` extends `JpaRepository<AttendanceRecord, Long>` and is the only interface in this package. It is annotated `@Repository` and declares no `@Query`, only Spring Data derived query methods:

- `Optional<AttendanceRecord> findByStudentIdAndSessionIdAndSessionDate(Long studentId, Long sessionId, LocalDate sessionDate)` — duplicate check for a student/session/date tuple.
- `List<AttendanceRecord> findBySessionIdAndSessionDate(Long sessionId, LocalDate sessionDate)` — roster for one scheduled session on a date.
- `List<AttendanceRecord> findBySessionDateAndDojoOwnerId(LocalDate sessionDate, Long ownerId)` — all records for an owner on a date (traverses `record.dojo.owner.id`).
- `List<AttendanceRecord> findByStudentIdAndDojoOwnerId(Long studentId, Long ownerId)` — one student's history, owner-scoped.

The entity `AttendanceRecord` (`attendance_record` table) has a unique constraint on `student_id, session_id, session_date`, `@ManyToOne` links to `Student`, `Dojo`, and optional `DailySchedule`, plus `sessionDate`, `AttendanceStatus status`, `recordedBy`, and audit timestamps. The unscoped `JpaRepository` finders are intentionally avoided by the service layer for cross-owner safety; only `findById` is used with a post-load owner check.

## Flow
`AttendanceService.checkIn` receives check-in forms plus an `OwnerPrincipal` → resolves the owner and owned student → calls `findByStudentIdAndSessionIdAndSessionDate` to reject duplicates → builds and `save`s the new `AttendanceRecord` (student, dojo, session, date, status, recordedBy, timestamps). Read flows (`findBySession`, `findByDate`, `findByStudent`) call the matching derived finder and map results to `AttendanceRecordDto`; `getById` calls `findById` and verifies the record's dojo owner before returning.

## Integration
Consumed by `com.dojomanager.services.AttendanceService` (check-in, session/date/student listings, single-record lookup), which is exposed by `rest/v1/attendance/AttendanceController` (`/api/v1/attendance`) and by `StudentController` (`GET /api/v1/students/{id}/attendance`). Manages the `AttendanceRecord` entity and reads relationships to `Student`, `Dojo`, and `DailySchedule`.
