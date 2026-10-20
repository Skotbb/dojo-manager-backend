# src/main/java/

## Responsibility
Root of the Java source tree. Structural package-path node with a single child package, `com`.

## Design
Maven source root; contains no classes directly. All code lives under the reverse-domain namespace `com.dojomanager`.

## Flow
Compilation output goes to `target/classes`; at runtime this root is on the application classpath.

## Integration
- `com/` — the only subpackage; leads to `com.dojomanager.*`.
