# src/main/java/com/dojomanager/rest/v1/attendance/

## Responsibility
REST API v1 — attendance endpoints: create check-ins, query records by session/date, and fetch one record. All operations are scoped to the authenticated dojo owner.

## Design
- `AttendanceController` — `@RestController`, `@RequestMapping(path = "/api/v1/attendance")`, `@CrossOrigin(origins = "http://localhost:4200", maxAge = 3600)`, Lombok `@RequiredArgsConstructor` constructor-injecting `AttendanceService`.
- The owner is taken from `@AuthenticationPrincipal OwnerPrincipal`; it is never read from the payload. `AttendanceService` enforces owner scope and rejects cross-owner access.
- Endpoint methods, relative to the class mapping:
  - `POST /api/v1/attendance` → `checkIn(...)`, body `AttendanceCheckInForm`.
  - `GET /api/v1/attendance?sessionId=&date=` → `list(...)`; at least one parameter is required, and `sessionId` requires `date`.
  - `GET /api/v1/attendance/{id}` → `getById(...)`.
- Controller-level guards throw `BadRequestException` for a missing body, for neither `sessionId` nor `date`, and for `sessionId` without `date`.
- `AttendanceApiExceptionHandler` is a `@RestControllerAdvice(assignableTypes = AttendanceController.class)` mapping `ResourceNotFoundException` → 404, `ForbiddenOperationException` → 403, `DuplicateResourceException` → 409, `BadRequestException` → 400, `DataIntegrityViolationException` → 409, all as `{status, error, message}`.

## Flow
- `POST /api/v1/attendance` + `AttendanceCheckInForm` → `attendanceService.checkIn(principal, List.of(form))` → `201 Created` with `List<AttendanceRecordDto>` (the service returns a list even for a single check-in).
- `GET /api/v1/attendance?sessionId=X&date=Y` → `attendanceService.findBySession(principal, sessionId, date)` → `200 OK` with `List<AttendanceRecordDto>`.
- `GET /api/v1/attendance?date=Y` → `attendanceService.findByDate(principal, date)` → `200 OK` with `List<AttendanceRecordDto>`.
- `GET /api/v1/attendance/{id}` → `attendanceService.getById(principal, id)` → `200 OK` with `AttendanceRecordDto`.

## Integration
- Service: `com.dojomanager.services.AttendanceService` (`checkIn`, `findBySession`, `findByDate`, `getById`).
- Input: `com.dojomanager.data.forms.attendance.AttendanceCheckInForm` (`studentId` required, `status` required, optional `sessionId`, optional `date`).
- Output: `com.dojomanager.data.dto.attendance.AttendanceRecordDto`.
- Caller identity: `com.dojomanager.security.services.OwnerPrincipal`, populated by the JWT filter; route requires authentication per `WebSecurityConfig` (`anyRequest().authenticated()`), with ownership enforced in the service layer.
- Exceptions: `com.dojomanager.services.exceptions.*`, translated by `AttendanceApiExceptionHandler`.
