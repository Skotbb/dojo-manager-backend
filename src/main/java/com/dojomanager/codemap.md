# src/main/java/com/dojomanager/

## Responsibility
Root application package. It hosts the Spring Boot bootstrap class in the nested `dojomanager` subpackage and defines the top-level layer split of the backend.

## Design
Layered monolith under a single base package so Spring Boot's default component scan covers everything:
- `dojomanager/` — application entry point `DojomanagerApplication` (`@SpringBootApplication`); the name collides with the base package and is the nested app package.
- `rest/` — HTTP/API layer: versioned controllers (`rest/v1/{attendance,authorization,dojo,students}`) and per-controller `@RestControllerAdvice` exception handlers.
- `services/` — business logic (`StudentService`, `AttendanceService`, `DojoService`, `DojoOwnerService`, `RankService`, `ScheduleService`, `AddressService`) and domain exceptions in `services/exceptions`.
- `data/` — data layer umbrella: `entities`, `repositories`, `dto`, `forms`.
- `security/` — Spring Security and JWT plumbing (`security/jwt`, `security/services`, incl. `OwnerPrincipal`).

## Flow
`DojomanagerApplication.main` starts the context → component scan discovers controllers, services, repositories, security config → inbound HTTP requests are authenticated by the `security` layer → `rest` controllers call `services` → services use `data.repositories` over `data.entities` → responses are mapped to `data.dto` objects.

## Integration
- Packaging/build: Maven artifact `dojomanager` (Spring Boot 3.5.16, Java, Spring Web + Security + Data JPA, JJWT, Lombok, Guava).
- All cross-layer wiring happens through Spring dependency injection inside this package tree; there are no other top-level source packages.
