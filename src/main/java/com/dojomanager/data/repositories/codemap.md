# src/main/java/com/dojomanager/data/repositories/

## Responsibility
Data Access Layer — the complete set of Spring Data JPA repository interfaces for the DojoManager backend. This parent package contains no interfaces itself, only one sub-package per domain area:

- `attendance/` — `AttendanceRecordRepository`, persistence and owner-scoped lookups for attendance check-ins.
- `dojo/` — `DojoRepository` and `DojoOwnerRepository`, persistence for dojos and their owning accounts.
- `people/` — `StudentRepository` and `AddressRepository`, persistence for students and postal addresses.
- `rank/` — `BeltColorRepository`, `RankLevelRepository`, `RankNameRepository`, `RankRepository` (`StudentRank`), and `RankSettingRepository`, persistence for the belt/rank catalog and student rank history.
- `schedule/` — `DailyScheduleRepository` and `WeeklyScheduleRepository`, persistence for recurring and per-day class schedules.

## Design
Every interface extends `org.springframework.data.jpa.repository.JpaRepository<T, Long>`, where `T` is the domain entity and the ID type is always `Long` (inherited from `AbstractEntity`'s `@Id @GeneratedValue(strategy = GenerationType.AUTO) Long id`). There are no `CrudRepository` or custom base interfaces, no `@Query`/native queries, and no custom repository implementations: all query behavior is expressed through Spring Data derived query methods whose names encode the predicates (`findBy...`, `existsBy...`, `deleteById`, `save`, `saveAll`, `findAll`). `@Repository` is declared explicitly only on `AttendanceRecordRepository`; the other interfaces rely on Spring's interface scanning. The interfaces are organized by domain sub-package rather than by entity hierarchy, and several expose deliberately owner-scoped finders (`...DojoOwnerId`) so services never need unscoped queries.

## Flow
Service code calls a repository method with a `Long` ID, an entity instance, or a derived-query parameter set → Spring Data generates the JPA query against the mapped entity/table → Hibernate executes it in the current transaction → managed entities are returned (`Optional`, `List`, or entity) or persisted/flushed. Write paths enter through `save`, `saveAll`, `saveAndFlush`, and `deleteById`; read paths use derived finders and `findAll`/`findById`.

## Integration
Consumed exclusively by the service layer in `com.dojomanager.services`: `AttendanceService`, `StudentService`, `DojoService`, `DojoOwnerService`, `RankService`, `ScheduleService`, and `AddressService`. Those services are in turn called by the REST layer (`AttendanceController`, `StudentController`, `DojoController`, `DojoOwnerController`, `ScheduleController`, `AuthorizationController`) and by `security/services/UserDetailsServiceImpl` (through `DojoOwnerService`). Repositories manage entities in `com.dojomanager.data.entities.{attendance,dojo,people,rank,schedule}` and depend on no other application layer.
