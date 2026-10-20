# src/main/java/com/dojomanager/data/entities/attendance/

## Responsibility

JPA Entity Layer — attendance persistence model. Defines a single check-in record for a student at a dojo, optionally tied to a scheduled session, plus the status enum used to classify that check-in.

## Design

- `AttendanceRecord` extends `AbstractEntity` (inherits `Long id`), annotated `@Entity` with `@Table(name = "attendance_record")`. Lombok `@Getter`, `@Setter`, `@NoArgsConstructor`.
- Table-level `@UniqueConstraint(name = "uk_attendance_record", columnNames = {"student_id", "session_id", "session_date"})` enforces one check-in per student/session/date.
- Associations, all `@ManyToOne(fetch = FetchType.LAZY)`:
  - `student` → `Student`, `@JoinColumn(name = "student_id", nullable = false)`
  - `dojo` → `Dojo`, `@JoinColumn(name = "dojo_id", nullable = false)`
  - `session` → `DailySchedule`, `@JoinColumn(name = "session_id")` (nullable — date-only check-ins have no session)
- Scalar fields: `sessionDate` (`LocalDate`, column `session_date`), `status` (`AttendanceStatus`, `@Enumerated(EnumType.STRING)`, non-null), `recordedBy` (`Long`, column `recorded_by`, non-null — the owning `DojoOwner` id, not a JPA relation), `createdAt` (`LocalDateTime`, column `created_at`, non-null, `updatable = false`), `updatedAt` (`LocalDateTime`, column `updated_at`).
- `AttendanceStatus` is a plain enum: `PRESENT`, `ABSENT`, `LATE`, documented as the outcome of a single check-in.

## Flow

`AttendanceCheckInForm` (studentId, status, optional sessionId, optional date) enters `AttendanceService.checkIn` → the service resolves the owned `Student`/`Dojo`, optionally loads the `DailySchedule`, builds an `AttendanceRecord` and sets `recordedBy = owner.getId()` plus timestamps → `AttendanceRecordRepository.save` persists it → `AttendanceRecordDto.from` flattens the entity (ids, student full name, status, timestamps) for the API response. Uniqueness is checked first via `findByStudentIdAndSessionIdAndSessionDate` and backed by the table constraint, which the REST layer maps to HTTP 409.

## Integration

- Repository: `com.dojomanager.data.repositories.attendance.AttendanceRecordRepository` (`JpaRepository<AttendanceRecord, Long>`) — finders `findByStudentIdAndSessionIdAndSessionDate`, `findBySessionIdAndSessionDate`, `findBySessionDateAndDojoOwnerId`, `findByStudentIdAndDojoOwnerId`, all owner-scoped.
- Service: `AttendanceService` creates, updates and reads records; it reaches `Student`, `Dojo` and `DailySchedule` through these associations for ownership checks.
- DTO/form: `AttendanceRecordDto` (with `from(AttendanceRecord)`), `AttendanceCheckInForm` (uses `AttendanceStatus`).
- REST: `com.dojomanager.rest.v1.attendance.AttendanceController` and its `AttendanceApiExceptionHandler`.
