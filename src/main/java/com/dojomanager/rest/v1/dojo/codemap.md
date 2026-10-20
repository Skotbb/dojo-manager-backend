# src/main/java/com/dojomanager/rest/v1/dojo/

## Responsibility
REST API v1 — dojo profile management, dojo-owner lookup, and weekly schedule management. Contains three controllers with mixed (partly legacy) routing conventions.

## Design
- `DojoController` — legacy `@Controller` + `@ResponseBody`, class mapping `@RequestMapping(path = "/dojo")` but methods declare absolute paths:
  - `GET /api/v1/dojo` → `getMyDojo()`.
  - `PUT /api/v1/dojo` → `updateDojo(@RequestBody DojoUpdateRequest)`.
  - Field-injects `DojoOwnerService`, `DojoService`, `RankService` (RankService currently unused); reads the caller's email from `SecurityContextHolder.getContext().getAuthentication()`.
- `DojoOwnerController` — `@RestController`, `@CrossOrigin(origins = "http://localhost:4200", maxAge = 3600)`, `@RequestMapping(path = "/owner")`:
  - `GET /owner/{userId}` → `getDojos(@PathVariable String email)`; the path variable is used as an email.
  - `GET /owner/all` → `getAllOwners()`; despite the name, it returns only the caller's own `DojoOwner`.
  - Field-injects `DojoOwnerService`.
- `ScheduleController` — `@RestController`, `@RequestMapping(path = "/schedules")`, field-injects `ScheduleService`:
  - `POST /schedules`, `PUT /schedules/{id}`, `DELETE /schedules/{id}`, `GET /schedules/week`.
  - No `@CrossOrigin`, no `@Valid`, and no explicit owner parameter — a legacy, not-yet-owner-scoped surface.
- Identity convention is inconsistent: `DojoController` and `DojoOwnerController.getAllOwners` use the JWT-derived `SecurityContext`, while `/owner/{userId}` trusts an email from the URL.
- Responses in `DojoController`/`DojoOwnerController` expose JPA entities (`Dojo`, `DojoOwner`), unlike the DTO-based `students/` and `attendance/` packages.

## Flow
- `GET /api/v1/dojo` → email from `SecurityContext` → `ownerService.getOwnerByEmail(email)` → `DojoResponse(List.copyOf(owner.getManagedDojos()))`; empty owner → `DojoResponse(null)`.
- `PUT /api/v1/dojo` + `DojoUpdateRequest` → resolve owner → build a new `Dojo` from `name`, `website`, `address` (the `contact` field is accepted but ignored; `address` becomes `Address(address, null, null, null, null)`) → `dojoService.saveDojo(dojo)` → `DojoResponse(List.of(dojo))`. Note: the new dojo is not linked to the owner here.
- `GET /owner/{email}` → `ownerService.getOwnerByEmail(email)` → `Collection<Dojo>` of managed dojos, or an empty set.
- `GET /owner/all` → caller's email → `getOwnerByEmail` → singleton list of that owner (not all owners).
- `POST /schedules` + `ScheduleSessionDTO` → `scheduleService.saveSession(dto)` → `201 Created` with saved `ScheduleSessionDTO`.
- `PUT /schedules/{id}` + `ScheduleSessionDTO` → `scheduleService.updateSession(id, dto)` → `200 OK`, or `404 Not Found` when the service returns `null`.
- `DELETE /schedules/{id}` → `scheduleService.deleteSession(id)` → `200 OK` with empty body.
- `GET /schedules/week` → `scheduleService.getWeekView()` → `ResponseEntity<Map<String, List<WeeklySchedule>>>`.

## Integration
- Services: `DojoOwnerService` (`getOwnerByEmail`), `DojoService` (`saveDojo`), `RankService` (injected, unused), `ScheduleService` (`saveSession`, `updateSession`, `deleteSession`, `getWeekView`).
- Payloads/types: `DojoUpdateRequest`, `DojoResponse`, `OwnerDTO` (declared but not used by these controllers), `ScheduleSessionDTO`, and entities `Dojo`, `DojoOwner`, `Address`, `WeeklySchedule`.
- Security: every route requires authentication except `/api/auth/**`; `WebSecurityConfig` defines no role rules, so any authenticated owner can call the schedule endpoints. Owner identity normally comes from the JWT (`OwnerPrincipal` in the context); `/owner/{userId}` is the exception, taking the email from the path.
