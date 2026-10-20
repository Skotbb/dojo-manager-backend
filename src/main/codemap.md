# src/main/

## Responsibility
Production source set of the backend: Java sources under `java/` and runtime configuration under `resources/`. Structural node only.

## Design
Maven `src/main` convention. Runtime configuration is profile-based: `application.properties` plus `application-local.properties`, `application-dev.properties`, `application-test.properties`, `application-prod.properties`; `META-INF/` holds additional metadata.

## Flow
At startup Spring Boot loads `application.properties`, overlays the active profile file (e.g. `dev`), then component-scans the `com.dojomanager` tree.

## Integration
- `java/` — all application code (`com.dojomanager.*`).
- `resources/` — datasource, JPA, JWT and server settings per environment.
