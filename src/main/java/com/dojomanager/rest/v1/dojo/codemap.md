# src/main/java/com/dojomanager/rest/v1/dojo/

## Responsibility
REST API v1 — dojo management, authenticated owner profile, and weekly schedule management. All three controllers now follow the `/api/v1/...` convention with principal-derived identity.

## Design
- `DojoController` — `@RestController`, `@CrossOrigin(origins = "http://localhost:4200", maxAge = 3600)`, `@RequestMapping(path = "/api/v1/dojos")`, constructor-injects `DojoService` via `@RequiredArgsConstructor`:
  - `GET /api/v1/dojos` → `getMyDojos(@AuthenticationPrincipal OwnerPrincipal)` → `DojoResponse` of `DojoDto`.
  - `POST /api/v1/dojos` → `createDojo(principal, @Valid CreateDojoRequest)` → `201 Created` `DojoDto`; persists the dojo together with its rank graph (see `DojoService.persistRanks`).
  - `PUT /api/v1/dojos/{id}` → `updateDojo(principal, id, DojoUpdateRequest)` → `DojoDto`; a foreign/missing dojo is reported as 404. Updates deliberately do NOT touch ranks.
- `DojoOwnerController` — `@RestController`, `@CrossOrigin`, `@RequestMapping(path = "/api/v1/owner")`:
  - `GET /api/v1/owner` → `getMyOwner(principal)` → `OwnerResponse` (owner + flattened managed dojos).
  - The legacy `GET /owner/{userId}` (email trusted from the path) and `GET /owner/all` are removed; there is no path variable that can select another owner.
- `ScheduleController` — `@RestController`, `@RequestMapping(path = "/api/v1/schedules")` (path normalized only; service contract unchanged):
  - `POST /api/v1/schedules`, `PUT /api/v1/schedules/{id}`, `DELETE /api/v1/schedules/{id}`, `GET /api/v1/schedules/week`.
  - Still no `@CrossOrigin`/`@Valid` and not yet owner-scoped — a known legacy surface.
- `DojoApiExceptionHandler` — `@RestControllerAdvice(assignableTypes = {DojoController, DojoOwnerController})`: maps `ResourceNotFoundException` → 404, `ForbiddenOperationException` → 403, `HttpMessageNotReadableException` → 400, and `MethodArgumentNotValidException` → 400 with a `fieldErrors` map (mirrors the students handler). Without it, a missing dojo surfaced as a 500.
- Responses are DTOs only (`DojoDto`, `DojoResponse`, `OwnerResponse`) — no raw JPA entity bodies.

## Flow
- `GET /api/v1/dojos` → `DojoService.listDojos(principal)` (owner resolved via `DojoOwnerService.requireOwner`) → `DojoDto::from` per dojo → `DojoResponse`.
- `POST /api/v1/dojos` → `DojoService.createDojo(principal, CreateDojoRequest)` builds a `Dojo`, applies `name`/`website`/`address`, sets the authenticated owner, persists, then `persistRanks` fans out the nested ranks: one `RankName(dojo)` per rank, and per level `RankLevel(rankName)` → `BeltColor(color, stripeColor, stripeCount, level)` → `RankSetting(dojo, rankName, level, beltColor)` via `RankService` — all in one `@Transactional` (null `stripeCount` defaults to 0) → `201 Created`.
- `PUT /api/v1/dojos/{id}` → `DojoService.updateDojo(principal, id, request)` filters the principal's own dojos (foreign id → `ResourceNotFoundException` → 404 via handler) → applies fields → saved.
- `GET /api/v1/owner` → `DojoOwnerService.getOwnerProfile(principal)` (`@Transactional(readOnly = true)`, initializes `managedDojos`) → `OwnerResponse.from(...)`.
- Schedule endpoints unchanged apart from path: `POST` → `saveSession` → 201; `PUT /{id}` → `updateSession` → 200/404; `DELETE /{id}` → `deleteSession` → 200; `GET /week` → `getWeekView` → `Map<String, List<WeeklySchedule>>`.

## Integration
- Services: `DojoService` (`listDojos`, `createDojo`, `updateDojo`), `DojoOwnerService` (`requireOwner`, `getOwnerProfile`), `ScheduleService` (`saveSession`, `updateSession`, `deleteSession`, `getWeekView`).
- Payloads/types: `DojoUpdateRequest`, `DojoResponse`, `DojoDto`, `OwnerResponse`, `ScheduleSessionDTO`.
- Security: all routes require authentication (`/api/auth/**` is the only permitAll); owner identity always comes from `@AuthenticationPrincipal OwnerPrincipal`. Errors mapped by `DojoApiExceptionHandler`.
- UI consumer: `dojo-manager-ui` `DojoService` (`getMyDojoOwner`, `submitNewDojo`) calls `GET /api/v1/owner` and `POST /api/v1/dojos`.
