# src/main/java/com/dojomanager/rest/v1/authorization/

## Responsibility
REST API v1 — authentication: email/password login and dojo-owner signup, both returning a JWT. These are the only public (unauthenticated) endpoints in the API.

## Design
- `AuthorizationController` — `@RestController`, `@RequestMapping("/api/auth")`, `@CrossOrigin(origins = "*", maxAge = 3600)`; field injection via `@Autowired` for `AuthenticationManager`, `DojoOwnerService`, `PasswordEncoder` (BCrypt), and `JwtProvider`.
- Endpoints, relative to `/api/auth`:
  - `POST /api/auth/login` → `authenticateUser(@Valid @RequestBody LoginForm)`.
  - `POST /api/auth/signup` → `registerUser(@Valid @RequestBody SignupForm)`.
- Login delegates to a private `authenticateUser(email, password)` helper; any exception (e.g. bad credentials) is caught and returned as `400 Bad Request` with a `MessageResponse` carrying `error`.
- Signup rejects an existing email with `400 Bad Request` (`MessageResponse("Error: Email already exists")`), builds a `DojoOwner` from the form with a BCrypt-hashed password, persists it, then immediately authenticates and returns a JWT.
- Both endpoints return `ResponseEntity<?>` because success (`JwtResponse`) and failure (`MessageResponse`) bodies differ.

## Flow
- `POST /api/auth/login` + `LoginForm` → `authenticationManager.authenticate(UsernamePasswordAuthenticationToken(email, password))` → `SecurityContextHolder` set → `jwtProvider.generateJwtToken(authentication)` → `200 OK` `JwtResponse(jwt, username)`; on failure `400 Bad Request` `MessageResponse`.
- `POST /api/auth/signup` + `SignupForm` → `ownerService.doesEmailExist(email)` → `encoder.encode(password)` → `new DojoOwner(firstName, lastName, email, hash)` → `ownerService.saveDojoOwner(owner)` → private `authenticateUser(...)` → `200 OK` `JwtResponse`.

## Integration
- Services/beans: `DojoOwnerService` (`doesEmailExist`, `saveDojoOwner`), Spring Security `AuthenticationManager`, `PasswordEncoder`, `JwtProvider`.
- Entities: `com.dojomanager.data.entities.dojo.DojoOwner`.
- Input forms: `com.dojomanager.data.forms.LoginForm`, `com.dojomanager.data.forms.SignupForm`.
- Output DTOs: `com.dojomanager.data.dto.authorization.JwtResponse`, `com.dojomanager.data.dto.MessageResponse`.
- Security rules: `/api/auth/**` is `permitAll()` in `WebSecurityConfig`; the filter chain is stateless with CSRF disabled, so these endpoints are reachable without a Bearer token. Later requests authenticate with the issued JWT via `JwtAuthenticationTokenFilter`.
