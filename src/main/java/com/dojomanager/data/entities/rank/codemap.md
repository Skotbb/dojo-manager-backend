# src/main/java/com/dojomanager/data/entities/rank/

## Responsibility

JPA Entity Layer — rank and belt model. Defines the per-dojo rank catalog (`RankName` → `RankLevel` → `BeltColor`, assembled into `RankSetting`) and the per-student rank instance (`StudentRank`) that tracks time in grade.

## Design

- `RankName` — `@Entity` extends `AbstractEntity`. Fields `name` and `dojo` (`@ManyToOne(optional = false)`), i.e. rank names are dojo-scoped. Constructor `(String name, Dojo dojo)`.
- `RankLevel` — `@Entity` extends `AbstractEntity`. Fields `sortOrder`, `number`, `hoursRequired` (all `int`), and `rankName` (`@ManyToOne(optional = false)`). Constructor `(sortOrder, number, hoursRequired, rankName)`.
- `BeltColor` — `@Entity` extends `AbstractEntity`. Fields `color`, `stripeColor` (String), `stripeCount` (`int`, read-only Lombok getter; custom `setStripeCount(Integer)` normalises null to `0`), and `rankLevel` (`@ManyToOne(optional = false)`). Constructor takes `Integer stripeCount` and routes through the null-safe setter.
- `RankSetting` — `@Entity` extends `AbstractEntity`. The join/configuration row combining `dojo`, `rankName`, `rankLevel` and `beltColor` (all `@ManyToOne`, nullable). Constructor `(Dojo, RankName, RankLevel, BeltColor)`. This is what a student is actually assigned.
- `StudentRank` — `@Entity` extends `AbstractEntity`. Fields `hoursInRank` (`double`), `rankSetting` (`@ManyToOne`), and `student` as the inverse `@OneToOne(mappedBy = "rank")`. Constructor `(hoursInRank, rankSetting)`; the student link is set from `Student.rank`.
- No enum types live in this package; belt/rank metadata is fully data-driven per dojo. All five entities share the `AbstractEntity` id/equality contract and Lombok accessors.

## Flow

`RankService` persists catalog pieces (`saveRankName`, `saveRankLevel`, `saveBeltColor`, `saveRankSetting`) and reads a dojo's configuration via `RankSettingRepository.findByDojo`. A `StudentRank` is created/assigned through `saveRank` and then linked from `Student.rank`. On the read path, `StudentService.toDto` walks `Student.rank → StudentRank.rankSetting` to emit `currentRankName` (from `RankSetting.rankName.name`), `currentRankLevel` (from `RankSetting.rankLevel.number`) and `hoursInRank`. On the write path, `StudentRequestForm.rankId` is resolved by `StudentService.requireRankInDojo`, which loads the `StudentRank` and rejects it unless its `rankSetting.dojo` matches the student's dojo.

## Integration

- Repositories: `com.dojomanager.data.repositories.rank.RankNameRepository`, `RankLevelRepository`, `BeltColorRepository`, `RankSettingRepository` (`findByDojo`), and `RankRepository` (`JpaRepository<StudentRank, Long>`).
- Services: `RankService` (catalog CRUD/read), `StudentService` (rank assignment and ownership validation).
- Referenced by: `Student.rank` (owning `@OneToOne`, join column `rank_id`), `DojoController`/`DojoOwnerController` (import `BeltColor`, `RankLevel`, `RankName` for dojo setup endpoints).
- DTO/form: `data.dto.student.StudentDto` (`rankId`, `currentRankName`, `currentRankLevel`, `hoursInRank`) and `data.forms.student.StudentRequestForm` (`rankId`).
