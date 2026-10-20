# src/main/java/com/dojomanager/data/dto/

## Responsibility

DTO layer root — outbound API payload shapes and the small generic response envelope used by controllers. No inbound request bodies live here (those are in `data/forms/`); this package and its subpackages define what the REST API returns.

## Design

Direct classes:

- `MessageResponse` — generic message/error envelope (Lombok `@Data`, `@NoArgsConstructor`). Fields: `message`, `error`. Extra constructor `MessageResponse(String message)` for success text. Used for auth failures where a full payload is not appropriate (e.g. duplicate email, login exception).
- Subpackages group DTOs by feature:
  - `attendance/` — `AttendanceRecordDto`, a flattened attendance read model built from the `AttendanceRecord` entity.
  - `authorization/` — `JwtResponse`, the bearer-token login/signup response.
  - `dojo/` — `DojoResponse`, `DojoUpdateRequest`, `OwnerDTO` for dojo and owner payloads.
  - `schedule/` — `ScheduleSessionDTO`, a string-typed view of a weekly schedule slot.
  - `student/` — `StudentDto`, an owner-scoped, flattened student read model.

Conventions are mixed: newer DTOs use Lombok and static `from(entity)` mappers; older ones (`DojoResponse`, `DojoUpdateRequest`, `ScheduleSessionDTO`, `JwtResponse`) use hand-written constructors/accessors. Request validation annotations are deliberately absent here — this layer is serialization-only; only `forms/` carries `jakarta.validation` constraints.

## Flow

1. Controller receives a form/request object or path/query parameters (see `data/forms/`).
2. Service loads entities, enforces owner scoping, and converts results to a DTO (`AttendanceRecordDto.from`, `StudentService.toDto`, or by constructing `JwtResponse`/`DojoResponse`/`ScheduleSessionDTO` directly).
3. Controller wraps the DTO in a `ResponseEntity` (or returns it with `@ResponseBody`) and Jackson serializes it to JSON.
4. Error paths return `MessageResponse` with the `error` field set, or are handled by the service exception layer.

## Integration

- `com.dojomanager.rest.v1.authorization.AuthorizationController` — returns `JwtResponse` and `MessageResponse`.
- `com.dojomanager.rest.v1.attendance.AttendanceController`, `...students.StudentController` — return `AttendanceRecordDto` / `StudentDto`.
- `com.dojomanager.rest.v1.dojo.DojoController`, `...dojo.ScheduleController` — return `DojoResponse`, `DojoUpdateRequest`, `ScheduleSessionDTO`.
- Mapped from entities in `com.dojomanager.data.entities.*` via `AttendanceService`, `StudentService`, `ScheduleService`, and inline in controllers.
- Note: `DojoResponse` serializes JPA `Dojo` entities directly, unlike the flattened DTOs elsewhere in this package.
