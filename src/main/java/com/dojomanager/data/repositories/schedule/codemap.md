# src/main/java/com/dojomanager/data/repositories/schedule/

## Responsibility
Data Access Layer — schedule repositories. Persists recurring weekly class templates (`WeeklySchedule`) and concrete per-day sessions (`DailySchedule`) used by the scheduling and attendance features.

## Design
Two interfaces extending `JpaRepository<T, Long>`, both with no custom query methods and no `@Query`:

- `DailyScheduleRepository extends JpaRepository<DailySchedule, Long>` — inherited CRUD only. `DailySchedule` stores `hoursPerClass`, `startTime`, and a `@ManyToOne WeeklySchedule`.
- `WeeklyScheduleRepository extends JpaRepository<WeeklySchedule, Long>` — inherited CRUD only. `WeeklySchedule` stores `dayOfWeek`, `classesPerDay`, `@ManyToOne Dojo`, `startTime`, `endTime`, `title`, and `instructorId`.

Neither is annotated `@Repository`; Spring Data discovers them. Entities use the shared `AbstractEntity` `Long` generated ID. Querying is done with inherited `findById`, `findAll`, `save`, and `deleteById`, with grouping/filtering performed in the service layer.

## Flow
`ScheduleService.saveWeeklySchedule`/`saveDailySchedule` call `save`. `updateSession(id, ...)` loads via `weeklyRepo.findById`, mutates the managed entity, then saves it; `deleteSession(id)` calls `weeklyRepo.deleteById`; `getWeekView()` calls `weeklyRepo.findAll()` and groups the results by `DayOfWeek` in Java. `AttendanceService` calls `dailyRepo.findById(sessionId)` to validate that a check-in's session exists and belongs to the student's dojo/owner.

## Integration
Consumed by `services.ScheduleService` (create/update/delete weekly sessions and week view) and `services.AttendanceService` (`DailyScheduleRepository` for session validation during check-in). Exposed through `rest/v1/dojo/ScheduleController` (`/schedules`) and indirectly via `rest/v1/attendance/AttendanceController`. Manages the `WeeklySchedule` and `DailySchedule` entities and their links to `Dojo`.
