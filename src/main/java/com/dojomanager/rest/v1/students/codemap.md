# src/main/java/com/dojomanager/rest/v1/students/

## Responsibility
REST API v1 — student management for the authenticated dojo owner: create, list, read, update, soft-deactivate, and fetch a student's attendance history.

## Design
- `StudentController` — `@RestController`, `@RequestMapping(path = "/api/v1/students")`, `@CrossOrigin(origins = "http://localhost:4200", maxAge = 3600)`, Lombok `@RequiredArgsConstructor` constructor-injecting `StudentService` and `AttendanceService`.
- Owner identity always comes from `@AuthenticationPrincipal OwnerPrincipal`, never from the payload; `StudentService` scopes every operation to that owner's dojos. `StudentRequestForm.dojoId` only selects among the caller's dojos (403 otherwise).
- Bodies are validated with Jakarta Bean Validation (`@Valid`); responses are flat DTOs, never JPA entities.
- Endpoints, relative to the class mapping:
  - `POST /api/v1/students` → `createStudent(...)`, returns `201 Created` `StudentDto`.
  - `GET /api/v1/students?dojoId=&active=` → `listStudents(...)`, optional filters.
  - `GET /api/v1/students/{id}` → `getStudent(...)`.
  - `PUT /api/v1/students/{id}` → `updateStudent(...)`.
  - `DELETE /api/v1/students/{id}` → `deactivateStudent(...)` (soft delete; rank and rank history retained).
  - `GET /api/v1/students/{id}/attendance` → `getAttendanceHistory(...)`.
- `StudentApiExceptionHandler` is a `@RestControllerAdvice(assignableTypes = StudentController.class)` mapping `ResourceNotFoundException` → 404, `ForbiddenOperationException` → 403, `DuplicateResourceException` → 409, `DataIntegrityViolationException` → 409, `HttpMessageNotReadableException` → 400, and `MethodArgumentNotValidException` → 400 with an extra `fieldErrors` map.

## Flow
- `POST /api/v1/students` + `StudentRequestForm` → `studentService.createStudent(principal, form)` → `201 Created` `StudentDto`.
- `GET /api/v1/students?dojoId=&active=` → `studentService.listStudents(principal, dojoId, active)` → `List<StudentDto>`.
- `GET /api/v1/students/{id}` → `studentService.getStudent(principal, id)` → `StudentDto`.
- `PUT /api/v1/students/{id}` + `StudentRequestForm` → `studentService.updateStudent(principal, id, form)` → `StudentDto`.
- `DELETE /api/v1/students/{id}` → `studentService.deactivateStudent(principal, id)` → `StudentDto` with `active = false`.
- `GET /api/v1/students/{id}/attendance` → `attendanceService.findByStudent(principal, id)` → `List<AttendanceRecordDto>`.

## Integration
- Services: `com.dojomanager.services.StudentService` (`createStudent`, `listStudents`, `getStudent`, `updateStudent`, `deactivateStudent`) and `com.dojomanager.services.AttendanceService` (`findByStudent`).
- Input: `com.dojomanager.data.forms.student.StudentRequestForm` (`firstName`/`lastName` required, valid `email` required, optional `rankId`, optional `dojoId`).
- Output: `com.dojomanager.data.dto.student.StudentDto` and `com.dojomanager.data.dto.attendance.AttendanceRecordDto`.
- Caller identity: `com.dojomanager.security.services.OwnerPrincipal` from the JWT filter; all routes require authentication per `WebSecurityConfig`, and ownership is enforced in `StudentService`.
