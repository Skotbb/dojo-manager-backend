# src/main/java/com/dojomanager/dojomanager/

## Responsibility

Application bootstrap — contains `DojomanagerApplication`, the Spring Boot entry point that starts the embedded server and defines the component/entity/repository scanning boundaries for the entire backend.

## Design

- `DojomanagerApplication` is annotated with `@SpringBootApplication` plus explicit `@ComponentScan({"com.dojomanager"})`, `@EntityScan({"com.dojomanager.data.entities"})`, and `@EnableJpaRepositories("com.dojomanager.data.repositories")`. The explicit scans widen discovery beyond this package (the class lives in `com.dojomanager.dojomanager`, a sibling of the feature packages).
- A package-private, empty `ApplicationConfiguration` class is also annotated `@Configuration`; it currently declares no beans and serves as a placeholder for application-wide configuration.
- `main(String[] args)` delegates to `SpringApplication.run(DojomanagerApplication.class, args)`; there is no custom banner, runner, or property customization.

## Flow

1. JVM launches `DojomanagerApplication.main`.
2. `SpringApplication.run` creates the application context, performing component scanning over `com.dojomanager` (picking up `WebSecurityConfig`, `AuthorizationController`, REST controllers, services, and `JwtProvider`), entity scanning, and JPA repository proxy creation.
3. Auto-configuration (including Spring Security and the servlet container) initializes; `JwtProvider.ensureJwtSecretConfigured()` fails fast if `JWT_SECRET` is missing.
4. The embedded server starts and routes requests into the security filter chain.

## Integration

- Root of the package tree: enables `com.dojomanager.security`, `com.dojomanager.security.jwt`, `com.dojomanager.security.services`, `com.dojomanager.rest`, `com.dojomanager.services`, and `com.dojomanager.utils`.
- `@EntityScan` registers `com.dojomanager.data.entities` (including `DojoOwner`), and `@EnableJpaRepositories` activates `com.dojomanager.data.repositories`, which back `DojoOwnerService` and the other domain services.
- Depends only on Spring Boot; it has no application-level imports and is not referenced by other classes at runtime beyond framework startup.
