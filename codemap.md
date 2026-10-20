# dojo-manager-backend — Repository Atlas

## Purpose
Spring Boot 3.5 (Java 25) REST API for managing martial-arts dojos: owners sign up, authenticate via JWT, and manage dojos, students, ranks, schedules, and attendance. Layered monolith: REST controllers → services → Spring Data JPA repositories → MySQL (H2 for tests).

## Root Assets
| File | Role |
|------|------|
| `pom.xml` | Maven build; Spring Boot parent 3.5.16, Java 25; deps: spring-security, jjwt 0.9 (+ jaxb-api shim), data-jpa, mysql-connector-j, validation, lombok, guava; Maven profiles `local`/`dev`/`prod`/`test` drive `application-*.properties` |
| `mvnw`, `mvnw.cmd`, `.mvn/` | Maven wrapper |
| `docker/` | Container support |
| `.tool-versions` | asdf toolchain pinning |
| `src/main/resources/` | Profile-based `application*.properties` (DB config per environment) |
| `src/test/` | Integration/data tests (excluded from this map) |

## Architecture Summary
- **Auth**: stateless JWT filter chain. `/api/auth/**` is permitAll; everything else requires a Bearer token. `JwtProvider` (HS512), `JwtAuthenticationTokenFilter` populates `SecurityContext` with `OwnerPrincipal`, `JwtAuthenticationEntryPoint` returns 401.
- **Ownership model**: every business operation is scoped to the authenticated `DojoOwner`. Cross-owner access is reported as 404 (`ResourceNotFoundException`) to avoid leaking resource existence.
- **API versioning**: modern endpoints live under `/api/v1/...`; the auth controller uses legacy `/api/auth`. The dojo/owner/schedule controllers were normalized to `/api/v1/dojos`, `/api/v1/owner`, and `/api/v1/schedules` with principal-derived identity and DTO-only responses (documented in the dojo REST map).
- **Exceptions**: unchecked domain exceptions (`BadRequest` 400, `ForbiddenOperation` 403, `ResourceNotFound` 404, `DuplicateResource` 409) translated to JSON by per-controller `@RestControllerAdvice` handlers.

## Repository Directory Map
| Directory | Responsibility | Link |
|-----------|----------------|------|
| `src/main/java/com/dojomanager/dojomanager/` | Application bootstrap — `DojomanagerApplication` with explicit component/entity/repository scans | [View Map](src/main/java/com/dojomanager/dojomanager/codemap.md) |
| `src/main/java/com/dojomanager/rest/` | REST API root (versioned under `v1/`) | [View Map](src/main/java/com/dojomanager/rest/codemap.md) |
| `src/main/java/com/dojomanager/rest/v1/` | API v1 overview — attendance, authorization, dojo, students subpackages (`/api/v1` convention) | [View Map](src/main/java/com/dojomanager/rest/v1/codemap.md) |
| `src/main/java/com/dojomanager/rest/v1/attendance/` | `AttendanceController` (`/api/v1/attendance`) — check-in/history endpoints + scoped exception handler | [View Map](src/main/java/com/dojomanager/rest/v1/attendance/codemap.md) |
| `src/main/java/com/dojomanager/rest/v1/authorization/` | `AuthorizationController` (`/api/auth`) — login/signup, JWT issuance | [View Map](src/main/java/com/dojomanager/rest/v1/authorization/codemap.md) |
| `src/main/java/com/dojomanager/rest/v1/dojo/` | `DojoController` (`GET/POST /api/v1/dojos`, `PUT /{id}`), `DojoOwnerController` (`/api/v1/owner`), `ScheduleController` (`/api/v1/schedules`) — principal-scoped, DTO responses, `DojoApiExceptionHandler` (404/403/400 incl. field errors) | [View Map](src/main/java/com/dojomanager/rest/v1/dojo/codemap.md) |
| `src/main/java/com/dojomanager/rest/v1/students/` | `StudentController` (`/api/v1/students`) — student CRUD, soft-delete, attendance history | [View Map](src/main/java/com/dojomanager/rest/v1/students/codemap.md) |
| `src/main/java/com/dojomanager/services/` | Service layer — business logic, transaction/authorization boundary, DTO mapping | [View Map](src/main/java/com/dojomanager/services/codemap.md) |
| `src/main/java/com/dojomanager/services/exceptions/` | Domain exceptions (400/403/404/409) consumed by `@RestControllerAdvice` handlers | [View Map](src/main/java/com/dojomanager/services/exceptions/codemap.md) |
| `src/main/java/com/dojomanager/security/` | Spring Security config — stateless chain, `/api/auth/**` permitAll, BCrypt | [View Map](src/main/java/com/dojomanager/security/codemap.md) |
| `src/main/java/com/dojomanager/security/jwt/` | JWT primitives — `JwtProvider` (HS512), auth filter, 401 entry point | [View Map](src/main/java/com/dojomanager/security/jwt/codemap.md) |
| `src/main/java/com/dojomanager/security/services/` | `UserDetailsServiceImpl` → `DojoOwnerService` adapter; `OwnerPrincipal` | [View Map](src/main/java/com/dojomanager/security/services/codemap.md) |
| `src/main/java/com/dojomanager/data/` | Data-layer umbrella (entities, repositories, dto, forms) — no direct classes | [View Map](src/main/java/com/dojomanager/data/codemap.md) |
| `src/main/java/com/dojomanager/data/entities/` | JPA entities — `AbstractEntity` base, `Person` inheritance, table mapping | [View Map](src/main/java/com/dojomanager/data/entities/codemap.md) |
| `src/main/java/com/dojomanager/data/entities/attendance/` | `AttendanceRecord` (+ unique key on student/session/date), `AttendanceStatus` enum | [View Map](src/main/java/com/dojomanager/data/entities/attendance/codemap.md) |
| `src/main/java/com/dojomanager/data/entities/dojo/` | `Dojo`, `DojoOwner` (extends Person, cascaded managed dojos) | [View Map](src/main/java/com/dojomanager/data/entities/dojo/codemap.md) |
| `src/main/java/com/dojomanager/data/entities/people/` | `Person` mapped superclass, `Student`, `Address` | [View Map](src/main/java/com/dojomanager/data/entities/people/codemap.md) |
| `src/main/java/com/dojomanager/data/entities/rank/` | Rank catalog chain (`RankName`→`RankLevel`→`BeltColor`→`RankSetting`) + `StudentRank` | [View Map](src/main/java/com/dojomanager/data/entities/rank/codemap.md) |
| `src/main/java/com/dojomanager/data/entities/schedule/` | `WeeklySchedule`, `DailySchedule` (attendance session linkage) | [View Map](src/main/java/com/dojomanager/data/entities/schedule/codemap.md) |
| `src/main/java/com/dojomanager/data/repositories/` | Spring Data JPA repository root (`JpaRepository<T, Long>`) | [View Map](src/main/java/com/dojomanager/data/repositories/codemap.md) |
| `src/main/java/com/dojomanager/data/repositories/attendance/` | `AttendanceRecordRepository` — owner/session/student-scoped finders | [View Map](src/main/java/com/dojomanager/data/repositories/attendance/codemap.md) |
| `src/main/java/com/dojomanager/data/repositories/dojo/` | `DojoRepository`, `DojoOwnerRepository` (email lookup/uniqueness) | [View Map](src/main/java/com/dojomanager/data/repositories/dojo/codemap.md) |
| `src/main/java/com/dojomanager/data/repositories/people/` | `StudentRepository` (dojo/owner-scoped finders), `AddressRepository` | [View Map](src/main/java/com/dojomanager/data/repositories/people/codemap.md) |
| `src/main/java/com/dojomanager/data/repositories/rank/` | Five rank repositories (`RankRepository` manages `StudentRank`; `RankSettingRepository.findByDojo`) | [View Map](src/main/java/com/dojomanager/data/repositories/rank/codemap.md) |
| `src/main/java/com/dojomanager/data/repositories/schedule/` | `WeeklyScheduleRepository`, `DailyScheduleRepository` (CRUD-only) | [View Map](src/main/java/com/dojomanager/data/repositories/schedule/codemap.md) |
| `src/main/java/com/dojomanager/data/dto/` | Response/request DTOs root — `MessageResponse` + five subpackages | [View Map](src/main/java/com/dojomanager/data/dto/codemap.md) |
| `src/main/java/com/dojomanager/data/dto/attendance/` | `AttendanceRecordDto` — null-safe static `from(entity)` mapping | [View Map](src/main/java/com/dojomanager/data/dto/attendance/codemap.md) |
| `src/main/java/com/dojomanager/data/dto/authorization/` | `JwtResponse` (Bearer token + email) | [View Map](src/main/java/com/dojomanager/data/dto/authorization/codemap.md) |
| `src/main/java/com/dojomanager/data/dto/dojo/` | `CreateDojoRequest` (nested `ranks`), `DojoDto`, `DojoResponse`, `OwnerResponse`, `DojoUpdateRequest` — flat dojo/owner payloads (legacy `OwnerDTO` removed) | [View Map](src/main/java/com/dojomanager/data/dto/dojo/codemap.md) |
| `src/main/java/com/dojomanager/data/dto/schedule/` | `ScheduleSessionDTO` — string-typed day/times with `@JsonProperty` | [View Map](src/main/java/com/dojomanager/data/dto/schedule/codemap.md) |
| `src/main/java/com/dojomanager/data/dto/student/` | `StudentDto` — rank name/level/hours projection built by `StudentService.toDto` | [View Map](src/main/java/com/dojomanager/data/dto/student/codemap.md) |
| `src/main/java/com/dojomanager/data/forms/` | Validated request forms — `LoginForm`, `SignupForm` + subpackages | [View Map](src/main/java/com/dojomanager/data/forms/codemap.md) |
| `src/main/java/com/dojomanager/data/forms/attendance/` | `AttendanceCheckInForm` (sessionId-requires-date invariant) | [View Map](src/main/java/com/dojomanager/data/forms/attendance/codemap.md) |
| `src/main/java/com/dojomanager/data/forms/student/` | `StudentRequestForm` (optional rankId/dojoId, principal-derived ownership) | [View Map](src/main/java/com/dojomanager/data/forms/student/codemap.md) |
| `src/main/java/com/dojomanager/utils/` | `Utils` — empty `@Component` placeholder (unused) | [View Map](src/main/java/com/dojomanager/utils/codemap.md) |

## Request Flow (happy path)
1. HTTP request → `JwtAuthenticationTokenFilter` (Bearer token → `OwnerPrincipal` in `SecurityContext`) or rejected 401 by `JwtAuthenticationEntryPoint`.
2. Versioned controller (`rest/v1/...`) validates the form/DTO (`@Valid`) and extracts `@AuthenticationPrincipal OwnerPrincipal`.
3. Owner-scoped service enforces ownership (`requireOwner`-style helpers), loads entities via scoped repository finders, applies business rules (duplicates → 409, missing → 404).
4. Repository persists via Spring Data JPA (MySQL).
5. Service maps entity → DTO; controller returns `ResponseEntity`; domain exceptions → JSON via the controller's `@RestControllerAdvice` handler.
