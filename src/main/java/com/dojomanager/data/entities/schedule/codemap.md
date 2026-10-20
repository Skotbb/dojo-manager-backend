# src/main/java/com/dojomanager/data/entities/schedule/

## Responsibility

JPA Entity Layer — class schedule model. Represents a dojo's weekly class template (`WeeklySchedule`) and a concrete session instance inside that week (`DailySchedule`), which attendance check-ins can reference.

## Design

- `WeeklySchedule` — `@Entity` extends `AbstractEntity`, `@NoArgsConstructor`. Fields:
  - `dayOfWeek` (`java.time.DayOfWeek`; no `@Enumerated`, so Hibernate stores the default ordinal),
  - `classesPerDay` (`int`),
  - `dojo` (`@ManyToOne`, links the template to its `Dojo`),
  - `startTime` / `endTime` (`LocalTime`),
  - `title` (`String`),
  - `instructorId` (`Long` — plain FK-style id to a student/user, not a JPA association).
  - Constructor `WeeklySchedule(DayOfWeek, int classesPerDay, Dojo)`.
- `DailySchedule` — `@Entity` extends `AbstractEntity`, `@NoArgsConstructor`. Fields `hoursPerClass` (`double`), `startTime` (`LocalTime`), and `weeklySchedule` (`@ManyToOne`) pointing back to the owning weekly template. Constructor `(double hoursPerClass, LocalTime startTime, WeeklySchedule weeklySchedule)`.
- `DayOfWeek` is Java's `java.time` enum, not a project enum. There are no cascade or collection mappings here; the relation is a simple many-to-one aggregate reference, and `AttendanceRecord.session` is the downstream link to a concrete daily session.

## Flow

`ScheduleSessionDTO` (dayOfWeek as String, startTime/endTime as String, title) → `ScheduleService.saveSession` parses into `DayOfWeek`/`LocalTime` and populates a `WeeklySchedule`; `updateSession` loads by id, mutates the same fields and saves; `deleteSession` removes by id; `getWeekView` reads `weeklyRepo.findAll()` and groups sessions into a `Map<String, List<WeeklySchedule>>` keyed by `dayOfWeek.name()`. `saveWeeklySchedule`/`saveDailySchedule` persist entities directly. Attendance flows separately: `AttendanceService` loads a `DailySchedule` by id, walks `dailySchedule.weeklySchedule.dojo` to verify ownership, then stores the session on `AttendanceRecord.session`.

## Integration

- Repositories: `com.dojomanager.data.repositories.schedule.WeeklyScheduleRepository` and `DailyScheduleRepository` (both plain `JpaRepository`).
- Services: `ScheduleService` (create/update/delete/list week view, DTO translation).
- Consumers of the entities: `AttendanceService` (`DailySchedule` lookup and dojo ownership checks), `AttendanceRecord.session` (`@ManyToOne` join column `session_id`), and `WeeklySchedule.dojo` linking schedules to the dojo tenancy root.
- REST/DTO: `rest.v1.dojo.ScheduleController` (`/schedules`, `GET /schedules/week`), `data.dto.schedule.ScheduleSessionDTO`.
