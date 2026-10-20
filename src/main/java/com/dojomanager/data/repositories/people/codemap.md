# src/main/java/com/dojomanager/data/repositories/people/

## Responsibility
Data Access Layer — people repositories. Provides persistence for `Student` records and `Address` values, including owner/dojo-scoped student lookups and per-dojo email uniqueness checks.

## Design
Two interfaces extending `JpaRepository<T, Long>`:

- `StudentRepository extends JpaRepository<Student, Long>` — derived finders only:
  - `List<Student> findByDojoId(Long dojoId)` — students of one dojo.
  - `List<Student> findByDojoOwnerId(Long ownerId)` — all students across an owner's dojos (traverses `student.dojo.owner.id`).
  - `Optional<Student> findByIdAndDojoOwnerId(Long id, Long ownerId)` — owner-scoped single lookup used for authorization.
  - `boolean existsByDojoIdAndEmailIgnoreCase(Long dojoId, String email)` — duplicate-email guard, case-insensitive.
- `AddressRepository extends JpaRepository<Address, Long>` — no custom methods; relies entirely on inherited CRUD operations.

`Student` has a lazy `@ManyToOne Dojo` (`dojo_id`, non-updatable), an `active` flag, and a `@OneToOne StudentRank rank`; `Student` extends `Person`, which gives it name/email fields. `Address` is a flat entity with address lines, city, state, and zip. Neither interface declares `@Repository`; Spring Data discovers them. IDs come from `AbstractEntity` (`Long`, `GenerationType.AUTO`).

## Flow
`StudentService.listStudents` calls `findByDojoOwnerId` or `findByDojoId` and filters by `active` in memory; `getStudent`/`updateStudent`/`deactivateStudent` call `findByIdAndDojoOwnerId`; `createStudent`/`updateStudent` call `existsByDojoIdAndEmailIgnoreCase` then `save`; deactivation is a soft delete via `save` with `active = false`. `AddressService.saveAddress` simply calls `save` on `AddressRepository` when persisting a dojo/owner address graph.

## Integration
Consumed by `services.StudentService` (list/get/create/update/deactivate students), `services.AttendanceService` (`findByIdAndDojoOwnerId` to validate the student belongs to the authenticated owner), and `services.AddressService` (address persistence, typically cascaded from dojo/owner setup flows). Exposed through `rest/v1/students/StudentController` (`/api/v1/students`). Manages the `Student` and `Address` entities and their links to `Dojo` and `StudentRank`.
