# src/main/java/com/dojomanager/data/repositories/rank/

## Responsibility
Data Access Layer — rank and belt repositories. Persists the rank catalog (`RankName`, `RankLevel`, `BeltColor`), per-dojo rank configurations (`RankSetting`), and individual student rank assignments/history (`StudentRank`).

## Design
Five interfaces extending `JpaRepository<T, Long>`, almost all relying on inherited CRUD methods with no custom queries:

- `BeltColorRepository extends JpaRepository<BeltColor, Long>` — no custom methods. `BeltColor` holds color/stripe strings, `stripeCount`, and a `@ManyToOne RankLevel`.
- `RankLevelRepository extends JpaRepository<RankLevel, Long>` — no custom methods. `RankLevel` holds `sortOrder`, `number`, `hoursRequired`, and a `@ManyToOne RankName`.
- `RankNameRepository extends JpaRepository<RankName, Long>` — no custom methods. `RankName` holds `name` and a `@ManyToOne Dojo`, scoping rank names to a dojo.
- `RankRepository extends JpaRepository<StudentRank, Long>` — no custom methods; despite the generic name it manages `StudentRank` (hours in rank, `@ManyToOne RankSetting`, `@OneToOne(mappedBy = "rank") Student`).
- `RankSettingRepository extends JpaRepository<RankSetting, Long>` — `Optional<List<RankSetting>> findByDojo(Dojo dojo)`, the package's only derived finder; `RankSetting` joins `Dojo`, `RankName`, `RankLevel`, and `BeltColor`.

No interface is annotated `@Repository`; Spring Data discovers them. IDs are the inherited `AbstractEntity` `Long` generated values.

## Flow
`RankService` writes each catalog entity via `save` (`saveRankName`, `saveRankLevel`, `saveBeltColor`, `saveRankSetting`, `saveRank`) and reads a dojo's configuration via `settingRepo.findByDojo(dojo)`, unwrapping the `Optional` to an empty list when absent. `StudentService` calls `rankRepo.findById(rankId)` when assigning a rank and verifies the rank's `RankSetting.dojo` matches the student's dojo; it also traverses `StudentRank.rankSetting` to expose current rank name/level in `StudentDto`.

## Integration
Consumed by `services.RankService` (catalog and setting persistence, dojo rank settings) and `services.StudentService` (`RankRepository` for rank assignment/validation and DTO enrichment). `RankService` is used by `rest/v1/dojo/DojoController` for dojo/rank setup. Manages `BeltColor`, `RankLevel`, `RankName`, `RankSetting`, and `StudentRank`, linking them to `Dojo` and `Student`.
