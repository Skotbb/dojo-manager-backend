# src/main/java/com/dojomanager/security/jwt/

## Responsibility

JWT mechanics for the application — issue HS512-signed tokens, validate incoming bearer tokens per request, populate the security context, and translate authentication failures into a `401` response.

## Design

- `JwtProvider` (`@Component`) is the token service. It reads `security.app.jwtSecret` (from `JWT_SECRET` in `application.properties`) and `security.app.jwtExpiration` (43200 seconds) via `@Value`. `@PostConstruct ensureJwtSecretConfigured()` fails fast at startup when the secret is blank, preventing accidental use of an unset signing key. Tokens are built with JJWT using subject = owner username/email, issued-at and expiration claims, signed with `SignatureAlgorithm.HS512`.
- `JwtAuthenticationTokenFilter` extends `OncePerRequestFilter`. It is not annotated as a component; `WebSecurityConfig.authenticationJwtTokenFilter()` constructs it as a `@Bean`, and its `JwtProvider` / `UserDetailsServiceImpl` dependencies are `@Autowired`. Extraction is a simple `Authorization` header check and `replace("Bearer", "")`.
- `JwtAuthenticationEntryPoint` (`@Component`) implements Spring Security's `AuthenticationEntryPoint`, logs the `AuthenticationException` and calls `response.sendError(401, "Error: Unauthorized")`.
- Validation is exception-driven: `validateJwsToken` catches `SignatureException`, `MalformedJwtException`, `ExpiredJwtException`, `UnsupportedJwtException`, and `IllegalArgumentException`, logs them, and returns `false`; no exception escapes to the filter chain.

## Flow

1. Request carries `Authorization: Bearer <jwt>`; `JwtAuthenticationTokenFilter.getJwt` strips the prefix.
2. `JwtProvider.validateJwsToken` verifies signature and expiry. On failure the filter logs and continues without authenticating, so the request stays anonymous.
3. On success, `getUsernameFromJwtToken` parses the subject; `UserDetailsServiceImpl.loadUserByUsername` resolves it to an `OwnerPrincipal`.
4. The filter builds `UsernamePasswordAuthenticationToken(userDetails, null, authorities)`, attaches request details, and stores it in `SecurityContextHolder`.
5. `filterChain.doFilter(...)` always continues. If the target path is protected and no authentication was set, `WebSecurityConfig` routes to `JwtAuthenticationEntryPoint`, producing `401 Unauthorized`.
6. For login/signup, `AuthorizationController` calls `JwtProvider.generateJwtToken(authentication)` after `AuthenticationManager` authenticates the owner, returning the token in `JwtResponse`.

## Integration

- Wired by `com.dojomanager.security.WebSecurityConfig` (filter bean + entry point).
- Consumes `com.dojomanager.security.services.OwnerPrincipal` (cast from `Authentication.getPrincipal()`) and `UserDetailsServiceImpl` (username-to-principal lookup).
- Called by `com.dojomanager.rest.v1.authorization.AuthorizationController` for token issuance; all protected REST controllers rely on the context this package populates.
- Configuration keys come from `src/main/resources/application.properties` (`security.app.jwtSecret=${JWT_SECRET}`, `security.app.jwtExpiration=43200`).
