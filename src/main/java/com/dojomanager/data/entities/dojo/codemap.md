# src/main/java/com/dojomanager/data/entities/dojo/

## Responsibility

JPA Entity Layer — dojo tenancy model. Defines a martial-arts school (`Dojo`) and the account that owns it (`DojoOwner`), forming the ownership root for students, attendance, ranks and schedules.

## Design

- `Dojo` extends `AbstractEntity`; `@Entity`, `@Table(name = "dojo")`, `@NoArgsConstructor`.
  - `owner` → `DojoOwner`, `@ManyToOne(fetch = FetchType.LAZY)`, `@JoinColumn(name = "owner_id", nullable = false, updatable = false)` (ownership is immutable once set).
  - `name`, `website` — plain `String` columns.
  - `address` → `Address`, `@ManyToOne` (default EAGER) — shared address entity from `people`.
  - `leadInstructor` → `Student`, `@ManyToOne` — the student acting as head instructor.
  - `students` → `Set<Student>`, `@OneToMany(mappedBy = "dojo", orphanRemoval = true, cascade = CascadeType.ALL)`, initialised to `HashSet`, annotated `@JsonIgnore` to prevent serialization loops and only exposed via Lombok `@Getter` (no public setter).
  - Constructors `Dojo(name, website)` and `Dojo(name, website, address)`; `addStudent(Student)` adds to the set and back-references `student.setDojo(this)`.
- `DojoOwner` extends `Person` (so it inherits `id`, `firstName`, `lastName`, unique `email` from `AbstractEntity`/`Person`); `@Entity`, `@Table(name = "dojo_owner")`.
  - `managedDojos` → `Set<Dojo>`, `@OneToMany(cascade = CascadeType.ALL, orphanRemoval = true, mappedBy = "owner")`, `@JsonIgnore`; exposed through `getManagedDojos()` returning `Collections.unmodifiableSet`.
  - `password` — `String`, stored as-is (no hashing in the entity).
  - Constructor `DojoOwner(firstName, lastName, email, password)`; `addDojo(Dojo)`/`removeDojo(Dojo)` keep both sides of the relation consistent (`dojo.setOwner(...)`).
- Aggregate boundary: `DojoOwner` is the parent side that cascades persistence to its `Dojo` set, and `Dojo` cascades to its `Student` set.

## Flow

`AuthorizationController` signup builds a `DojoOwner` and hands it to `DojoOwnerService.saveDojoOwner` (which de-duplicates by email) → owners are loaded by email for JWT authentication. `DojoController`/`DojoOwnerController` read the authenticated owner, resolve `owner.getManagedDojos()`, and create/update `Dojo` instances (name, website, `Address`) through `DojoService.saveDojo`/`addDojoToOwner`. Incoming `DojoResponse`/`DojoUpdateRequest` DTOs wrap or drive these entities. Lazy `owner`/`dojo` links are traversed by services for authorization (`owner.getId()` comparisons) rather than serialized.

## Integration

- Repositories: `com.dojomanager.data.repositories.dojo.DojoRepository` (`findByName`, `findByOwner`) and `DojoOwnerRepository` (`findByEmail`, `existsByEmail`).
- Services: `DojoService` (save, `getDojoById`, `getDojosForOwner`, `addDojoToOwner`), `DojoOwnerService` (`saveDojoOwner`, `getOwnerByEmail`, `getOwnerById`, `getAllOwners`, `getDojos`).
- Security: `security.services.UserDetailsServiceImpl` resolves a `DojoOwner` by email; `OwnerPrincipal.build(DojoOwner)` carries its id into every controller/service call.
- Other entities referencing these: `Student.dojo`, `AttendanceRecord.dojo`, `WeeklySchedule.dojo`, `RankName.dojo`, `RankSetting.dojo`.
- REST/DTO: `rest.v1.dojo.DojoController`, `DojoOwnerController`, `rest.v1.authorization.AuthorizationController`; `data.dto.dojo.DojoResponse`, `DojoUpdateRequest`.
