# src/main/java/com/dojomanager/security/

## Responsibility

Security Layer — configures the stateless JWT authentication chain for the whole Spring Boot application. It owns HTTP authorization rules, password hashing, the `AuthenticationManager` exposed to the login endpoint, and the wiring of JWT authentication into the servlet filter chain.

## Design

- `WebSecurityConfig` is the single `@Configuration` / `@EnableWebSecurity` / `@EnableMethodSecurity` entry point. It declares the `SecurityFilterChain`, a `BCryptPasswordEncoder` `PasswordEncoder` bean, and exposes Spring Security's auto-configured `AuthenticationManager` via `AuthenticationConfiguration` (deliberately not building a second manager, to avoid the Spring Security 6 "already built object" conflict).
- The chain is **stateless** (`SessionCreationPolicy.STATELESS`), CSRF is disabled, CORS is enabled, and `JwtAuthenticationTokenFilter` is registered as a bean and inserted before `UsernamePasswordAuthenticationFilter`.
- Unauthorized access is delegated to `JwtAuthenticationEntryPoint` (`unauthorizedHandler`) through `exceptionHandling(...).authenticationEntryPoint(...)`.
- Authentication responsibilities are split: `JwtProvider` issues/validates tokens, `JwtAuthenticationTokenFilter` authenticates each request, `UserDetailsServiceImpl` loads the owner, and `OwnerPrincipal` is the authenticated principal type.
- `@EnableMethodSecurity` is enabled, but authorization here is currently URL-based rather than annotation-based.

## Flow

1. Incoming HTTP request enters the Spring Security filter chain; CORS is applied.
2. `JwtAuthenticationTokenFilter` runs before `UsernamePasswordAuthenticationFilter`. It reads `Authorization: Bearer <token>`, validates it with `JwtProvider`, resolves the username, loads an `OwnerPrincipal` through `UserDetailsServiceImpl`, and places a `UsernamePasswordAuthenticationToken` in `SecurityContextHolder`.
3. `authorizeHttpRequests` permits `/api/auth/**` and requires authentication for **any other request**.
4. Authenticated requests continue to the controller; a protected request without a valid token reaches `JwtAuthenticationEntryPoint`, which responds `401 Unauthorized`.
5. On login/signup, `AuthorizationController` calls the `AuthenticationManager` bean (BCrypt password check via `UserDetailsServiceImpl`) and then `JwtProvider` to mint the token.

## Integration

- Protects every endpoint outside `/api/auth/**`: `/api/v1/students/**`, `/api/v1/attendance/**`, `/dojo/**`, `/owner/**`, `/schedules/**`.
- `/api/auth/login` and `/api/auth/signup` are public and are implemented in `com.dojomanager.rest.v1.authorization.AuthorizationController`.
- Depends on `com.dojomanager.security.jwt` for token/filter/entry-point behavior and `com.dojomanager.security.services` for principal loading.
- Downstream controllers and services receive the authenticated `OwnerPrincipal` via `@AuthenticationPrincipal` (e.g. `StudentController`, `AttendanceController`) or by casting the `SecurityContextHolder` authentication (e.g. `DojoController`, `DojoOwnerController`).
