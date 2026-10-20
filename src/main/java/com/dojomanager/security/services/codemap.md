# src/main/java/com/dojomanager/security/services/

## Responsibility

Adapts the `DojoOwner` domain entity to Spring Security — provides the `UserDetailsService` used during login and JWT authentication, plus the custom `UserDetails` principal (`OwnerPrincipal`) that carries the owner id and email through the security context.

## Design

- **Adapter pattern:** `UserDetailsServiceImpl` (`@Service`, `@Transactional`) implements Spring's `UserDetailsService` and delegates to the domain service `DojoOwnerService`, keeping security concerns out of `DojoOwnerService`.
- **Custom principal:** `OwnerPrincipal` implements `UserDetails`. It stores `id`, `username` (the owner's email), `@JsonIgnore password`, and a `Collection<? extends GrantedAuthority>`. The two-argument-authorities-free constructor initializes `Collections.emptyList()`; the static factory `build(DojoOwner)` maps `owner.getId()`, `owner.getEmail()`, and `owner.getPassword()` and gives the principal no authorities.
- Account-state methods (`isAccountNonExpired`, `isAccountNonLocked`, `isCredentialsNonExpired`, `isEnabled`) are hardcoded to `true`. `equals` compares only the `id` via Guava `Objects.equal`.
- `loadUserByUsername` currently calls `Optional.get()` directly, so an unknown email surfaces as `NoSuchElementException` rather than `UsernameNotFoundException` (relevant if login error mapping is tightened).

## Flow

- **Login:** `AuthorizationController` → `AuthenticationManager` → `UserDetailsServiceImpl.loadUserByUsername(email)` → `DojoOwnerService.getOwnerByEmail(email)` → `Optional<DojoOwner>` → `OwnerPrincipal.build(owner)` → DaoAuthenticationProvider compares the BCrypt password and returns an authenticated principal.
- **JWT request:** `JwtAuthenticationTokenFilter` extracts the subject email from the token → same `loadUserByUsername` path → principal placed in `SecurityContextHolder`.
- **Controller/service use:** the principal is injected as `@AuthenticationPrincipal OwnerPrincipal` or read from the context; its `getId()` / `getUsername()` identify the owning `DojoOwner` for authorization scoping.

## Integration

- Depends on `com.dojomanager.data.entities.dojo.DojoOwner` and `com.dojomanager.services.DojoOwnerService`.
- Used by `WebSecurityConfig` (autowired `UserDetailsServiceImpl`, bean wiring), `JwtAuthenticationTokenFilter`, and `JwtProvider` (cast to `OwnerPrincipal`).
- Consumed by REST controllers (`StudentController`, `AttendanceController`) and domain services (`StudentService`, `AttendanceService`) to enforce per-owner data access.
- Passwords stored/compared through `BCryptPasswordEncoder` configured in `WebSecurityConfig`.
