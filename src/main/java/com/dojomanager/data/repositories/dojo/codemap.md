# src/main/java/com/dojomanager/data/repositories/dojo/

## Responsibility
Data Access Layer — dojo and dojo-owner repositories. Manages persistence and lookup of `Dojo` businesses and the `DojoOwner` accounts that own them, including email-based owner lookup used during authentication and signup.

## Design
Two interfaces, both extending `JpaRepository<T, Long>` with purely derived query methods and no `@Query`:

- `DojoOwnerRepository extends JpaRepository<DojoOwner, Long>` — `Optional<DojoOwner> findByEmail(String email)` and `boolean existsByEmail(String email)`. `DojoOwner` (`dojo_owner` table) extends `Person`, holds `password`, and has a cascading `@OneToMany(mappedBy = "owner")` set of managed dojos.
- `DojoRepository extends JpaRepository<Dojo, Long>` — `Dojo findByName(String name)` (returns `null` when absent, used for uniqueness checks) and `List<Dojo> findByOwner(DojoOwner owner)`. `Dojo` (`dojo` table) has a lazy `@ManyToOne` owner, name/website, `@ManyToOne Address`, `@ManyToOne Student leadInstructor`, and a cascading `@OneToMany` set of students.

Neither interface is annotated `@Repository`; they are discovered as Spring Data interfaces. Entities use the shared `AbstractEntity` `Long` generated ID.

## Flow
`DojoOwnerService` calls `existsByEmail`/`findByEmail` before saving an owner (to reuse an existing ID) and uses `saveAndFlush`, `findById`, and `findAll`. `DojoService` calls `findByName` for name availability, `findByOwner` for owner listings, and `saveAndFlush`/`saveAll` for writes. `StudentService` uses `findByOwner` to resolve which of the caller's dojos a request targets; `AttendanceService` uses `findById` on the owner to authenticate the principal; `ScheduleService` also resolves owners via `DojoOwnerRepository`.

## Integration
Consumed by `services.DojoOwnerService`, `services.DojoService`, `services.StudentService`, `services.AttendanceService`, and `services.ScheduleService`. Indirectly reached from `rest/v1/dojo/DojoController`, `rest/v1/dojo/DojoOwnerController`, `rest/v1/authorization/AuthorizationController`, `rest/v1/students/StudentController`, and `security/services/UserDetailsServiceImpl` (login). Manages the `Dojo` and `DojoOwner` entities and their relationships to `Address`, `Student`, and dojo collections.
