# src/main/java/com/dojomanager/data/forms/attendance/

## Responsibility

Form Layer — inbound check-in body for the attendance API. Describes one student's attendance entry, optionally tied to a scheduled session.

## Design

- `AttendanceCheckInForm` — Lombok `@Getter`/`@Setter`/`@NoArgsConstructor`; no `@Data`, no builders.
- Fields:
  - `studentId` (`Long`, `@NotNull(message = "studentId is required")`).
  - `status` (`AttendanceStatus` enum, `@NotNull(message = "status is required")`).
  - `sessionId` (`Long`, optional).
  - `date` (`LocalDate`, optional).
- The class Javadoc states the cross-field invariant: provide either `sessionId` (which always requires a `date`) or a `date` alone. This is not expressible with the annotations present, so `AttendanceService.checkOne` enforces it: neither → 400 "A sessionId or date is required"; `sessionId` without `date` → 400 "A sessionId requires a date".
- Ownership is not a field: the student must belong to a dojo owned by the authenticated principal, checked in `AttendanceService.requireOwnedStudent`.

## Flow

1. `POST /api/v1/attendance` binds the JSON body to `AttendanceCheckInForm` (controller null-checks the body, then wraps it in `List.of(form)`).
2. `AttendanceService.checkIn(principal, forms)` resolves the owner, then per form `checkOne`: validates the student/dojo ownership, resolves the optional `DailySchedule`, verifies the session belongs to the student's dojo, rejects duplicates by `(studentId, sessionId, sessionDate)`, and saves an `AttendanceRecord` (`status`, `sessionDate`, `recordedBy = owner.id`, timestamps).
3. The saved record is mapped through `AttendanceRecordDto.from` and returned as `List<AttendanceRecordDto>` with `201 Created`.

## Integration

- Consumed by `com.dojomanager.rest.v1.attendance.AttendanceController` (`POST /api/v1/attendance`).
- Used by `com.dojomanager.services.AttendanceService` (`checkIn`, `checkOne`).
- Maps to `com.dojomanager.data.entities.attendance.AttendanceRecord` / `AttendanceStatus`, `com.dojomanager.data.entities.people.Student`, and `com.dojomanager.data.entities.schedule.DailySchedule`.
