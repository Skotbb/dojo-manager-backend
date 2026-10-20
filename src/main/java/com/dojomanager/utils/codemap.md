# src/main/java/com/dojomanager/utils/

## Responsibility

Shared utility namespace — currently a placeholder. The only class, `Utils`, is an empty `@Component` with no methods or fields, intended as the home for cross-cutting helper code.

## Design

- `Utils` carries only the `org.springframework.stereotype.Component` annotation, making it an injectable singleton bean despite having no behavior.
- No static helpers, constants, or state exist yet; there are no sub-packages or related utility classes.
- Because it is a Spring-managed bean rather than a final utility class, future helpers can receive dependencies via constructor/field injection if needed.

## Flow

- No runtime flow: no code invokes `Utils`, and it participates in no request, security, or persistence path.
- If methods are added, consumers would autowire `Utils` like any other Spring bean.

## Integration

- No references anywhere in the codebase (verified via grep); it is discovered by the `com.dojomanager` component scan from `DojomanagerApplication`.
- Standalone from `security`, `rest`, `services`, and `data` packages; intended to be a neutral shared dependency rather than depending on them.
