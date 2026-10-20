# src/main/java/com/dojomanager/rest/v1/

## Responsibility
REST API version 1 — the domain-grouped HTTP controllers that back the DojoManager frontend.

## Design
- One subpackage per bounded area, each containing its controller plus (where applicable) a controller-scoped exception handler:
  - `attendance/` — owner-scoped attendance check-in and query endpoints (`/api/v1/attendance`).
  - `authorization/` — public login/signup endpoints that issue JWTs (`/api/auth`).
  - `dojo/` — dojo management, authenticated owner profile, and weekly-schedule endpoints (`/api/v1/dojos`, `/api/v1/owner`, `/api/v1/schedules`); normalized to the v1 convention with principal-derived identity and DTO responses (schedule service logic itself remains partly legacy).
  - `students/` — owner-scoped student CRUD, soft deactivation and attendance history (`/api/v1/students`).
- Versioning convention: the `v1` package corresponds to the `/api/v1` URL prefix declared in `@RequestMapping`/`@GetMapping`. `authorization` is intentionally unversioned (`/api/auth`); the `dojo` package now follows the `/api/v1` prefix uniformly.
- Newer controllers use `@RestController` + `@CrossOrigin(origins = "http://localhost:4200")` + Lombok `@RequiredArgsConstructor` and take the caller via `@AuthenticationPrincipal OwnerPrincipal`; older ones read `SecurityContextHolder` directly or use field injection.
- Input binding uses `@Valid @RequestBody` forms/DTOs; responses are `ResponseEntity<Dto>` / DTO lists.
- Error contracts are provided by per-controller `@RestControllerAdvice(assignableTypes = ...)` classes in `attendance/` and `students/`.

## Flow
HTTP request with `Authorization: Bearer <jwt>` → `JwtAuthenticationTokenFilter` validates the token and loads an `OwnerPrincipal` into the security context → matching controller method in this package → service call that applies owner scoping → DTO serialized as JSON. Requests to `/api/auth/**` skip the filter's requirement and are handled by `AuthorizationController`.

## Integration
- Services: `AttendanceService`, `StudentService`, `DojoOwnerService`, `DojoService`, `ScheduleService`, `RankService`.
- Inbound payloads: `AttendanceCheckInForm`, `StudentRequestForm`, `LoginForm`, `SignupForm`, `DojoUpdateRequest`, `ScheduleSessionDTO`.
- Outbound payloads: `AttendanceRecordDto`, `StudentDto`, `JwtResponse`, `MessageResponse`, `DojoResponse`, `ScheduleSessionDTO`, `Map<String, List<WeeklySchedule>>`.
- Security: `WebSecurityConfig` permits `/api/auth/**` and requires authentication for everything else; `JwtProvider` mints tokens, `PasswordEncoder` hashes signup passwords.
