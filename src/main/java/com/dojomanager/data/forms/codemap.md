# src/main/java/com/dojomanager/data/forms/

## Responsibility

Form layer root — inbound request bodies with bean-validation constraints, validated at the REST boundary. These objects never reach the persistence layer directly; services translate them into entities.

## Design

Direct classes:

- `LoginForm` — Lombok `@Data`. `email` (`@NotBlank`, `@Size(min = 3, max = 60)` — note: no `@Email` here), `password` (`@NotBlank`, `@Size(min = 6, max = 128)`).
- `SignupForm` — Lombok `@Data`. `firstName` / `lastName` (`@NotBlank`, `@Size(min = 3)`), `email` (`@NotBlank`, `@Email`), `password` (`@NotBlank`, `@Size(min = 6, max = 128)`).
- Subpackages group feature-specific forms:
  - `attendance/` — `AttendanceCheckInForm`, a single check-in request.
  - `student/` — `StudentRequestForm`, the create/update payload for students.

Design conventions: Lombok `@Data` (or `@Getter`/`@Setter` + `@NoArgsConstructor` in the attendance form), `jakarta.validation.constraints` annotations only, no JPA/entity references, and no ownership fields taken from the client — the authenticated `OwnerPrincipal` supplies the owner. Cross-field rules that bean validation cannot express (e.g. "sessionId requires date", rank/dojo ownership) are enforced in the services.

## Flow

1. Controller binds JSON with `@Valid @RequestBody` (`AuthorizationController` for `LoginForm`/`SignupForm`; `StudentController` for `StudentRequestForm`; `AttendanceController` binds `AttendanceCheckInForm` without `@Valid` and null-checks manually).
2. Validation failures are raised before the controller body runs; controller-scoped advice handles them (`StudentApiExceptionHandler` maps `MethodArgumentNotValidException` to 400; `AuthorizationController` catches its own exceptions into `MessageResponse`).
3. Controllers pass the form to a service: `AuthenticationManager`/`DojoOwnerService` for auth, `StudentService` for students, `AttendanceService` for attendance.
4. Services map form fields onto entities (`DojoOwner`, `Student`, `StudentRank`, `AttendanceRecord`) and return DTOs from `data/dto/`.

## Integration

- `com.dojomanager.rest.v1.authorization.AuthorizationController` — `/api/auth/login` (`LoginForm`), `/api/auth/signup` (`SignupForm`).
- `com.dojomanager.rest.v1.students.StudentController` — `POST /api/v1/students`, `PUT /api/v1/students/{id}` (`StudentRequestForm`).
- `com.dojomanager.rest.v1.attendance.AttendanceController` — `POST /api/v1/attendance` (`AttendanceCheckInForm`).
- Services: `com.dojomanager.services.StudentService`, `AttendanceService`, `DojoOwnerService`; security: `AuthenticationManager`, `PasswordEncoder`, `JwtProvider`.
