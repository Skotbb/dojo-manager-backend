# src/main/java/com/

## Responsibility
Java package-path node for the `com` namespace segment. No direct classes.

## Design
Structural only; exists so that the application code can use the conventional `com.dojomanager` base package.

## Flow
No runtime behaviour of its own — it participates only in package naming and classpath layout.

## Integration
- `dojomanager/` — the application's sole root package, containing the Spring Boot entry point and all domain layers.
