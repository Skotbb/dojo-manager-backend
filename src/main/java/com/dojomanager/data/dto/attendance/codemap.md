# src/main/java/com/dojomanager/data/dto/attendance/

## Responsibility

DTO Layer — outbound representation of a single attendance record for the attendance and student APIs. Decouples the JPA `AttendanceRecord` graph from JSON output.

## Design

- `AttendanceRecordDto` — Lombok `@Getter`/`@Setter`/`@NoArgsConstructor`, no validation annotations (response-only).
- Fields: `id`, `studentId`, `studentName`, `dojoId`, `sessionId`, `sessionDate` (`LocalDate`), `status` (`AttendanceStatus` enum), `recordedBy` (`Long` owner id), `createdAt`, `updatedAt` (`LocalDateTime`).
- Static factory `from(AttendanceRecord record)` is the single mapping point:
  - Null-safe extraction of `record.getStudent().getId()`, `record.getDojo().getId()`, `record.getSession().getId()`, so an attendance record with no `DailySchedule` session (date-only check-in) serializes `sessionId` as `null`.
  - `studentName(Student)` private helper concatenates trimmed `firstName` + `lastName`; returns `null` when both are blank.
  - Copies `sessionDate`, `status`, `recordedBy`, `createdAt`, `updatedAt` verbatim.
- Flattening avoids exposing `Student`, `Dojo`, `DailySchedule`, or the lazy `DojoOwner` aggregate in JSON. `status` is serialized as the enum name by Jackson.

## Flow

1. `AttendanceController.checkIn` accepts `AttendanceCheckInForm`, calls `AttendanceService.checkIn(principal, List.of(form))`.
2. `AttendanceService.checkOne` creates/saves an `AttendanceRecord`; `toDto` delegates to `AttendanceRecordDto.from`.
3. Query endpoints (`findBySession`, `findByDate`, `findByStudent`, `getById`) map repository results through the same factory.
4. Controller returns `List<AttendanceRecordDto>` or a single `AttendanceRecordDto` (`201 Created` for check-in).
5. `StudentController.getAttendanceHistory` reuses the same DTO for `GET /api/v1/students/{id}/attendance`.

## Integration

- Produced by `com.dojomanager.services.AttendanceService` (`checkIn`, `findBySession`, `findByDate`, `findByStudent`, `getById`, `toDto`).
- Consumed by `com.dojomanager.rest.v1.attendance.AttendanceController` and `com.dojomanager.rest.v1.students.StudentController`.
- Maps from `com.dojomanager.data.entities.attendance.AttendanceRecord` (with `people.Student`, `dojo.Dojo`, `schedule.DailySchedule`, `attendance.AttendanceStatus`).
