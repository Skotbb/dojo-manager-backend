# src/main/java/com/dojomanager/services/

## Responsibility
Service layer — Spring-managed business logic for the dojo domain: students, attendance, schedules, dojos/owners, ranks, and addresses. Each class is the transaction/authorization boundary between the REST controllers and the JPA repositories.

## Design
- Classes annotated `@Service`; split across two styles:
  - **Modern owner-scoped services** — `StudentService`, `AttendanceService`. Use Lombok `@RequiredArgsConstructor` constructor injection, `@Transactional` / `@Transactional(readOnly = true)` at method level, take `com.dojomanager.security.services.OwnerPrincipal`, and resolve the caller's `DojoOwner` through `DojoOwnerRepository` before every operation.
  - **Legacy CRUD services** — `DojoOwnerService`, `DojoService`, `RankService`, `ScheduleService`, `AddressService`. Field injection via `@Autowired`, thin pass-through methods over repositories (`save`, `saveAndFlush`, `findById`, `findBy...`), often returning empty entities instead of throwing.
- DTO mapping lives in the service: `StudentService.toDto` hand-builds `StudentDto` (including rank name/level from `RankSetting`); `AttendanceService.toDto` delegates to `AttendanceRecordDto.from(entity)`; `ScheduleService` maps `ScheduleSessionDTO` ↔ `WeeklySchedule`.
- Ownership enforcement helpers in the owner-scoped services: `requireOwner`, `requireOwnedStudent`, `requireOwnedDojo`, `resolveTargetDojo`, `requireRankInDojo`, `verifySessionOwner`, `verifySessionDojo`. Unauthorized access to another owner's data is deliberately reported as `ResourceNotFoundException` (404) rather than 403 to avoid leaking existence.
- Soft delete: `StudentService.deactivateStudent` sets `active = false` and preserves rank history.
- Duplicate protection: `StudentService` checks `existsByDojoIdAndEmailIgnoreCase`; `AttendanceService` checks `findByStudentIdAndSessionIdAndSessionDate`; both throw `DuplicateResourceException`. Emails are normalised to trimmed lowercase (`Locale.ROOT`).
- `ScheduleService` is partially implemented: `saveSession` builds a `WeeklySchedule` but does not persist it and returns the input DTO; `getWeekView` returns a `ResponseEntity<Map<String, List<WeeklySchedule>>>` grouped by `DayOfWeek.name()` — i.e. it leaks HTTP concerns into the service layer.

## Flow
1. Controller (e.g. `StudentController`, `AttendanceController`) extracts `OwnerPrincipal` from the security context and calls a service method with a form/DTO.
2. Service validates the request, loads the `DojoOwner`, and resolves entities through owner/dojo-scoped repository finders (e.g. `StudentRepository.findByIdAndDojoOwnerId`, `DojoRepository.findByOwner`).
3. Repository (Spring Data JPA) reads/writes the entity.
4. Service maps entity → DTO (`StudentDto`, `AttendanceRecordDto`, `ScheduleSessionDTO`) and returns it; controllers wrap it in `ResponseEntity`.
5. Domain failures surface as unchecked exceptions from `services/exceptions` and are translated to JSON by the per-controller `@RestControllerAdvice` handlers.

## Integration
- **Consumers (controllers):**
  - `StudentController` → `StudentService`, `AttendanceService`.
  - `AttendanceController` → `AttendanceService`.
  - `DojoController` → `DojoOwnerService`, `DojoService`, `RankService`.
  - `DojoOwnerController` → `DojoOwnerService`.
  - `AuthorizationController` → `DojoOwnerService` (signup/email checks).
  - `ScheduleController` → `ScheduleService`.
  - `AddressService` and `RankService` write paths have no current controller callers (available for rank/address management endpoints).
- **Repositories used:** `StudentRepository`, `DojoRepository`, `DojoOwnerRepository`, `AttendanceRecordRepository`, `DailyScheduleRepository`, `WeeklyScheduleRepository`, `RankRepository`, `RankNameRepository`, `RankLevelRepository`, `BeltColorRepository`, `RankSettingRepository`, `AddressRepository`.
- **Entities touched:** `Student`, `Dojo`, `DojoOwner`, `AttendanceRecord`, `DailySchedule`, `WeeklySchedule`, `StudentRank`, `RankName`, `RankLevel`, `BeltColor`, `RankSetting`, `Address`.
- **Cross-cutting:** depends on `com.dojomanager.security.services.OwnerPrincipal` for identity and on `com.dojomanager.services.exceptions` for error signalling; `DojoOwnerService` also uses raw `EntityManager`/Guava `Lists`, and `DojoService` depends on `DojoOwnerService`.
