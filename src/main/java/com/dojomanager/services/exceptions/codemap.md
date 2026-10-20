# src/main/java/com/dojomanager/services/exceptions/

## Responsibility
Domain exception types thrown by the service layer to signal business-rule failures. They carry a human-readable message and are mapped to HTTP status codes by the REST-layer `@RestControllerAdvice` classes.

## Design
- All four types extend `RuntimeException` (unchecked, so `@Transactional` rolls back) and expose a single `String message` constructor.
- No Spring annotations, no status codes, and no error-body fields here — the classes are deliberately transport-agnostic; the HTTP mapping lives in `AttendanceApiExceptionHandler` and `StudentApiExceptionHandler`.
- Status contract:
  - `BadRequestException` → **400** — malformed/incomplete request (e.g. attendance check-in with neither `sessionId` nor `date`, or a `sessionId` without a `date`).
  - `ForbiddenOperationException` → **403** — authenticated principal may not perform the operation (e.g. dojo/rank belongs to another owner, no owned dojo yet, several dojos without a `dojoId`).
  - `ResourceNotFoundException` → **404** — resource missing *within the caller's scope*; also used intentionally for other owners' resources so existence is not leaked.
  - `DuplicateResourceException` → **409** — well-formed payload conflicts with existing data (e.g. duplicate student email in a dojo, duplicate attendance record).
- Javadoc on each class documents the intended semantics.

## Flow
1. A service method detects a violated rule and throws one of these exceptions.
2. The exception propagates out of the `@Transactional` service method (rolling back writes).
3. The controller-scoped `@RestControllerAdvice` matches the type and builds `{ status, error, message }` with the corresponding `HttpStatus`.
4. Database-level conflicts that bypass service checks (`DataIntegrityViolationException`) are handled separately by the same advices as 409.

## Integration
- **Thrown by:** `StudentService` (`DuplicateResourceException`, `ForbiddenOperationException`, `ResourceNotFoundException`) and `AttendanceService` (all four types: `BadRequestException`, `DuplicateResourceException`, `ForbiddenOperationException`, `ResourceNotFoundException`).
- **Consumed by:** `com.dojomanager.rest.v1.attendance.AttendanceApiExceptionHandler` (all four + `DataIntegrityViolationException`) and `com.dojomanager.rest.v1.students.StudentApiExceptionHandler` (`DuplicateResourceException`, `ForbiddenOperationException`, `ResourceNotFoundException` + `DataIntegrityViolationException`). Legacy services (`DojoService`, `DojoOwnerService`, `RankService`, `ScheduleService`, `AddressService`) do not use these types.
