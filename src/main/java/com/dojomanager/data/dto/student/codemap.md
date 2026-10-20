# src/main/java/com/dojomanager/data/dto/student/

## Responsibility

DTO Layer — owner-scoped, flattened student read model returned by the student management API. Explicitly avoids serializing the JPA entity graph.

## Design

- `StudentDto` — Lombok `@Data`, `@Builder`, `@NoArgsConstructor`, `@AllArgsConstructor`; response-only, no validation.
- Identity fields: `id`, `firstName`, `lastName`, `email`, `active`.
- Ownership/rank fields: `dojoId` (owning dojo id), `rankId` (current `StudentRank` id, `null` when unranked), `currentRankName` (`RankSetting.rankName.name`), `currentRankLevel` (`RankSetting.rankLevel.number`), `hoursInRank` (`Double` from `StudentRank.getHoursInRank()`).
- The class Javadoc documents the design decision: the `Student` entity holds lazy `Dojo` (→ `DojoOwner`) and lazy `StudentRank` relations, so returning the entity would leak another aggregate's object graph; the DTO projects only ids and denormalized display values.
- Building is done with the generated `StudentDtoBuilder`; rank-related fields are populated only when `student.getRank()` is non-null, and name/level only when the nested `RankSetting` and its `RankName`/`RankLevel` are non-null.

## Flow

1. `StudentController` (`POST /api/v1/students`, `GET /{id}`, `PUT /{id}`, `DELETE /{id}`, `GET` list) calls `StudentService`.
2. `StudentService` loads and owner-checks the `Student`, mutates fields for create/update/deactivate, and calls `toDto(student)`.
3. `toDto` projects the entity/rank into the builder; controllers return the DTO directly (or wrapped in `ResponseEntity` with `201 Created` on create).
4. `GET /api/v1/students/{id}/attendance` returns `AttendanceRecordDto` instead, from `AttendanceService`.

## Integration

- Produced by `com.dojomanager.services.StudentService` (`listStudents`, `getStudent`, `createStudent`, `updateStudent`, `deactivateStudent`, private `toDto`).
- Consumed by `com.dojomanager.rest.v1.students.StudentController`.
- Maps from `com.dojomanager.data.entities.people.Student`, `com.dojomanager.data.entities.dojo.Dojo`, and the rank entities `StudentRank`/`RankSetting`/`RankName`/`RankLevel`.
