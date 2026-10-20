# src/

## Responsibility
Build source root of the `dojomanager` Maven/Spring Boot project. Pure container: it holds the production tree under `main/` and the test tree under `test/`, plus this codemap.

## Design
Standard Maven layout. No code or configuration of its own; build behaviour is driven by the project-root `pom.xml` (Spring Boot 3.5.16 parent).

## Flow
Nothing flows at runtime from this level — it only determines source-set roots for compilation and test execution.

## Integration
- `main/` — production sources and resources (`com.dojomanager` package tree, `application*.properties`).
- `test/` — JUnit/Spring test sources mirroring the main package structure.
