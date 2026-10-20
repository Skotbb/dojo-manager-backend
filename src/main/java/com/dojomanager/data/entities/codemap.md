# src/main/java/com/dojomanager/data/entities/

## Responsibility

JPA Entity Layer — the persistence model for the DojoManager domain. Holds the shared entity base class and the five domain sub-packages (attendance, dojo, people, rank, schedule) that map business objects to relational tables. No business logic or HTTP concerns live here; entities are the data contract consumed by repositories, services and (indirectly) DTO mapping.

## Design

- `AbstractEntity` (`@MappedSuperclass`) is the root of every persistent class. It declares `Long id` with `@Id` + `@GeneratedValue(strategy = GenerationType.AUTO)`, plus identity-based `equals` (same runtime class + id) and `hashCode` (`0` when id is null). Concrete entities extend it to inherit the primary key and identity semantics.
- Entities use Jakarta Persistence (`jakarta.persistence`) annotations: `@Entity`, `@Table`, `@MappedSuperclass`, `@ManyToOne`, `@OneToOne`, `@OneToMany`, `@JoinColumn`, `@Enumerated`, `@Column`.
- Inheritance is layered: `AbstractEntity` → `Person` (`@MappedSuperclass`, holds `firstName`, `lastName`, unique `email`) → `DojoOwner` and `Student` (`@Entity` subclasses). This gives owner and student a shared person-name/email shape without a joined table.
- Lombok (`@Getter`, `@Setter`, `@NoArgsConstructor`) supplies accessors; a few entities hand-write null-safe setters (`BeltColor.setStripeCount`) or guarded collection accessors (`DojoOwner.getManagedDojos` returns an unmodifiable set).
- Bidirectional associations are maintained by helper methods (`Dojo.addStudent`, `DojoOwner.addDojo`/`removeDojo`) so both sides stay in sync.
- Hibernate's default `CamelCaseToUnderscoresNamingStrategy` maps the unannotated classes to snake_case tables (`student`, `address`, `rank_name`, `rank_level`, `belt_color`, `rank_setting`, `student_rank`, `daily_schedule`, `weekly_schedule`); `Dojo` and `DojoOwner` pin names via `@Table`.

Sub-packages:

- `attendance/` — `AttendanceRecord` entity plus the `AttendanceStatus` enum (`PRESENT`, `ABSENT`, `LATE`) modelling one check-in per student/session/date.
- `dojo/` — `Dojo` and `DojoOwner` entities: the tenancy root (owner → dojos → students) and dojo address/lead-instructor links.
- `people/` — `Person` mapped superclass, `Student` entity and `Address` value entity used by both students and dojos.
- `rank/` — the belt/rank catalog: `RankName`, `RankLevel`, `BeltColor`, `RankSetting` configuration plus per-student `StudentRank` progress.
- `schedule/` — `WeeklySchedule` (dojo weekly class template) and `DailySchedule` (a concrete session within a week).

## Flow

Database tables → Hibernate/JPA materialises entity instances → Spring Data `JpaRepository` interfaces in `com.dojomanager.data.repositories` query and persist them → services (`AttendanceService`, `StudentService`, `DojoService`, `DojoOwnerService`, `RankService`, `ScheduleService`, `AddressService`) mutate entities inside transactions → DTOs/forms (`AttendanceRecordDto`, `StudentDto`, `DojoResponse`, `ScheduleSessionDTO`) flatten the graph for JSON responses. Entities are never intended to be serialized directly, except where explicitly annotated with `@JsonIgnore` on back-references.

## Integration

Every repository in `data/repositories` is parameterised by an entity from this tree (`JpaRepository<AttendanceRecord, Long>`, `JpaRepository<Dojo, Long>`, `JpaRepository<Student, Long>`, `JpaRepository<StudentRank, Long>`, etc.). Services in `com.dojomanager.services` own the transactional use of these entities; `com.dojomanager.security.services.UserDetailsServiceImpl` and `OwnerPrincipal` depend on `DojoOwner` for authentication; REST controllers under `com.dojomanager.rest.v1` receive/return DTOs and forms, not raw entities (with `DojoController`/`DojoOwnerController` being the legacy exception that still expose `Dojo`/`DojoOwner`).
