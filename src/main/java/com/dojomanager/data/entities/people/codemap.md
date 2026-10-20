# src/main/java/com/dojomanager/data/entities/people/

## Responsibility

JPA Entity Layer — people and address model. Provides the shared person-name/email superclass, the `Student` entity that belongs to a dojo and carries a rank, and the reusable `Address` entity referenced by dojos.

## Design

- `Person` — `@MappedSuperclass`, `abstract`, extends `AbstractEntity`. Fields: `firstName`, `lastName`, and `email` with `@Column(unique = true)` (backed by a unique index that `StudentApiExceptionHandler` treats as the concurrency backstop). Protected constructor `Person(firstName, lastName, email)`. No table of its own; columns are folded into subclasses.
- `Address` — `@Entity` extends `AbstractEntity` (table `address` by naming convention). Fields `addressLine1`, `addressLine2`, `city`, `state`, `zipCode`; two constructors (5-arg with line 2, 4-arg without). Lombok `@Getter`/`@Setter`/`@NoArgsConstructor`.
- `Student` — `@Entity` extends `Person` (table `student`), Lombok `@NoArgsConstructor`.
  - `active` — `boolean`, defaults to `true`; deactivation is logical, not deletion.
  - `dojo` → `Dojo`, `@ManyToOne(fetch = FetchType.LAZY)`, `@JoinColumn(name = "dojo_id", nullable = false, updatable = false)` — a student cannot move dojos.
  - `rank` → `StudentRank`, `@OneToOne`, `@JoinColumn(name = "rank_id")` — owning side of the student↔rank link (the inverse is `StudentRank.student` with `mappedBy = "rank"`).
  - Constructor `Student(Dojo dojo, StudentRank rank)`.
- Inheritance chain exercised here: `AbstractEntity` → `Person` → `Student`/`DojoOwner`; `Address` is a standalone reference entity shared across aggregates.

## Flow

`StudentRequestForm` (firstName, lastName, email, optional rankId, optional dojoId) → `StudentService.createStudent`/`updateStudent` resolves the target dojo from the authenticated owner, validates email uniqueness per dojo, constructs or mutates a `Student`, optionally sets `rank` via `requireRankInDojo`, and attaches it with `dojo.addStudent(student)` → `StudentRepository.save`. Reads are converted by `StudentService.toDto` into flat `StudentDto` (id, names, email, active, dojoId, rankId/currentRankName/currentRankLevel/hoursInRank) rather than serializing the lazy `Dojo`/`StudentRank` graph. `Address` is written through `AddressService.saveAddress` and assigned to dojos in `DojoController`, not to students.

## Integration

- Repositories: `com.dojomanager.data.repositories.people.StudentRepository` (`findByDojoId`, `findByDojoOwnerId`, `findByIdAndDojoOwnerId`, `existsByDojoIdAndEmailIgnoreCase`) and `AddressRepository` (`JpaRepository<Address, Long>`).
- Services: `StudentService` (CRUD + ownership enforcement, rank validation), `AddressService`, `DojoService`/`DojoOwnerService` (address and student cascade management).
- Referenced by: `Dojo.students` / `Dojo.leadInstructor`, `AttendanceRecord.student`, `StudentRank.student` (inverse), and the `DojoOwner` sibling subclass of `Person`.
- DTO/form: `data.dto.student.StudentDto`, `data.forms.student.StudentRequestForm`.
- REST: `rest.v1.students.StudentController`, `StudentApiExceptionHandler`; `rest.v1.dojo.DojoController` constructs `Address` for dojo updates.
