# src/main/java/com/dojomanager/data/

## Responsibility
Data layer umbrella package. It groups the persistence model and the API data contracts; it contains no classes of its own.

## Design
Organised by responsibility, each subpackage further split by domain area:
- `entities/` — JPA entities: `attendance` (`AttendanceRecord`), `dojo` (`Dojo`, `DojoOwner`), `people` (`Person`, `Student`, `Address`), `rank` (`StudentRank`, `RankSetting`, `RankName`, `RankLevel`, `BeltColor`), `schedule` (`WeeklySchedule`, `DailySchedule`).
- `repositories/` — Spring Data JPA interfaces mirroring the entity areas: `attendance`, `dojo`, `people`, `rank`, `schedule`.
- `dto/` — outbound API payloads: `attendance` (`AttendanceRecordDto`), `authorization` (`JwtResponse`), `dojo` (`DojoResponse`, `DojoUpdateRequest`, `OwnerDTO`), `schedule` (`ScheduleSessionDTO`), `student` (`StudentDto`).
- `forms/` — inbound request bodies: `attendance` (`AttendanceCheckInForm`), `student` (`StudentRequestForm`).

## Flow
Controllers bind `forms` → services load/save `entities` through `repositories` → services map entities to `dto` objects returned as JSON. Entities never leave the service layer directly on the newer endpoints.

## Integration
Consumed exclusively by `com.dojomanager.services` (and the `security` layer for owner/user lookups) and by `com.dojomanager.rest` for `forms`/`dto` types. `dto` and `forms` are additionally referenced in controller method signatures.
