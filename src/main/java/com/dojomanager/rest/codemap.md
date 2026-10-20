# src/main/java/com/dojomanager/rest/

## Responsibility
HTTP/JSON boundary of the application: every Spring MVC controller that exposes the DojoManager API to the Angular frontend lives under this package.

## Design
- Package-per-API-version: the only child today is `v1/`, holding one package per business domain (`attendance`, `authorization`, `dojo`, `students`).
- Versioning convention: new endpoints are namespaced by URL (`/api/v1/...`) and by Java package (`rest.v1.<domain>`); the package mirrors the URL version.
- Controllers are thin: they bind/validate HTTP input, resolve the authenticated caller, delegate to a `com.dojomanager.services` bean, and return DTOs (never JPA entities, except in the legacy `dojo/` endpoints).
- Mixed controller styles are present: HTTP-versioned `@RestController` classes with constructor injection (Lombok `@RequiredArgsConstructor`), plus older `/dojo`, `/owner`, `/schedules` controllers using field `@Autowired` and, in `DojoController`, `@Controller` + `@ResponseBody`.
- Per-controller `@RestControllerAdvice(assignableTypes = ...)` handlers (`AttendanceApiExceptionHandler`, `StudentApiExceptionHandler`) translate service exceptions into a shared `{status, error, message[, fieldErrors]}` JSON shape.

## Flow
`DispatcherServlet` → `JwtAuthenticationTokenFilter` (Bearer token → `OwnerPrincipal` in `SecurityContext`) → `rest.v1.<domain>` controller method → `com.dojomanager.services.*` service (ownership checks) → DTO / `ResponseEntity` serialized by Jackson. Unauthenticated requests outside `/api/auth/**` are rejected by `JwtAuthenticationEntryPoint`.

## Integration
- Downstream: `com.dojomanager.services` (`AttendanceService`, `StudentService`, `DojoOwnerService`, `DojoService`, `ScheduleService`, `RankService`) and `com.dojomanager.security` (`JwtProvider`, `AuthenticationManager`, `PasswordEncoder`, `OwnerPrincipal`).
- Payloads: forms in `com.dojomanager.data.forms`, DTOs in `com.dojomanager.data.dto`.
- Security rules (`WebSecurityConfig`): stateless JWT, CSRF disabled, CORS enabled; `/api/auth/**` is `permitAll`, every other request is `authenticated()`; no role/authority rules — owner scoping is enforced inside the service layer.
