# src/main/java/com/dojomanager/data/dto/schedule/

## Responsibility

DTO Layer — request/response payload for a weekly schedule session slot, used by the schedule CRUD endpoints.

## Design

- `ScheduleSessionDTO` — plain POJO, all fields annotated with `@JsonProperty`, manual no-arg + all-args constructors and getters/setters.
- Fields are intentionally string-typed to match JSON form input: `dayOfWeek` (`String`, parsed with `DayOfWeek.valueOf`), `startTime` / `endTime` (`String`, parsed with `LocalTime.parse`), `title` (`String`), `instructorId` (`Long`).
- No validation annotations: invalid day/time strings surface as `IllegalArgumentException` from `DayOfWeek.valueOf`/`LocalTime.parse` inside `ScheduleService`, not as 400 validation errors.
- Serves as both inbound body and outbound body (same class), unlike the split form/DTO pattern used for students and attendance.

## Flow

1. `POST /schedules` with `ScheduleSessionDTO` → `ScheduleService.saveSession` converts day/time to `DayOfWeek`/`LocalTime` and copies them plus `title` into a `WeeklySchedule`, then returns the original DTO (note: the entity is built but not persisted, and `instructorId` is not copied in `saveSession`).
2. `PUT /schedules/{id}` → `ScheduleService.updateSession` loads the `WeeklySchedule`, applies the parsed fields, saves, and echoes the DTO; returns `null` when the id is unknown (controller maps to 404).
3. `DELETE /schedules/{id}` → service deletes and returns `null`; controller returns 200.
4. `GET /schedules/week` bypasses this DTO and returns `Map<String, List<WeeklySchedule>>` entities grouped by day name.

## Integration

- Consumed and produced by `com.dojomanager.rest.v1.dojo.ScheduleController` (`/schedules`).
- Converted to/from `com.dojomanager.data.entities.schedule.WeeklySchedule` in `com.dojomanager.services.ScheduleService` (uses `WeeklyScheduleRepository`).
- `instructorId` corresponds to the `WeeklySchedule.instructorId` field but is only carried, not resolved to an instructor entity.
