# src/main/java/com/dojomanager/data/dto/authorization/

## Responsibility

DTO Layer — authentication response payload. Carries the JWT issued on successful login or signup back to the Angular client.

## Design

- `JwtResponse` — Lombok `@Data`, hand-written constructor. Fields:
  - `token` — the signed JWT produced by `JwtProvider.generateJwtToken`.
  - `email` — the authenticated principal's username/email (`UserDetails.getUsername()`).
  - `type` — initialized to the literal `"Bearer"`, telling the client how to build the `Authorization` header.
- No validation annotations; this is response-only. Unlike the other DTOs, it exposes no entity data, only the token and identity string.
- Error responses on the same endpoints use `com.dojomanager.data.dto.MessageResponse` rather than a JWT-shaped body.

## Flow

1. `AuthorizationController.authenticateUser(@Valid LoginForm)` calls the private `authenticateUser(email, password)`.
2. `AuthenticationManager` authenticates, `JwtProvider` generates the token, and `new JwtResponse(jwt, userDetails.getUsername())` is built.
3. `POST /api/auth/login` returns it via `ResponseEntity.ok(...)`.
4. `POST /api/auth/signup` saves the `DojoOwner`, then re-authenticates and returns the same `JwtResponse`.
5. Failure paths return `MessageResponse` with `error` populated and HTTP 400.

## Integration

- Produced and returned by `com.dojomanager.rest.v1.authorization.AuthorizationController` (`/api/auth/login`, `/api/auth/signup`).
- Depends on `com.dojomanager.security.jwt.JwtProvider` for token generation and on `DojoOwnerService`/`AuthenticationManager` for authentication.
- The client stores `token` and sends it as `Authorization: Bearer <token>`; `JwtAuthenticationTokenFilter` consumes it on subsequent requests.
